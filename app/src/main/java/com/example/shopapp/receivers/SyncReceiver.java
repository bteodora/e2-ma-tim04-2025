package com.example.shopapp.receivers;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.shopapp.R;
import com.example.shopapp.activities.HomeActivity;
import com.example.shopapp.services.SyncService;
import com.example.shopapp.tools.CheckConnectionTools;

import java.util.Objects;


/*
 * BroadcastReceiver je komonenta koja moze da reaguje na poruke drugih delova
 * samog sistema kao i korisnicki definisanih. Cesto se koristi u sprezi sa
 * servisima i asinhronim zadacima.
 *
 * Pored toga on moze da reaguje i na neke sistemske dogadjaje prispece sms poruke
 * paljenje uredjaja, novi poziv isl.
 */
public class SyncReceiver extends BroadcastReceiver {

    private static int NOTIFICATION_ID = 1;
    private static String SYNC_CHANNEL_ID = "Sync channel";
    public boolean isPermissions = false;
    private String[] permissions = {Manifest.permission.POST_NOTIFICATIONS};

    /*
     * Intent je bitan parametar za BroadcastReceiver. Kada posaljemo neku poruku,
     * ovaj Intent cuva akciju i podatke koje smo mu poslali.
     * */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("REZ", "onReceive");
        // STARIJE VERZIJE ANDROIDA
//        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, SYNC_CHANNEL_ID);

        /*
         * Posto nas BroadcastReceiver reaguje samo na jednu akciju koju smo definisali
         * dobro je da proverimo da li som dobili bas tu akciju. Ako jesmo onda mozemo
         * preuzeti i sadrzaj ako ga ima.
         *
         * Voditi racuna o tome da se naziv akcije kada korisnik salje Intent mora poklapati sa
         * nazivom akcije kada akciju proveravamo unutar BroadcastReceiver-a. Isto vazi i za podatke.
         * Dobra praksa je da se ovi nazivi izdvoje unutar neke staticke promenljive.
         * */
        if (Objects.equals(intent.getAction(), HomeActivity.SYNC_DATA)) {
            int resultCode = Objects.requireNonNull(intent.getExtras()).getInt(SyncService.RESULT_CODE);
            Bitmap bm;
            Intent wiFiintent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            PendingIntent pIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, wiFiintent, PendingIntent.FLAG_IMMUTABLE);

            if (resultCode == CheckConnectionTools.TYPE_NOT_CONNECTED) {
                bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_network_wifi);
                mBuilder.setSmallIcon(R.drawable.ic_action_about);
                mBuilder.setContentTitle(context.getString(R.string.autosync_problem));
                mBuilder.setContentText(context.getString(R.string.no_internet));
                mBuilder.setContentIntent(pIntent);
                mBuilder.addAction(R.drawable.ic_action_network_wifi, context.getString(R.string.turn_wifi_on), pIntent);
                mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            } else if (resultCode == CheckConnectionTools.TYPE_MOBILE) {
                bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_network_cell);
                mBuilder.setSmallIcon(R.drawable.ic_action_warning);
                mBuilder.setContentTitle(context.getString(R.string.autosync_warning));
                mBuilder.setContentText(context.getString(R.string.connect_to_wifi));
                mBuilder.addAction(R.drawable.ic_action_network_wifi, context.getString(R.string.turn_wifi_on), pIntent);
            } else {
                bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground);
                mBuilder.setSmallIcon(R.drawable.ic_action_refresh_w);
                mBuilder.setContentTitle(context.getString(R.string.autosync));
                mBuilder.setContentText(context.getString(R.string.good_news_sync));
            }
            mBuilder.setLargeIcon(bm);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity)context, permissions, 101);
            }
            else {
                notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            }


        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 101) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("ShopApp", "permission " + permissions[i] + " " + grantResults[i]);
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    isPermissions = false;
                }
            }
        }

        if (!isPermissions) {
            Log.e("ShopApp", "Error: no permission");
        }

    }




}
