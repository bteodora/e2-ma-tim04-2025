package com.example.rpgapp.fragments.profile;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.database.AuthRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseUser;

public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "ProfileViewModel";
    private AuthRepository authRepository;
    private UserRepository userRepository;

    private MutableLiveData<User> displayedUser = new MutableLiveData<>();
    private MutableLiveData<Boolean> isMyProfile = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
        userRepository = new UserRepository(application);
    }

    public LiveData<User> getDisplayedUser() { return displayedUser; }
    public LiveData<Boolean> getIsMyProfile() { return isMyProfile; }
    public LiveData<String> getErrorMessage() { return errorMessage; }


    public void loadUserProfile(@Nullable String userId) {
        FirebaseUser currentUser = authRepository.getCurrentUser();

        if (currentUser == null && userId == null) {
            errorMessage.postValue("Niko nije ulogovan!");
            return;
        }

        String profileIdToLoad = (userId == null) ? currentUser.getUid() : userId;

        isMyProfile.postValue(profileIdToLoad.equals(currentUser.getUid()));

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
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    // U ProfileViewModel.java

    public void updateUser(User user) {
        if (user != null) {
            userRepository.updateUser(user);
            displayedUser.postValue(user);
        }
    }
}
