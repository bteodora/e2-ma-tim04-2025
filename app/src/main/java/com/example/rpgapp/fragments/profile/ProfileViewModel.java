package com.example.rpgapp.fragments.profile;


import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.AuthRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.FriendshipStatus;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseUser;

public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "ProfileViewModel";
    private AuthRepository authRepository;
    private UserRepository userRepository;

    private MutableLiveData<User> displayedUser = new MutableLiveData<>();
    private MutableLiveData<Boolean> isMyProfile = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();
    private MediatorLiveData<FriendshipStatus> friendshipStatus = new MediatorLiveData<>();
    private LiveData<User> loggedInUserLiveData;
    private String currentProfileId;
    private boolean autoSendPending = false;
    public void setAutoSendFlag() {
        this.autoSendPending = true;
    }
    private AllianceRepository allianceRepository;
    private LiveData<Alliance> myAllianceLiveData;
    public LiveData<String> getSuccessMessage() { return successMessage; }


    public ProfileViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
        userRepository = UserRepository.getInstance(application);
        this.loggedInUserLiveData = userRepository.getLoggedInUserLiveData();
        allianceRepository = AllianceRepository.getInstance(application);
        myAllianceLiveData = Transformations.switchMap(loggedInUserLiveData, user -> {
            if (user != null && user.getAllianceId() != null) {
                return allianceRepository.getAllianceLiveData(user.getAllianceId());
            } else {
                MutableLiveData<Alliance> empty = new MutableLiveData<>();
                empty.setValue(null);
                return empty;
            }
        });

        friendshipStatus.addSource(loggedInUserLiveData, loggedInUser -> calculateFriendshipStatus());
        friendshipStatus.addSource(displayedUser, profileUser -> calculateFriendshipStatus());
        friendshipStatus.addSource(myAllianceLiveData, myAlliance -> calculateFriendshipStatus());
    }

    public LiveData<FriendshipStatus> getFriendshipStatus() {
        return friendshipStatus;
    }

    public void executeAutoSendIfNeeded(FriendshipStatus status) {
        if (autoSendPending && status == FriendshipStatus.NOT_FRIENDS) {
            Log.d("ProfileViewModel", "Automatsko slanje zahteva pokrenuto...");
            autoSendPending = false;
            sendFriendRequest();
        }
    }

    private void calculateFriendshipStatus() {
        User me = loggedInUserLiveData.getValue();
        User other = displayedUser.getValue();
        Alliance myAlliance = myAllianceLiveData.getValue();

        if (me == null || other == null) return;

        if (me.getUserId().equals(other.getUserId())) {
            friendshipStatus.setValue(FriendshipStatus.MY_PROFILE);
        } else if (me.getFriendIds() != null && me.getFriendIds().contains(other.getUserId())) {
            boolean iAmLeader = myAlliance != null && myAlliance.getLeaderId().equals(me.getUserId());

            if (iAmLeader) {
                boolean otherIsInMyAlliance = myAlliance.getMemberIds().contains(other.getUserId());
                boolean inviteIsPending = myAlliance.getPendingInviteIds() != null && myAlliance.getPendingInviteIds().contains(other.getUserId());

                if (otherIsInMyAlliance) {
                    friendshipStatus.setValue(FriendshipStatus.FRIENDS);
                } else if (inviteIsPending) {
                    friendshipStatus.setValue(FriendshipStatus.FRIEND_INVITE_PENDING);
                } else {
                    friendshipStatus.setValue(FriendshipStatus.FRIEND_CAN_BE_INVITED);
                }
            } else {
                friendshipStatus.setValue(FriendshipStatus.FRIENDS);
            }
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
        userRepository.sendFriendRequest(otherUser.getUserId(), (status, e) -> {
            switch (status) {
                case SUCCESS:
                    // Ako je uspešno, ažuriraj UI da prikaže "Pending"
                    friendshipStatus.postValue(FriendshipStatus.PENDING_SENT);
                    // Možeš dodati i neku successMessage ako želiš
                    break;
                case ALREADY_FRIENDS:
                    errorMessage.postValue("You are already friends.");
                    break;
                case REQUEST_ALREADY_SENT:
                    break;
                case CANNOT_ADD_SELF:
                    errorMessage.postValue("You cannot add yourself.");
                    break;
                case FAILURE:
                    errorMessage.postValue("Failed to send request. Please try again.");
                    Log.e(TAG, "Error sending friend request", e);
                    break;
            }
        });
    }

    public void refresh() {
        loadUserProfile(currentProfileId);
    }

    public LiveData<User> getDisplayedUser() { return displayedUser; }
    public LiveData<Boolean> getIsMyProfile() { return isMyProfile; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadUserProfile(@Nullable String userId) {
        this.currentProfileId = userId;
        FirebaseUser currentUser = authRepository.getCurrentUser();
        String loggedInUserId = (currentUser != null) ? currentUser.getUid() : null;

        if (userId == null && loggedInUserId == null) {
            errorMessage.postValue("Korisnik nije specificiran i niko nije ulogovan.");
            return;
        }

        String profileIdToLoad = (userId != null) ? userId : loggedInUserId;
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


    public void acceptFriendRequest() {
        User otherUser = displayedUser.getValue();
        if (otherUser == null) {
            errorMessage.postValue("User data not available.");
            return;
        }

        userRepository.acceptFriendRequest(otherUser.getUserId(), new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                userRepository.refreshLoggedInUser();
                successMessage.postValue("Friend added!");
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to accept request.");
            }
        });
    }

    public void declineFriendRequest() {
        User otherUser = displayedUser.getValue();
        if (otherUser == null) {
            errorMessage.postValue("User data not available.");
            return;
        }

        userRepository.declineFriendRequest(otherUser.getUserId(), new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                successMessage.postValue("Request declined.");
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to decline request.");
            }
        });
    }

    public void inviteFriendToAlliance() {
        User friend = displayedUser.getValue();
        Alliance myAlliance = myAllianceLiveData.getValue();

        if (friend == null || myAlliance == null) {
            errorMessage.postValue("Data not loaded.");
            return;
        }

        allianceRepository.inviteToAlliance(myAlliance.getAllianceId(), friend.getUserId(), new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to send invite.");
            }
        });
    }

    private MutableLiveData<MissionTask> currentSpecialMission = new MutableLiveData<>();
    public LiveData<MissionTask> getCurrentSpecialMission() {
        return currentSpecialMission;
    }

    public void setCurrentSpecialMission(MissionTask mission) {
        currentSpecialMission.setValue(mission);
    }

}
