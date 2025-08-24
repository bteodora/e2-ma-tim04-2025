package com.example.rpgapp.database;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // LiveData za praćenje da li je registracija uspešna
    public MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>();
    // LiveData za praćenje poruka o greškama
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public void registerUser(String email, String password, String username, String avatarId) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            User newUser = new User(username, avatarId);

                            db.collection("users").document(userId)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        firebaseUser.sendEmailVerification()
                                                .addOnCompleteListener(verifyTask -> {
                                                    if (verifyTask.isSuccessful()) {
                                                        Log.d(TAG, "Registracija POTPUNO USPEŠNA.");
                                                        registrationSuccess.postValue(true);
                                                    } else {
                                                        errorMessage.postValue("Greška pri slanju verifikacionog email-a.");
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> errorMessage.postValue("Greška pri čuvanju podataka korisnika."));
                        }
                    } else {
                        errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }
}
