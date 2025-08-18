package com.example.shopapp.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;


public class CheckConnectionTools {
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            // https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("REZ", "Povezan na WIFI");
                    return TYPE_WIFI;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("REZ", "Povezan na celularnu mrezu");
                    return TYPE_MOBILE;
                }
            }
        }else{
        // https://developer.android.com/reference/android/net/NetworkInfo
        //Android is below R
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (null != activeNetwork) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                    return TYPE_WIFI;

                if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                    return TYPE_MOBILE;
            }
        }

        Log.i("REZ", "Nije povezan na internet");
        return TYPE_NOT_CONNECTED;
    }

    public static int calculateTimeTillNextSync(int minutes){
        return 1000 * 60 * minutes;
    }




}
