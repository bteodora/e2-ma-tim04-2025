package com.example.rpgapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.model.User;
import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.tools.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProcessInviteActivity extends AppCompatActivity {

    public static final String ACTION_ACCEPT = "com.example.rpgapp.ACTION_ACCEPT_ALLIANCE_INVITE";
    public static final String ACTION_DECLINE = "com.example.rpgapp.ACTION_DECLINE_ALLIANCE_INVITE";
    public static final String EXTRA_ALLIANCE_ID = "EXTRA_ALLIANCE_ID";
    private static final String TAG = "ProcessInviteActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String allianceId = intent.getStringExtra(EXTRA_ALLIANCE_ID);
        String action = intent.getAction();

        if (allianceId == null || action == null) {
            finish();
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Please open the app and log in to respond.", Toast.LENGTH_LONG).show();
            new NotificationHelper(this).cancelAllianceNotification();
            finish();
            return;
        }
        String currentUserId = firebaseUser.getUid();

        AllianceRepository allianceRepository = AllianceRepository.getInstance(this);
        NotificationHelper notificationHelper = new NotificationHelper(this);
        UserRepository userRepository = UserRepository.getInstance(this);

        if (action.equals(ACTION_ACCEPT)) {
            userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    if (user == null) {
                        Log.e(TAG, "User data is null for userId: " + currentUserId);
                        Toast.makeText(ProcessInviteActivity.this, "Could not verify user data.", Toast.LENGTH_SHORT).show();
                        notificationHelper.cancelAllianceNotification();
                        finish();
                        return;
                    }

                    if (user.getAllianceId() != null) {
                        String oldAllianceId = user.getAllianceId();
                        allianceRepository.getAllianceById(oldAllianceId, oldAlliance -> {
                            if (oldAlliance != null) {
                                boolean isLeader = user.getUserId().equals(oldAlliance.getLeaderId());
                                if (isLeader) {
                                    notificationHelper.cancelAllianceNotification();
                                    Toast.makeText(ProcessInviteActivity.this, "As a leader, you must first disband your current alliance.", Toast.LENGTH_LONG).show();
                                    finish();
                                    return;
                                }
                                if (oldAlliance.isMissionStarted()) {
                                    notificationHelper.cancelAllianceNotification();
                                    Toast.makeText(ProcessInviteActivity.this, "Cannot accept invite: A mission is active in your current alliance.", Toast.LENGTH_LONG).show();
                                    finish();

                                } else {
                                    notificationHelper.cancelAllianceNotification();
                                    Intent confirmIntent = new Intent(ProcessInviteActivity.this, ConfirmAllianceSwitchActivity.class);
                                    confirmIntent.putExtra(ConfirmAllianceSwitchActivity.EXTRA_NEW_ALLIANCE_ID, allianceId);
                                    confirmIntent.putExtra(ConfirmAllianceSwitchActivity.EXTRA_CURRENT_USER_ID, user.getUserId());

                                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(confirmIntent);
                                    finish();
                                }
                            } else {
                                acceptInvite(allianceRepository, notificationHelper, allianceId, user);
                            }
                        });
                    } else {
                        acceptInvite(allianceRepository, notificationHelper, allianceId, user);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to get user data: ", e);
                    finish();
                }
            });
        } else if (action.equals(ACTION_DECLINE)) {
            allianceRepository.declineAllianceInvite(allianceId, currentUserId, new UserRepository.RequestCallback() {
                @Override
                public void onSuccess() {
                    notificationHelper.cancelAllianceNotification();
                    finish();
                }
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to decline invite: ", e);
                    notificationHelper.cancelAllianceNotification();
                    Toast.makeText(ProcessInviteActivity.this, "Decline failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private void acceptInvite(AllianceRepository allianceRepository, NotificationHelper notificationHelper, String allianceId, User currentUser) {
        allianceRepository.acceptAllianceInvite(allianceId, currentUser, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                notificationHelper.cancelAllianceNotification();
                Intent openAppIntent = new Intent(ProcessInviteActivity.this, HomeActivity.class);
                openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                openAppIntent.putExtra("NAVIGATE_TO", "ALLIANCE");
                startActivity(openAppIntent);
                finish();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to accept invite: ", e);
                notificationHelper.cancelAllianceNotification();
                Toast.makeText(ProcessInviteActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}