package com.example.rpgapp.fragments.registration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.rpgapp.database.AuthRepository;

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
}
