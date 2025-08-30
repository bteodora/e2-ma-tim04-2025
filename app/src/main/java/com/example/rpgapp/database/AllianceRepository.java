package com.example.rpgapp.database;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.rpgapp.model.Alliance;
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
}
