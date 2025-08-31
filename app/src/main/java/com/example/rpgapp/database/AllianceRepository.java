package com.example.rpgapp.database;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.Notification;
import com.example.rpgapp.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class AllianceRepository {

    private static final String TAG = "AllianceRepository";
    private static volatile AllianceRepository INSTANCE;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserRepository userRepository;

    private AllianceRepository(Context context) {
        this.userRepository = UserRepository.getInstance(context);
    }

    public static AllianceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AllianceRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AllianceRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void createAlliance(String name, List<String> invitedFriendIds, UserRepository.RequestCallback callback) {
        User currentUser = userRepository.getLoggedInUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not logged in."));
            return;
        }

        if (currentUser.getAllianceId() != null) {
            callback.onFailure(new Exception("You are already in an alliance."));
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference newAllianceRef = db.collection("alliances").document();
        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUser.getUserId());

        Alliance newAlliance = new Alliance(name, currentUser.getUserId(), currentUser.getUsername(), memberIds, invitedFriendIds);
        newAlliance.setAllianceId(newAllianceRef.getId());
        batch.set(newAllianceRef, newAlliance);

        DocumentReference myDocRef = db.collection("users").document(currentUser.getUserId());
        batch.update(myDocRef, "allianceId", newAlliance.getAllianceId());

        for (String friendId : invitedFriendIds) {
            DocumentReference friendDocRef = db.collection("users").document(friendId);
            batch.update(friendDocRef, "allianceInvites", FieldValue.arrayUnion(newAlliance.getAllianceId()));
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Alliance created successfully!");
            userRepository.refreshLoggedInUser(); // Pozivamo refresh da se podaci o korisniku osveže
            callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to create alliance.", e);
            callback.onFailure(e);
        });
    }

    // TODO: Ovde ćemo kasnije dodati ostale metode (get allianceById, acceptInvite, sendMessage, itd.)
    public interface AllianceCallback {
        void onAllianceLoaded(Alliance alliance);
    }
    public void listenToAlliance(String allianceId, AllianceCallback callback) {
        db.collection("alliances").document(allianceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Alliance alliance = snapshot.toObject(Alliance.class);
                        callback.onAllianceLoaded(alliance);
                    } else {
                        Log.d(TAG, "Current data: null");
                        callback.onAllianceLoaded(null);
                    }
                });
    }
    public void getMemberProfiles(List<String> memberIds, UserRepository.FriendsCallback callback) {
        if (memberIds == null || memberIds.isEmpty()) {
            callback.onFriendsLoaded(new ArrayList<>());
            return;
        }

        List<User> members = new ArrayList<>();
        final int[] tasksCompleted = {0};
        int totalTasks = memberIds.size();

        for (String userId : memberIds) {
            userRepository.getUserById(userId, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    if (user != null) {
                        members.add(user);
                    }
                    tasksCompleted[0]++;
                    if (tasksCompleted[0] == totalTasks) {
                        callback.onFriendsLoaded(members);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Greška pri učitavanju profila člana " + userId, e);
                    tasksCompleted[0]++;
                    if (tasksCompleted[0] == totalTasks) {
                        callback.onFriendsLoaded(members);
                    }
                }
            });
        }
    }
    public void getAllianceById(String allianceId, AllianceCallback callback) {
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onAllianceLoaded(documentSnapshot.toObject(Alliance.class));
                    } else {
                        callback.onAllianceLoaded(null);
                    }
                });
    }

    public void acceptAllianceInvite(String newAllianceId, UserRepository.RequestCallback callback) {
        User currentUser = userRepository.getLoggedInUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not logged in."));
            return;
        }

        // TODO: Provera da li je misija u starom savezu aktivna pre nego što se ovo pozove!

        WriteBatch batch = db.batch();
        String currentUserId = currentUser.getUserId();

        DocumentReference userDoc = db.collection("users").document(currentUserId);
        batch.update(userDoc, "allianceId", newAllianceId);
        batch.update(userDoc, "allianceInvites", FieldValue.arrayRemove(newAllianceId));

        DocumentReference newAllianceDoc = db.collection("alliances").document(newAllianceId);
        batch.update(newAllianceDoc, "pendingInviteIds", FieldValue.arrayRemove(currentUserId));
        batch.update(newAllianceDoc, "memberIds", FieldValue.arrayUnion(currentUserId));

        String oldAllianceId = currentUser.getAllianceId();
        if (oldAllianceId != null && !oldAllianceId.isEmpty()) {
            DocumentReference oldAllianceDoc = db.collection("alliances").document(oldAllianceId);
            batch.update(oldAllianceDoc, "memberIds", FieldValue.arrayRemove(currentUserId));
        }

        db.collection("alliances").document(newAllianceId).get()
                .addOnSuccessListener(allianceDoc -> {
                    if (allianceDoc.exists()) {
                        String leaderId = allianceDoc.getString("leaderId");
                        String allianceName = allianceDoc.getString("name");

                        String title = "New Member!";
                        String message = currentUser.getUsername() + " has joined your alliance '" + allianceName + "'.";
                        Notification notification = new Notification(leaderId, title, message);

                        DocumentReference notificationRef = db.collection("notifications").document(); // Novi ID
                        batch.set(notificationRef, notification);

                        batch.commit().addOnSuccessListener(aVoid -> {
                            userRepository.refreshLoggedInUser();
                            callback.onSuccess();
                        }).addOnFailureListener(e -> callback.onFailure(e));
                    } else {
                        callback.onFailure(new Exception("Alliance not found."));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void declineAllianceInvite(String allianceId, UserRepository.RequestCallback callback) {
        User currentUser = userRepository.getLoggedInUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not logged in."));
            return;
        }

        DocumentReference userDoc = db.collection("users").document(currentUser.getUserId());

        userDoc.update("allianceInvites", FieldValue.arrayRemove(allianceId))
                .addOnSuccessListener(aVoid -> {
                    userRepository.refreshLoggedInUser();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

}
