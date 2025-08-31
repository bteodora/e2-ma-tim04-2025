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

    private static final String DATABASE_NAME = "rpgapp_final.db";
    private static final int DATABASE_VERSION = 6; 

    private static final String DB_CREATE = "create table "
            + TABLE_PRODUCTS + "("
            + COLUMN_ID  + " integer primary key autoincrement , "
            + COLUMN_TITLE + " text, "
            + COLUMN_DESCRIPTION + " text, "
            + COLUMN_IMAGE + " integer"
            + ")";

    //--------------- USER ---------------------
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
    public static final String COLUMN_WEAPONS_JSON = "weapons_json";
    public static final String COLUMN_ITEMS_JSON = "items_json";
    public static final String COLUMN_FRIENDS_JSON = "friends_json";
    public static final String COLUMN_FRIEND_REQUESTS_JSON = "friend_requests_json";
    public static final String COLUMN_ALLIANCE_ID = "alliance_id";
    public static final String COLUMN_ALLIANCE_INVITES_JSON = "alliance_invites_json";

    //--------------- ALLIANCE ---------------------
    public static final String TABLE_ALLIANCES = "ALLIANCES";
    public static final String COLUMN_ALLIANCE_PK_ID = "_id";
    public static final String COLUMN_ALLIANCE_DOC_ID = "alliance_doc_id";
    public static final String COLUMN_ALLIANCE_NAME = "name";
    public static final String COLUMN_LEADER_ID = "leader_id";
    public static final String COLUMN_LEADER_USERNAME = "leader_username";
    public static final String COLUMN_MEMBER_IDS_JSON = "member_ids_json";
    public static final String COLUMN_PENDING_INVITE_IDS_JSON = "pending_invite_ids_json";
    public static final String COLUMN_MISSION_STARTED = "mission_started";
    public static final String COLUMN_CREATED_AT = "created_at";

    private static final String DB_CREATE_ALLIANCES = "create table "
            + TABLE_ALLIANCES + "("
            + COLUMN_ALLIANCE_PK_ID + " integer primary key autoincrement, "
            + COLUMN_ALLIANCE_DOC_ID + " text unique not null, "
            + COLUMN_ALLIANCE_NAME + " text, "
            + COLUMN_LEADER_ID + " text, "
            + COLUMN_LEADER_USERNAME + " text, "
            + COLUMN_MEMBER_IDS_JSON + " text, "
            + COLUMN_PENDING_INVITE_IDS_JSON + " text, "
            + COLUMN_MISSION_STARTED + " integer, "
            + COLUMN_CREATED_AT + " integer"
            + ")";

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
            + COLUMN_WEAPONS_JSON + " text,"
            + COLUMN_ITEMS_JSON + " text,"
            + COLUMN_FRIENDS_JSON + " text,"
            + COLUMN_FRIEND_REQUESTS_JSON+ " text"
            + ")";

    //--------------- TASK ---------------------
    public static final String TABLE_TASKS = "TASKS";
    public static final String COLUMN_TASK_ID = "task_id";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_FREQUENCY = "frequency";
    public static final String COLUMN_INTERVAL = "interval";
    public static final String COLUMN_INTERVAL_UNIT = "interval_unit";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TITLE_TASK = "title";
    public static final String COLUMN_DESCRIPTION_TASK = "description";
    public static final String COLUMN_DIFFICULTY_XP = "difficulty_xp";
    public static final String COLUMN_IMPORTANCE_XP = "importance_xp";
    public static final String COLUMN_TOTAL_XP = "total_xp";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DUE_DATE = "dueDate";

    private static final String DB_CREATE_TASKS = "CREATE TABLE " + TABLE_TASKS + "("
            + COLUMN_TASK_ID + " TEXT PRIMARY KEY, "
            + COLUMN_TITLE_TASK + " TEXT, "
            + COLUMN_DESCRIPTION_TASK + " TEXT, "
            + COLUMN_CATEGORY + " TEXT, "
            + COLUMN_COLOR + " TEXT, "
            + COLUMN_FREQUENCY + " TEXT, "
            + COLUMN_INTERVAL + " INTEGER, "
            + COLUMN_INTERVAL_UNIT + " TEXT, "
            + COLUMN_START_DATE + " TEXT, "
            + COLUMN_END_DATE + " TEXT, "
            + COLUMN_TIME + " TEXT, "
            + COLUMN_DIFFICULTY_XP + " INTEGER, "
            + COLUMN_IMPORTANCE_XP + " INTEGER, "
            + COLUMN_TOTAL_XP + " INTEGER, "
            + COLUMN_STATUS + " TEXT,"
            + COLUMN_DUE_DATE + " TEXT"
            + ")";


    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("REZ_DB", "ON CREATE SQLITE HELPER");
        db.execSQL(DB_CREATE);
        db.execSQL(DB_CREATE_USERS);
        db.execSQL(DB_CREATE_ALLIANCES); 
        db.execSQL(DB_CREATE_TASKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("REZ_DB", "ON UPGRADE SQLITE HELPER");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALLIANCES); 
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }
}