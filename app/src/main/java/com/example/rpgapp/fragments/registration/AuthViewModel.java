package com.example.rpgapp.fragments.registration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.rpgapp.database.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    public void register(String email, String password, String username, String avatarId) {
        authRepository.registerUser(email, password, username, avatarId);
    }

    public LiveData<Boolean> getRegistrationSuccess() {
        return authRepository.registrationSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return authRepository.errorMessage;
    }

    public LiveData<Boolean> getUserExpired() {
        return authRepository.userExpired;
    }

    public void login(String email, String password) {
        authRepository.loginUser(email, password);
    }

    public LiveData<FirebaseUser> getLoggedInUser() {
        return authRepository.loggedInUser;
    }

    public LiveData<Boolean> getUserDeletedAndShouldRegisterAgain() {
        return authRepository.userExpired;
    }
}
