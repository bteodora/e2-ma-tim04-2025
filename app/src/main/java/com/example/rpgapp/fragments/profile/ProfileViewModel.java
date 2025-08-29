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
        userRepository = UserRepository.getInstance(application);
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
