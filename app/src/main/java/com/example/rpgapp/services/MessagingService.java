package com.example.rpgapp.services;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.tools.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "New FCM message received!");

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String type = remoteMessage.getData().get("type");
            if (type == null) {
                Log.w(TAG, "Message received without a 'type' field.");
                return;
            }

            NotificationHelper helper = new NotificationHelper(this);

            if ("ALLIANCE_INVITE".equals(type)) {
                String allianceId = remoteMessage.getData().get("allianceId");
                String leaderUsername = remoteMessage.getData().get("leaderUsername");
                String allianceName = remoteMessage.getData().get("allianceName");

                if (allianceId != null && leaderUsername != null && allianceName != null) {
                    helper.showAllianceInviteNotification(allianceId, leaderUsername, allianceName);
                }
            } else if ("MEMBER_JOINED".equals(type)) {
                String title = remoteMessage.getData().get("title");
                String body = remoteMessage.getData().get("body");
                if (title != null && body != null) {
                    helper.showSimpleNotification(title, body);
                }
            }
            else if ("NEW_CHAT_MESSAGE".equals(type)) {
                String title = remoteMessage.getData().get("title");
                String body = remoteMessage.getData().get("body");
                if (title != null && body != null) {
                    helper.showSimpleNotification(title, body);
                }
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance(this).updateUserFcmToken(currentUser.getUid(), token);
        } else {
            Log.d(TAG, "User not logged in, token will be updated on next login.");
        }
    }
}