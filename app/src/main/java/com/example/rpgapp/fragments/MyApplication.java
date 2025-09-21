package com.example.rpgapp.fragments;

import android.app.Application;
import android.os.StrictMode;

import com.applandeo.materialcalendarview.BuildConfig;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Uključi StrictMode SAMO U DEBUG VERZIJI APLIKACIJE
        // Ovo sprečava da se StrictMode pokreće u finalnoj (production) verziji
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects() // Detektuj nezatvorene SQLite objekte
                    .detectLeakedClosableObjects() // Detektuj i druge nezatvorene objekte (npr. fajlove)
                    .penaltyLog() // Ispiši upozorenje u Logcat
                    .penaltyDeath() // SRUŠI APLIKACIJU kada se problem desi
                    .build());
        }
    }
}