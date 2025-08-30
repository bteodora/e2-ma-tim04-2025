package com.example.rpgapp.fragments.profile;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.database.AuthRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.FriendshipStatus;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseUser;

public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "ProfileViewModel";
    private AuthRepository authRepository;
    private UserRepository userRepository;

    private MutableLiveData<User> displayedUser = new MutableLiveData<>();
    private MutableLiveData<Boolean> isMyProfile = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private MediatorLiveData<FriendshipStatus> friendshipStatus = new MediatorLiveData<>();
    private LiveData<User> loggedInUserLiveData;


    public ProfileViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
        userRepository = UserRepository.getInstance(application);
        this.loggedInUserLiveData = userRepository.getLoggedInUserLiveData();

        friendshipStatus.addSource(loggedInUserLiveData, loggedInUser -> {
            calculateFriendshipStatus(loggedInUser, displayedUser.getValue());
        });

        friendshipStatus.addSource(displayedUser, profileUser -> {
            calculateFriendshipStatus(loggedInUserLiveData.getValue(), profileUser);
        });
    }

    public LiveData<FriendshipStatus> getFriendshipStatus() {
        return friendshipStatus;
    }

    private void calculateFriendshipStatus(User me, User other) {
        if (me == null || other == null) {
            return;
        }

        if (me.getUserId().equals(other.getUserId())) {
            friendshipStatus.setValue(FriendshipStatus.MY_PROFILE);
        } else if (me.getFriendIds() != null && me.getFriendIds().contains(other.getUserId())) {
            friendshipStatus.setValue(FriendshipStatus.FRIENDS);
        } else if (me.getFriendRequests() != null && me.getFriendRequests().contains(other.getUserId())) {
            friendshipStatus.setValue(FriendshipStatus.PENDING_RECEIVED);
        } else if (other.getFriendRequests() != null && other.getFriendRequests().contains(me.getUserId())) {
            friendshipStatus.setValue(FriendshipStatus.PENDING_SENT);
        } else {
            friendshipStatus.setValue(FriendshipStatus.NOT_FRIENDS);
        }
    }

    public void sendFriendRequest() {
        User otherUser = displayedUser.getValue();

        if (otherUser == null) {
            errorMessage.postValue("Cannot send request, user data is not loaded.");
            return;
        }

        userRepository.sendFriendRequest(otherUser.getUserId(), new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                friendshipStatus.postValue(FriendshipStatus.PENDING_SENT);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to send friend request. Please try again.");
                Log.e(TAG, "Error sending friend request", e);
            }
        });
    }

    public LiveData<User> getDisplayedUser() { return displayedUser; }
    public LiveData<Boolean> getIsMyProfile() { return isMyProfile; }
    public LiveData<String> getErrorMessage() { return errorMessage; }


    public void loadUserProfile(@Nullable String userId) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        String loggedInUserId = (currentUser != null) ? currentUser.getUid() : null;

        // Ako nemamo ni prosleđen ID, ni ulogovanog korisnika, ne možemo ništa.
        if (userId == null && loggedInUserId == null) {
            errorMessage.postValue("Korisnik nije specificiran i niko nije ulogovan.");
            return;
        }

        // Određujemo koji profil treba učitati. Ako je userId null, učitavamo ulogovanog korisnika.
        String profileIdToLoad = (userId != null) ? userId : loggedInUserId;

        // Proveravamo da li je to naš profil.
        // isMyProfile je true ako je ID koji učitavamo jednak ID-ju ulogovanog korisnika.
        isMyProfile.postValue(profileIdToLoad.equals(loggedInUserId));

        userRepository.getUserById(profileIdToLoad, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                if (user != null) {
                    displayedUser.postValue(user);
                } else {
                    errorMessage.postValue("Korisnik nije pronađen.");
                }
            }

            @Override
            public void onError(Exception e) {
                errorMessage.postValue("Greška pri učitavanju profila.");
                Log.e(TAG, "Error loading user profile", e);
            }
        });
    }

    public void updateUser(User user) {
        if (user != null) {
            userRepository.updateUser(user);
            displayedUser.postValue(user);
        }
    }
}
