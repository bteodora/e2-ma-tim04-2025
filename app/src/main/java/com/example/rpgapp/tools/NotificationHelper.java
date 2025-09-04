package com.example.rpgapp.tools;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.rpgapp.R;
import com.example.rpgapp.recievers.AllianceInviteActionReceiver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class NotificationHelper {

    private static final String ALLIANCE_CHANNEL_ID = "ALLIANCE_INVITES_CHANNEL";
    public static final int ALLIANCE_NOTIFICATION_ID = 456;
    private Context context;

    private static final String GENERAL_CHANNEL_ID = "GENERAL_NOTIFICATIONS_CHANNEL";
    public static final int GENERAL_NOTIFICATION_ID = 789;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            CharSequence allianceInviteName = "Alliance Invitations";
            String allianceInviteDesc = "Notifications for new alliance invites. These are important and cannot be dismissed.";
            int allianceInviteImportance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel allianceChannel = new NotificationChannel(ALLIANCE_CHANNEL_ID, allianceInviteName, allianceInviteImportance);
            allianceChannel.setDescription(allianceInviteDesc);

            notificationManager.createNotificationChannel(allianceChannel);


            CharSequence generalNotifName = "General Notifications";
            String generalNotifDesc = "General updates, like new members joining.";
            int generalNotifImportance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel generalChannel = new NotificationChannel(GENERAL_CHANNEL_ID, generalNotifName, generalNotifImportance);
            generalChannel.setDescription(generalNotifDesc);

            notificationManager.createNotificationChannel(generalChannel);
        }
    }


    public void showAllianceInviteNotification(String allianceId, String inviterUsername, String allianceName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationHelper", "Cannot show notification, permission not granted.");
                return;
            }
        }

        Intent acceptIntent = new Intent(context, AllianceInviteActionReceiver.class);
        acceptIntent.setAction(AllianceInviteActionReceiver.ACTION_ACCEPT);
        acceptIntent.putExtra(AllianceInviteActionReceiver.EXTRA_ALLIANCE_ID, allianceId);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent declineIntent = new Intent(context, AllianceInviteActionReceiver.class);
        declineIntent.setAction(AllianceInviteActionReceiver.ACTION_DECLINE);
        declineIntent.putExtra(AllianceInviteActionReceiver.EXTRA_ALLIANCE_ID, allianceId);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALLIANCE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_action_person)
                .setContentTitle("Alliance Invitation")
                .setContentText(inviterUsername + " invited you to join " + allianceName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_new, "Accept", acceptPendingIntent)
                .addAction(R.drawable.ic_action_warning, "Decline", declinePendingIntent)

                .setAutoCancel(false);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(ALLIANCE_NOTIFICATION_ID, builder.build());
    }

    public void showSimpleNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationHelper", "Cannot show notification, permission not granted.");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_action_group)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(GENERAL_NOTIFICATION_ID, builder.build());
    }

    public void cancelAllianceNotification() {
        NotificationManagerCompat.from(context).cancel(ALLIANCE_NOTIFICATION_ID);
    }
}
