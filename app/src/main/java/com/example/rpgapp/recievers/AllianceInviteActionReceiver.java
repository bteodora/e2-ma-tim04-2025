package com.example.rpgapp.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.example.rpgapp.activities.ConfirmAllianceSwitchActivity;
import com.example.rpgapp.activities.HomeActivity;
import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.User;
import com.example.rpgapp.tools.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AllianceInviteActionReceiver extends BroadcastReceiver {

    public static final String ACTION_ACCEPT = "com.example.rpgapp.ACTION_ACCEPT_ALLIANCE_INVITE";
    public static final String ACTION_DECLINE = "com.example.rpgapp.ACTION_DECLINE_ALLIANCE_INVITE";
    public static final String EXTRA_ALLIANCE_ID = "EXTRA_ALLIANCE_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        String allianceId = intent.getStringExtra(EXTRA_ALLIANCE_ID);
        if (allianceId == null || intent.getAction() == null) {
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(context, "Please open the app and log in to respond.", Toast.LENGTH_LONG).show();
            new NotificationHelper(context).cancelAllianceNotification();
            return;
        }
        String currentUserId = firebaseUser.getUid();

        AllianceRepository allianceRepository = AllianceRepository.getInstance(context);
        NotificationHelper notificationHelper = new NotificationHelper(context);
        UserRepository userRepository = UserRepository.getInstance(context);

        userRepository.setLoggedInUser(currentUserId);

        if (intent.getAction().equals(ACTION_ACCEPT)) {
            User currentUser = userRepository.getLoggedInUser();

            if (currentUser != null && currentUser.getAllianceId() != null) {
                String oldAllianceId = currentUser.getAllianceId();
                allianceRepository.getAllianceById(oldAllianceId, oldAlliance -> {
                    if (oldAlliance != null) {
                        boolean isLeader = currentUser.getUserId().equals(oldAlliance.getLeaderId());
                        if (isLeader) {
                            notificationHelper.cancelAllianceNotification();
                            Toast.makeText(context, "As a leader, you must first disband your current alliance.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (oldAlliance.isMissionStarted()) {
                            notificationHelper.cancelAllianceNotification();
                            Toast.makeText(context, "Cannot accept invite: A mission is active in your current alliance.", Toast.LENGTH_LONG).show();
                        } else {
                            notificationHelper.cancelAllianceNotification();
                            Intent confirmIntent = new Intent(context, ConfirmAllianceSwitchActivity.class);
                            confirmIntent.putExtra(ConfirmAllianceSwitchActivity.EXTRA_NEW_ALLIANCE_ID, allianceId);
                            confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(confirmIntent);
                        }
                    }
                });
            } else {
                allianceRepository.acceptAllianceInvite(allianceId, new UserRepository.RequestCallback() {
                    @Override
                    public void onSuccess() {
                        notificationHelper.cancelAllianceNotification();
                        Intent openAppIntent = new Intent(context, HomeActivity.class);
                        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        openAppIntent.putExtra("NAVIGATE_TO", "ALLIANCE");
                        context.startActivity(openAppIntent);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        notificationHelper.cancelAllianceNotification();
                        Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if (intent.getAction().equals(ACTION_DECLINE)) {
            UserRepository.RequestCallback declineCallback = new UserRepository.RequestCallback() {
                @Override
                public void onSuccess() {
                    notificationHelper.cancelAllianceNotification();
                }
                @Override
                public void onFailure(Exception e) {
                    notificationHelper.cancelAllianceNotification();
                    Toast.makeText(context, "Decline failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            allianceRepository.declineAllianceInvite(allianceId, declineCallback);
        }
    }
}