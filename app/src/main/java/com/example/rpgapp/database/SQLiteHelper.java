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
    private static final String DATABASE_NAME = "rpgapp_final.db";
    //i pocetnu verziju baze. Obicno krece od 1
    private static final int DATABASE_VERSION = 1;

    private static final String DB_CREATE = "create table "
            + TABLE_PRODUCTS + "("
            + COLUMN_ID  + " integer primary key autoincrement , "
            + COLUMN_TITLE + " text, "
            + COLUMN_DESCRIPTION + " text, "
            + COLUMN_IMAGE + " integer"
            + ")";

    //--------------- USER ---------------------
    //TODO dodati za ostale klase isto ovako:
    public static final String TABLE_USERS = "USERS";
    public static final String COLUMN_USER_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_AVATAR_ID = "avatar_id";
    public static final String COLUMN_LEVEL = "level";
    public static final String COLUMN_TITLE_USER = "title";
    public static final String COLUMN_XP = "xp";
    public static final String COLUMN_POWER_POINTS = "power_points";
    public static final String COLUMN_COINS = "coins";
    public static final String COLUMN_REGISTRATION_TIMESTAMP = "registration_timestamp";
    public static final String COLUMN_BADGES_JSON = "badges_json";
    public static final String COLUMN_EQUIPPED_ITEMS_JSON = "equipped_items_json";
    public static final String COLUMN_INVENTORY_JSON = "inventory_json";

    private static final String DB_CREATE_USERS = "create table "
            + TABLE_USERS + "("
            + COLUMN_USER_ID + " text primary key, "
            + COLUMN_USERNAME + " text not null, "
            + COLUMN_AVATAR_ID + " text, "
            + COLUMN_LEVEL + " integer, "
            + COLUMN_TITLE_USER + " text, "
            + COLUMN_XP + " integer, "
            + COLUMN_POWER_POINTS + " integer, "
            + COLUMN_COINS + " integer, "
            + COLUMN_REGISTRATION_TIMESTAMP + " integer, "
            + COLUMN_BADGES_JSON + " text, "
            + COLUMN_EQUIPPED_ITEMS_JSON + " text, "
            + COLUMN_INVENTORY_JSON + " text"
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
        db.execSQL(DB_CREATE_USERS);
    }

    //kada zelimo da izmenimo tabele, moramo pozvati drop table za sve tabele koje imamo
    //  moramo voditi računa o podacima, pa ćemo onda raditi ovde migracije po potrebi
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("REZ_DB", "ON UPGRADE SQLITE HELPER");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS); //
        onCreate(db);
    }

}
