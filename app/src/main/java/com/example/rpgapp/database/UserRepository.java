package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.model.SendRequestStatus;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.model.UserWeapon;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private static final String TAG = "UserRepository";
    private static volatile UserRepository INSTANCE;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SQLiteHelper dbHelper;

    private User loggedInUser = null;
    private MutableLiveData<User> loggedInUserLiveData = new MutableLiveData<>();
    private Context context;

    private UserRepository(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = new SQLiteHelper(this.context);
    }

    public static UserRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void setLoggedInUser(String userId) {
        db.collection("users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    // LOG 1: DA LI SE LISTENER UOPŠTE AKTIVIRA?
                    Log.d("RPG_REACTIVE_DEBUG", "!!! Snapshot Listener je AKTIVIRAN !!!");

                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        User freshUser = snapshot.toObject(User.class);
                        if (freshUser != null) {
                            freshUser.setUserId(userId);
                            this.loggedInUser = freshUser;

                            // --- ISPRAVLJEN DEO ---
                            // Prvo proverimo da li je lista null, pre nego što je koristimo.
                            List<String> requests = freshUser.getFriendRequests();
                            int requestCount = (requests != null) ? requests.size() : 0; // Ako je null, broj je 0

                            // LOG 2: DA LI SE LIVE DATA AŽURIRA? (Sada je bezbedan)
                            Log.d("RPG_REACTIVE_DEBUG", "Listener: Ažuriram LiveData sa novim podacima. Broj zahteva: " + requestCount);
                            // ---------------------

                            this.loggedInUserLiveData.postValue(this.loggedInUser);
                            cacheUserToSQLite(this.loggedInUser);
                        }
                    }
                });
    }

    public void refreshLoggedInUser() {
        if (loggedInUser == null) return;

        String userId = loggedInUser.getUserId();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User freshUser = documentSnapshot.toObject(User.class);
                        if (freshUser != null) {
                            freshUser.setUserId(userId);
                            this.loggedInUser = freshUser;
                            this.loggedInUserLiveData.postValue(this.loggedInUser);
                            Log.d("RPG_REACTIVE_DEBUG", "RUČNO OSVEŽAVANJE USPELO!");
                        }
                    }
                });
    }

    public LiveData<User> getLoggedInUserLiveData() {
        return loggedInUserLiveData;
    }

    public void logoutUser() {
        String userId = (this.loggedInUser != null) ? this.loggedInUser.getUserId() : null;
        this.loggedInUser = null;
        this.loggedInUserLiveData.postValue(null);
        if (userId != null) {
            deleteUserFromSQLite(userId);
        }
    }

    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(Exception e);
    }
    public interface UserSearchCallback {
        void onUsersFound(List<User> users);
        void onError(Exception e);
    }
    public interface RequestCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    public interface FriendsCallback {
        void onFriendsLoaded(List<User> friends);
        void onError(Exception e);
    }
    public interface SendRequestCallback {
        void onComplete(SendRequestStatus status, @Nullable Exception e);
    }

    public User getLoggedInUser(){
        return loggedInUser;
    }


    public void sendFriendRequest(String targetUserId, SendRequestCallback callback) {
        if (loggedInUser == null) {
            callback.onComplete(SendRequestStatus.FAILURE, new Exception("User not logged in."));
            return;
        }

        String currentUserId = loggedInUser.getUserId();

        if (currentUserId.equals(targetUserId)) {
            callback.onComplete(SendRequestStatus.CANNOT_ADD_SELF, null);
            return;
        }

        if (loggedInUser.getFriendIds() != null && loggedInUser.getFriendIds().contains(targetUserId)) {
            callback.onComplete(SendRequestStatus.ALREADY_FRIENDS, null);
            return;
        }

        DocumentReference targetUserDocRef = db.collection("users").document(targetUserId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(targetUserDocRef);
            List<String> friendRequests = (List<String>) snapshot.get("friendRequests");

            if (friendRequests == null) {
                friendRequests = new ArrayList<>();
            }

            if (friendRequests.contains(currentUserId)) {
                return null;
            }

            friendRequests.add(currentUserId);
            transaction.update(targetUserDocRef, "friendRequests", friendRequests);
            return null;

        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transakcija za slanje zahteva uspešna.");
            callback.onComplete(SendRequestStatus.SUCCESS, null);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Greška pri slanju zahteva za prijateljstvo.", e);
            callback.onComplete(SendRequestStatus.FAILURE, e);
        });
    }

    public void getRequestingUsers(List<String> requestIds, FriendsCallback callback) {
        if (requestIds == null || requestIds.isEmpty()) {
            callback.onFriendsLoaded(new ArrayList<>());
            return;
        }

        List<User> requestingUsers = new ArrayList<>();
        final int[] tasksCompleted = {0};
        int totalTasks = requestIds.size();

        for (String userId : requestIds) {
            getUserById(userId, new UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    if (user != null) {
                        requestingUsers.add(user);
                    }
                    tasksCompleted[0]++;
                    if (tasksCompleted[0] == totalTasks) {
                        callback.onFriendsLoaded(requestingUsers);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Greška pri učitavanju korisnika " + userId, e);
                    tasksCompleted[0]++;
                    if (tasksCompleted[0] == totalTasks) {
                        callback.onFriendsLoaded(requestingUsers); // Vrati one koje si uspeo da učitaš
                    }
                }
            });
        }
    }


    public void acceptFriendRequest(String newFriendId, RequestCallback callback) {
        String myId = loggedInUser.getUserId();

        DocumentReference myDoc = db.collection("users").document(myId);
        DocumentReference friendDoc = db.collection("users").document(newFriendId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    transaction.update(myDoc, "friendRequests", FieldValue.arrayRemove(newFriendId));
                    transaction.update(myDoc, "friendIds", FieldValue.arrayUnion(newFriendId));
                    transaction.update(friendDoc, "friendIds", FieldValue.arrayUnion(myId));
                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void declineFriendRequest(String requesterId, RequestCallback callback) {
        String myId = loggedInUser.getUserId();
        DocumentReference myDoc = db.collection("users").document(myId);

        myDoc.update("friendRequests", FieldValue.arrayRemove(requesterId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }
    public void getFriendsProfiles(User currentUser, FriendsCallback callback) {
        if (currentUser.getFriendIds() == null || currentUser.getFriendIds().isEmpty()) {
            callback.onFriendsLoaded(new ArrayList<>());
            return;
        }

        List<String> friendIds = currentUser.getFriendIds();
        List<User> friends = new ArrayList<>();

        final int[] completedTasks = {0};
        int totalTasks = friendIds.size();

        for (String userId : friendIds) {
            getUserById(userId, new UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    if (user != null) {
                        friends.add(user);
                    }
                    completedTasks[0]++;
                    if (completedTasks[0] == totalTasks) {
                        callback.onFriendsLoaded(friends);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Greška pri učitavanju profila prijatelja " + userId, e);
                    completedTasks[0]++;
                    if (completedTasks[0] == totalTasks) {
                        callback.onFriendsLoaded(friends);
                    }
                }
            });
        }
    }
    public void getUserById(String userId, UserCallback callback) {
        User cachedUser = getUserFromSQLite(userId);
        if (cachedUser != null) {
            callback.onUserLoaded(cachedUser);
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(userId);
                            callback.onUserLoaded(user);
                        } else {
                            callback.onError(new Exception("Failed to parse user data."));
                        }
                    } else {
                        callback.onUserLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user from Firestore", e);
                    callback.onError(e);
                });
    }

    public void getUserByUsername(String username, UserSearchCallback callback) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", username)
                .whereLessThanOrEqualTo("username", username + "\uf8ff")
                .limit(10) // Ograničavamo na 10 rezultata da ne preopteretimo mrežu
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> foundUsers = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());
                        foundUsers.add(user);
                    }
                    callback.onUsersFound(foundUsers);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching for users", e);
                    callback.onError(e);
                });
    }
    public void searchUsersByUsername(String usernameQuery, UserSearchCallback callback) {
        if (usernameQuery == null || usernameQuery.trim().isEmpty()) {
            callback.onUsersFound(new ArrayList<>());
            return;
        }

        db.collection("users")
                .whereGreaterThanOrEqualTo("username", usernameQuery)
                .whereLessThanOrEqualTo("username", usernameQuery + "\uf8ff")
                .limit(15)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> foundUsers = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());
                        foundUsers.add(user);
                    }
                    callback.onUsersFound(foundUsers);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri pretrazi korisnika", e);
                    callback.onError(e);
                });
    }
    public void updateUser(User userToUpdate) {
        if (userToUpdate == null || userToUpdate.getUserId() == null) return;
        String userId = userToUpdate.getUserId();

        db.collection("users").document(userId).set(userToUpdate);
        cacheUserToSQLite(userToUpdate);

        if (loggedInUser != null && loggedInUser.getUserId().equals(userId)) {
            this.loggedInUser = userToUpdate;
            this.loggedInUserLiveData.postValue(this.loggedInUser);
        }
    }

    private void cacheUserToSQLite(User user) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            Gson gson = new Gson();

            database.delete(SQLiteHelper.TABLE_USERS, SQLiteHelper.COLUMN_USER_ID + " = ?", new String[]{user.getUserId()});

            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.COLUMN_USER_ID, user.getUserId());
            values.put(SQLiteHelper.COLUMN_USERNAME, user.getUsername());
            values.put(SQLiteHelper.COLUMN_AVATAR_ID, user.getAvatarId());
            values.put(SQLiteHelper.COLUMN_LEVEL, user.getLevel());
            values.put(SQLiteHelper.COLUMN_TITLE_USER, user.getTitle());
            values.put(SQLiteHelper.COLUMN_XP, user.getXp());
            values.put(SQLiteHelper.COLUMN_POWER_POINTS, user.getPowerPoints());
            values.put(SQLiteHelper.COLUMN_COINS, user.getCoins());
            values.put(SQLiteHelper.COLUMN_REGISTRATION_TIMESTAMP, user.getRegistrationTimestamp());
            values.put(SQLiteHelper.COLUMN_ALLIANCE_ID, user.getAllianceId());

            if (user.getBadges() != null) {
                values.put(SQLiteHelper.COLUMN_BADGES_JSON, gson.toJson(user.getBadges()));
            }
            if (user.getEquipped() != null) {
                values.put(SQLiteHelper.COLUMN_EQUIPPED_ITEMS_JSON, gson.toJson(user.getEquipped()));
            }
            if (user.getUserWeapons() != null) {
                values.put(SQLiteHelper.COLUMN_WEAPONS_JSON, gson.toJson(user.getUserWeapons()));
            }

            if (user.getUserItems() != null) {
                values.put(SQLiteHelper.COLUMN_ITEMS_JSON, gson.toJson(user.getUserItems()));
            }

            if (user.getFriendIds()!= null){
                values.put(SQLiteHelper.COLUMN_FRIENDS_JSON, gson.toJson(user.getFriendIds()));
            }

            if(user.getFriendRequests()!=null){
                values.put(SQLiteHelper.COLUMN_FRIEND_REQUESTS_JSON, gson.toJson(user.getFriendRequests()));
            }

            if (user.getAllianceInvites() != null) {
                values.put(SQLiteHelper.COLUMN_ALLIANCE_INVITES_JSON, gson.toJson(user.getAllianceInvites()));
            }

            database.insert(SQLiteHelper.TABLE_USERS, null, values);
        } finally {
            if (database != null) database.close();
        }
    }

    private User getUserFromSQLite(String userId) {
        SQLiteDatabase database = null;
        User user = null;
        try {
            database = dbHelper.getReadableDatabase();
            Cursor cursor = database.query(SQLiteHelper.TABLE_USERS, null, SQLiteHelper.COLUMN_USER_ID + " = ?", new String[]{userId}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = new User();
                Gson gson = new Gson();

                user.setUserId(userId);
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_USERNAME)));
                user.setAvatarId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_AVATAR_ID)));
                user.setLevel(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_LEVEL)));
                user.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TITLE_USER)));
                user.setXp(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_XP)));
                user.setPowerPoints(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_POWER_POINTS)));
                user.setCoins(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_COINS)));
                user.setRegistrationTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_REGISTRATION_TIMESTAMP)));
                user.setAllianceId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_ALLIANCE_ID)));

                Type badgeListType = new TypeToken<List<String>>(){}.getType();
                Type userItemMapType = new TypeToken<Map<String, UserItem>>(){}.getType();
                Type userWeaponMapType = new TypeToken<Map<String, UserWeapon>>(){}.getType();
                Type frirendsListType = new TypeToken<List<String>>(){}.getType();
                Type listStringType = new TypeToken<List<String>>(){}.getType();

                String badgesJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_BADGES_JSON));
                if (badgesJson != null) {
                    user.setBadges(gson.fromJson(badgesJson, badgeListType));
                }

                String equippedJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_EQUIPPED_ITEMS_JSON));
                if (equippedJson != null) {
                    user.setEquipped(gson.fromJson(equippedJson, userItemMapType));
                }

                String userItemsJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_ITEMS_JSON));
                if (userItemsJson != null) {
                    user.setUserItems(gson.fromJson(userItemsJson, userItemMapType));
                }

                String userWeaponsJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_WEAPONS_JSON));
                if (userWeaponsJson != null) {
                    user.setUserWeapons(gson.fromJson(userWeaponsJson, userWeaponMapType)); 
                }

                String friendsJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_FRIENDS_JSON));
                if(friendsJson!=null){
                    user.setFriendIds(gson.fromJson(friendsJson, frirendsListType));
                }

                String friendRequestsJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_FRIEND_REQUESTS_JSON));
                if(friendRequestsJson!=null){
                    user.setFriendRequests(gson.fromJson(friendRequestsJson, frirendsListType));
                }

                String allianceInvitesJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_ALLIANCE_INVITES_JSON));
                if (allianceInvitesJson != null) {
                    user.setAllianceInvites(gson.fromJson(allianceInvitesJson, listStringType));
                }

                cursor.close();
            }
        } finally {
            if (database != null) database.close();
        }
        return user;
    }

    private void deleteUserFromSQLite(String userId) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            database.delete(SQLiteHelper.TABLE_USERS, SQLiteHelper.COLUMN_USER_ID + " = ?", new String[]{userId});
        } finally {
            if (database != null) database.close();
        }
    }
}