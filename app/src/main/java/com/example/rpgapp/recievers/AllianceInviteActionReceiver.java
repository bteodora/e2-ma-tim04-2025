package com.example.rpgapp.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.tools.NotificationHelper;

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

        AllianceRepository allianceRepository = AllianceRepository.getInstance(context);
        NotificationHelper notificationHelper = new NotificationHelper(context);

        // Definišemo zajednički callback za obe akcije
        UserRepository.RequestCallback callback = new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                // Kada je operacija uspešna, skloni notifikaciju
                notificationHelper.cancelAllianceNotification();
            }
            @Override
            public void onFailure(Exception e) {
                // Čak i ako ne uspe, skloni notifikaciju da korisnik može ponovo da proba
                notificationHelper.cancelAllianceNotification();
                Toast.makeText(context, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (intent.getAction().equals(ACTION_ACCEPT)) {
            // TODO: Ovde treba dodati kompleksnu logiku provere pre prihvatanja
            // Za sada, samo prihvatamo direktno.
            allianceRepository.acceptAllianceInvite(allianceId, callback);
        } else if (intent.getAction().equals(ACTION_DECLINE)) {
            allianceRepository.declineAllianceInvite(allianceId, callback);
        }
    }
}