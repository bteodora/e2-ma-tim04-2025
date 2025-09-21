package com.example.rpgapp.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.User;

public class ConfirmAllianceSwitchActivity extends AppCompatActivity {

    public static final String EXTRA_NEW_ALLIANCE_ID = "EXTRA_NEW_ALLIANCE_ID";
    // *** OVDE JE ISPRAVKA: DODATA JE OVA LINIJA ***
    public static final String EXTRA_CURRENT_USER_ID = "EXTRA_CURRENT_USER_ID";
    private static final String TAG = "ConfirmAllianceSwitch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String newAllianceId = getIntent().getStringExtra(EXTRA_NEW_ALLIANCE_ID);
        String currentUserId = getIntent().getStringExtra(EXTRA_CURRENT_USER_ID);

        if (newAllianceId == null || currentUserId == null) {
            Log.e(TAG, "Missing newAllianceId or currentUserId");
            finish();
            return;
        }

        UserRepository userRepository = UserRepository.getInstance(this);
        AllianceRepository allianceRepository = AllianceRepository.getInstance(this);

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User currentUser) {
                if (currentUser == null) {
                    Toast.makeText(ConfirmAllianceSwitchActivity.this, "Could not verify user data.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                showConfirmationDialog(allianceRepository, newAllianceId, currentUser);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ConfirmAllianceSwitchActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showConfirmationDialog(AllianceRepository allianceRepository, String newAllianceId, User currentUser) {
        new AlertDialog.Builder(this)
                .setTitle("Switch Alliance?")
                .setMessage("You are already in an alliance. Accepting this invite will make you leave your current one. Are you sure?")
                .setPositiveButton("Accept & Leave", (dialog, which) -> {
                    allianceRepository.acceptAllianceInvite(newAllianceId, currentUser, new UserRepository.RequestCallback() {
                        @Override
                        public void onSuccess() {
                            Intent openAppIntent = new Intent(ConfirmAllianceSwitchActivity.this, HomeActivity.class);
                            openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            openAppIntent.putExtra("NAVIGATE_TO", "ALLIANCE");
                            startActivity(openAppIntent);
                            finish();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(ConfirmAllianceSwitchActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}