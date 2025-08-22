package com.example.rpgapp.tools;


import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.rpgapp.database.DBContentProvider;
import com.example.rpgapp.database.SQLiteHelper;

public class Util {
    public static void initDB(Activity activity) {
        SQLiteHelper dbHelper = new SQLiteHelper(activity);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.i("REZ_DB", "ENTRY INSERT TO DATABASE");
        {
            ContentValues entry = new ContentValues();
            entry.put(SQLiteHelper.COLUMN_TITLE, "Iphone");
            entry.put(SQLiteHelper.COLUMN_DESCRIPTION, "15 pro");
            entry.put(SQLiteHelper.COLUMN_IMAGE, -1);
            // content resolver salje neki zahtev content provideru sa nekim informacijama
            // i vracama nam se odgovor od content provider-a - u ovom slucaju uri
            activity.getContentResolver().insert(DBContentProvider.CONTENT_URI_PRODUCTS, entry);

            entry = new ContentValues();
            entry.put(SQLiteHelper.COLUMN_TITLE, "Samsung");
            entry.put(SQLiteHelper.COLUMN_DESCRIPTION, "S23 ultra");
            entry.put(SQLiteHelper.COLUMN_IMAGE, -1);

            activity.getContentResolver().insert(DBContentProvider.CONTENT_URI_PRODUCTS, entry);
        }

        db.close();
    }
}