package com.example.rpgapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_PRODUCTS = "PRODUCTS";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IMAGE = "image";

    //Dajemo ime bazi
    private static final String DATABASE_NAME = "products.db";
    //i pocetnu verziju baze. Obicno krece od 1
    private static final int DATABASE_VERSION = 1;

    private static final String DB_CREATE = "create table "
            + TABLE_PRODUCTS + "("
            + COLUMN_ID  + " integer primary key autoincrement , "
            + COLUMN_TITLE + " text, "
            + COLUMN_DESCRIPTION + " text, "
            + COLUMN_IMAGE + " integer"
            + ")";


    //Potrebno je dodati konstruktor zbog pravilne inicijalizacije
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //Prilikom kreiranja baze potrebno je da pozovemo odgovarajuce metode biblioteke
    //prilikom kreiranja moramo pozvati db.execSQL za svaku tabelu koju imamo
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("REZ_DB", "ON CREATE SQLITE HELPER");
        db.execSQL(DB_CREATE);
    }

    //kada zelimo da izmenimo tabele, moramo pozvati drop table za sve tabele koje imamo
    //  moramo voditi računa o podacima, pa ćemo onda raditi ovde migracije po potrebi
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("REZ_DB", "ON UPGRADE SQLITE HELPER");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

}
