package com.example.rpgapp.fragments.friends;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.User;
import java.util.ArrayList;
import java.util.List;

public class FriendsViewModel extends AndroidViewModel {
    private static final String TAG = "RPGApp_Debug";
    private UserRepository userRepository;

    // displayedUsers će sada biti MediatorLiveData da bi mogao da reaguje na druge LiveData izvore
    public MediatorLiveData<List<User>> displayedUsers = new MediatorLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private LiveData<User> currentUserLiveData;
    private AllianceRepository allianceRepository;

    private MutableLiveData<Boolean> allianceCreationStatus = new MutableLiveData<>();
    public LiveData<Boolean> getAllianceCreationStatus() { return allianceCreationStatus; }

    public FriendsViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance(application);
        allianceRepository = AllianceRepository.getInstance(application);
        this.currentUserLiveData = userRepository.getLoggedInUserLiveData();

        displayedUsers.addSource(currentUserLiveData, user -> {
            if (user != null) {
                executeFriendLoading(user);
            } else {
                displayedUsers.setValue(new ArrayList<>());
            }
        });
    }

    private void executeFriendLoading(User currentUser) {

        if (currentUser.getFriendIds() == null || currentUser.getFriendIds().isEmpty()) {
            displayedUsers.postValue(new ArrayList<>());
            isLoading.postValue(false);
            return;
        }

        isLoading.postValue(true);
        userRepository.getFriendsProfiles(currentUser, new UserRepository.FriendsCallback() {
            @Override
            public void onFriendsLoaded(List<User> friends) {
                displayedUsers.postValue(friends);
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.postValue("Failed to load friends.");
                isLoading.postValue(false);
            }
        });
    }


    public LiveData<List<User>> getDisplayedUsers() { return displayedUsers; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public LiveData<User> getCurrentUserLiveDataForDebug() {
        return currentUserLiveData;
    }
    public void searchUsers(String query) {
        isLoading.postValue(true);
        userRepository.searchUsersByUsername(query, new UserRepository.UserSearchCallback() {
            @Override
            public void onUsersFound(List<User> users) {
                displayedUsers.postValue(users);
                isLoading.postValue(false);
            }
            @Override
            public void onError(Exception e) {
                errorMessage.postValue("Search failed.");
                isLoading.postValue(false);
            }
        });
    }

    public void createAlliance(String name, List<String> invitedFriendIds) {
        allianceRepository.createAlliance(name, invitedFriendIds, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                allianceCreationStatus.postValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FriendsViewModel", "Alliance creation failed", e);
                allianceCreationStatus.postValue(false);
            }
        });
    }

    public void resetAllianceCreationStatus() {
        allianceCreationStatus.setValue(null);
    }

    public void sendFriendRequest(String targetUserId, UserRepository.SendRequestCallback callback) {
        userRepository.sendFriendRequest(targetUserId, callback);
    }
}