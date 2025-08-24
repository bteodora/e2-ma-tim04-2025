package com.example.rpgapp.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.rpgapp.activities.HomeActivity;
import com.example.rpgapp.database.ProductRepository;
import com.example.rpgapp.tools.CheckConnectionTools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncService extends Service {

    public static String RESULT_CODE = "RESULT_CODE";

    ExecutorService executor = Executors.newSingleThreadExecutor(); //kreira samo jedan thread
    Handler handler = new Handler(Looper.getMainLooper()); //handler koji upravlja glavim thread-om od aplikacije (applications main thread)

    /*
     * Metoda koja se poziva prilikom izvrsavanja zadatka servisa
     * Koristeci Intent mozemo prilikom startovanja servisa proslediti
     * odredjene parametre.
     * */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("REZ", "SyncService onStartCommand");
        int status = CheckConnectionTools.getConnectivityStatus(getApplicationContext());
        if(status == CheckConnectionTools.TYPE_WIFI || status == CheckConnectionTools.TYPE_MOBILE){
            // Alternativa za SyncTask
            executor.execute(() -> {
                //Background work here
                Log.i("REZ", "Background work here - POKRECEMO PRAVU SINHRONIZACIJU");

                // KREIRAMO INSTANCU REPOZITORIJUMA
                ProductRepository repository = new ProductRepository(getApplicationContext());

                // POZIVAMO SINHRONIZACIJU!
                repository.syncFirebaseData(new ProductRepository.SyncCompleteListener() {
                    @Override
                    public void onSyncComplete() {
                        // Sada kada je sinhronizacija gotova, možemo poslati broadcast
                        // da bi se npr. lista u aplikaciji osvežila.
                        handler.post(() -> {
                            Log.i("REZ", "Prava sinhronizacija završena, šaljem obaveštenje.");
                            Intent ints = new Intent(HomeActivity.SYNC_DATA);
                            int intsStatus = CheckConnectionTools.getConnectivityStatus(getApplicationContext());
                            ints.putExtra(RESULT_CODE, intsStatus);
                            sendBroadcast(ints);
                        });
                    }
                });
                // NAPOMENA: Izbrisali smo onaj drugi, suvišni handler.post() blok.
                // Obaveštenje (Broadcast) šaljemo SAMO kada je sinhronizacija zaista gotova.
            });
        }
        stopSelf(); // Servis se sam ugasi nakon što je pokrenuo posao, da ne troši resurse.
        return START_NOT_STICKY;
    }
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
