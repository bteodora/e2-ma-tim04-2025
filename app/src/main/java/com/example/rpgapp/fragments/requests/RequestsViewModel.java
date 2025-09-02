package com.example.rpgapp.fragments.requests;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.User;

import java.util.ArrayList;
import java.util.List;

public class RequestsViewModel extends AndroidViewModel {
    private UserRepository userRepository;
    private MutableLiveData<List<User>> requestingUsers = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private LiveData<User> loggedInUserLiveData;

    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public RequestsViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance(application);

        loggedInUserLiveData = userRepository.getLoggedInUserLiveData();

        loggedInUserLiveData.observeForever(loggedInUser -> {
            loadRequests();
        });
    }

    public LiveData<List<User>> getRequestingUsers() { return requestingUsers; }

    public void loadRequests() {
        User currentUser = userRepository.getLoggedInUser();
        if (currentUser != null && currentUser.getFriendRequests() != null) {
            userRepository.getRequestingUsers(currentUser.getFriendRequests(), new UserRepository.FriendsCallback() {
                @Override
                public void onFriendsLoaded(List<User> users) {
                    requestingUsers.postValue(users);
                }
                @Override
                public void onError(Exception e) {
                    errorMessage.postValue("Failed to load requests.");
                }
            });
        } else {
            requestingUsers.postValue(new ArrayList<>());
        }
    }

    public void acceptRequest(String userId) {
        userRepository.acceptFriendRequest(userId, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                successMessage.postValue("Friend added!");
                userRepository.refreshLoggedInUser();
                loadRequests();
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to accept request.");
            }
        });
    }

    public void declineRequest(String userId) {
        userRepository.declineFriendRequest(userId, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                successMessage.postValue("Request declined.");
                loadRequests();
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to decline request.");
            }
        });
    }
}