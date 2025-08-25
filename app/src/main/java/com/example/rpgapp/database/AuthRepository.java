package com.example.rpgapp.database;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private UserRepository userRepository;
    private static final String TAG = "AuthRepository";
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    public MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public MutableLiveData<Boolean> userExpired = new MutableLiveData<>();
    public MutableLiveData<FirebaseUser> loggedInUser = new MutableLiveData<>();
    private Context context;

    public AuthRepository(Context context){
        this.context = context;
        userRepository = new UserRepository(context);
    }



    public void registerUser(String email, String password, String username, String avatarId) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            User newUser = new User(username, avatarId);
                            newUser.setRegistrationTimestamp(System.currentTimeMillis());

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

    public void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            if (firebaseUser.isEmailVerified()) {
                                userRepository.setLoggedInUser(firebaseUser.getUid());
                                loggedInUser.postValue(firebaseUser);
                            } else {
                                checkVerification(firebaseUser);
                            }
                        }
                    } else {
                        Log.w(TAG, "Login failed", task.getException());
                        errorMessage.postValue("Invalid email or password.");
                    }
                });
    }

    public void logout() {
        mAuth.signOut();
        userRepository.logoutUser();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    private void checkVerification(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("registrationTimestamp")) {
                        long registrationTimestamp = documentSnapshot.getLong("registrationTimestamp");
                        long currentTime = System.currentTimeMillis();
                        long time = 24 * 60 * 60 * 1000;

                        if ((currentTime - registrationTimestamp) < time) {
                            Log.d(TAG, "User not verified, but still within 24h window.");
                            errorMessage.postValue("Your account is not activated. Please check your email.");
                            mAuth.signOut();
                        } else {
                            Log.d(TAG, "User not verified and 24h window has passed. Deleting user.");
                            deleteExpiredUser(firebaseUser, userId);
                        }
                    } else {
                        errorMessage.postValue("Error: User data is incomplete.");
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Could not check account status.");
                    mAuth.signOut();
                });
    }

    private void deleteExpiredUser(FirebaseUser firebaseUser, String userId) {
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Expired user data deleted from Firestore.");
                    firebaseUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Expired user deleted from Authentication.");
                                    errorMessage.postValue("Activation link has expired. Your account has been deleted. Please register again.");
                                    userExpired.postValue(true);
                                } else {
                                    Log.w(TAG, "Failed to delete expired user from Authentication.", task.getException());
                                    errorMessage.postValue("Error cleaning up expired account.");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to delete expired user data from Firestore.", e);
                    errorMessage.postValue("Error cleaning up expired account.");
                });
    }

    // U AuthRepository.java

    // Trebaće ti LiveData za praćenje statusa promene lozinke
    public MutableLiveData<Boolean> passwordChangedSuccess = new MutableLiveData<>();

    public void changePassword(String oldPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            errorMessage.postValue("No user is currently logged in.");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                Log.d(TAG, "User re-authenticated successfully.");

                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Log.d(TAG, "Password updated successfully.");
                        passwordChangedSuccess.postValue(true);
                    } else {
                        Log.w(TAG, "Error updating password.", updateTask.getException());
                        errorMessage.postValue("Failed to update password. Please try again.");
                    }
                });

            } else {
                Log.w(TAG, "Re-authentication failed.", reauthTask.getException());
                errorMessage.postValue("Incorrect old password.");
            }
        });
    }
}
