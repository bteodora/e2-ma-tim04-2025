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
    private static final int DATABASE_VERSION = 15;

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
    public static final String COLUMN_BASE_POWER_POINTS = "base_power_points";

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
    public static final String COLUMN_FCM_TOKEN = "fcm_token";

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
            + COLUMN_BASE_POWER_POINTS + " INTEGER, "

            + COLUMN_COINS + " integer, "
            + COLUMN_REGISTRATION_TIMESTAMP + " integer, "
            + COLUMN_BADGES_JSON + " text, "
            + COLUMN_EQUIPPED_ITEMS_JSON + " text, "
            + COLUMN_WEAPONS_JSON + " text,"
            + COLUMN_ITEMS_JSON + " text,"
            + COLUMN_FRIENDS_JSON + " text,"
            + COLUMN_FRIEND_REQUESTS_JSON+ " text,"
            + COLUMN_ALLIANCE_ID + " text,"
            + COLUMN_ALLIANCE_INVITES_JSON + " text,"
            + COLUMN_FCM_TOKEN + " text"
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
    public static final String COLUMN_DIFFICULTY_TEXT = "difficulty_text";
    public static final String COLUMN_IMPORTANCE_TEXT = "importance_text";

    public static final String COLUMN_TOTAL_XP = "total_xp";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DUE_DATE = "dueDate";
    public static final String COLUMN_RECURRING = "recurring";
    public static final String COLUMN_RECURRING_ID = "recurring_id";
    public static final String COLUMN_TASK_USER_ID = "user_id";
    public static final String COLUMN_CATEGORY_USER_ID = "user_id";
    public static final String COLUMN_CREATION_TIMESTAMP = "creationTimestamp";
    public static final String COLUMN_LAST_ACTION_TIMESTAMP = "lastActionTimestamp";


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
            + COLUMN_DIFFICULTY_TEXT + " TEXT, "
            + COLUMN_IMPORTANCE_TEXT + " TEXT, "
            + COLUMN_TOTAL_XP + " INTEGER, "
            + COLUMN_STATUS + " TEXT,"
            + COLUMN_DUE_DATE + " TEXT,"
            + COLUMN_RECURRING + " INTEGER, "
            + COLUMN_RECURRING_ID + " TEXT, "
            + COLUMN_TASK_USER_ID + " TEXT, "
            + COLUMN_CREATION_TIMESTAMP + " INTEGER,"
            + COLUMN_LAST_ACTION_TIMESTAMP + " INTEGER,"
            + "FOREIGN KEY(" + COLUMN_TASK_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
            + ")";
    //--------------- CATEGORY ---------------------

    public static final String TABLE_CATEGORIES = "CATEGORIES";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_CATEGORY_NAME = "name";
    public static final String COLUMN_CATEGORY_COLOR = "color";

    private static final String DB_CREATE_CATEGORIES = "CREATE TABLE " + TABLE_CATEGORIES + "("
            + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_CATEGORY_NAME + " TEXT NOT NULL, "
            + COLUMN_CATEGORY_COLOR + " TEXT NOT NULL, "
            + COLUMN_CATEGORY_USER_ID + " TEXT, "
            + "FOREIGN KEY(" + COLUMN_CATEGORY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), "
            + "UNIQUE(" + COLUMN_CATEGORY_USER_ID + ", " + COLUMN_CATEGORY_COLOR + ")"
            + ")";


    //--------------- BATTLE ---------------------

    public static final String TABLE_BOSS_BATTLES = "boss_battles";
    public static final String COLUMN_BATTLE_ID = "battle_id";
    public static final String COLUMN_BOSS_LEVEL = "boss_level";
    public static final String COLUMN_BOSS_HP = "boss_hp";
    public static final String COLUMN_REMAINING_ATTACKS = "remaining_attacks";
    public static final String COLUMN_COINS_EARNED = "coins_earned";
    public static final String COLUMN_FINISHED = "finished";
    public static final String COLUMN_ACTIVE_ITEMS = "active_items"; // JSON
    public static final String COLUMN_ACTIVE_WEAPON = "active_weapon"; // JSON
    public static final String COLUMN_USER_BATTLE_ID = "user_id";

    public static final String COLUMN_BATTLE_CREATED_AT = "created_at";

    // SQL CREATE TABLE
    private static final String DB_CREATE_BOSS_BATTLES = "CREATE TABLE " + TABLE_BOSS_BATTLES + " ("
            + COLUMN_BATTLE_ID + " TEXT PRIMARY KEY, "
            + COLUMN_USER_BATTLE_ID + " TEXT NOT NULL, "
            + COLUMN_BOSS_LEVEL + " INTEGER, "
            + COLUMN_BOSS_HP + " INTEGER, "
            + COLUMN_REMAINING_ATTACKS + " INTEGER, "
            + COLUMN_COINS_EARNED + " INTEGER, "
            + COLUMN_FINISHED + " INTEGER, "
            + COLUMN_ACTIVE_ITEMS + " TEXT, "
            + COLUMN_BATTLE_CREATED_AT + " INTEGER, "
            + COLUMN_ACTIVE_WEAPON + " TEXT, "
            + "FOREIGN KEY(" + COLUMN_USER_BATTLE_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
            + ");";


    //--------------- SPECIAL MISSION ---------------------
    public static final String TABLE_SPECIAL_MISSIONS = "SPECIAL_MISSIONS";
    public static final String COLUMN_MISSION_ID = "mission_id";
    public static final String COLUMN_ALLIANCE_ID_FK = "alliance_id";
    public static final String COLUMN_SPECIAL_MISSION_BOSS_HP = "boss_hp";
    public static final String COLUMN_MAX_BOSS_HP = "max_boss_hp";
    public static final String COLUMN_USER_PROGRESS_JSON = "user_progress_json"; // JSON mapa userId -> progress
    public static final String COLUMN_ALLIANCE_PROGRESS = "alliance_progress";
    public static final String COLUMN_TASKS_JSON = "tasks_json"; // JSON lista zadataka sa napretkom
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    private static final String DB_CREATE_SPECIAL_MISSIONS = "CREATE TABLE " + TABLE_SPECIAL_MISSIONS + "("
            + COLUMN_MISSION_ID + " TEXT PRIMARY KEY, "
            + COLUMN_ALLIANCE_ID_FK + " TEXT NOT NULL, "
            + COLUMN_SPECIAL_MISSION_BOSS_HP + " INTEGER, "
            + COLUMN_MAX_BOSS_HP + " INTEGER, "
            + COLUMN_USER_PROGRESS_JSON + " TEXT, "
            + COLUMN_ALLIANCE_PROGRESS + " INTEGER, "
            + COLUMN_TASKS_JSON + " TEXT, "
            + COLUMN_START_TIME + " INTEGER, "
            + COLUMN_DURATION + " INTEGER, "
            + COLUMN_IS_ACTIVE + " INTEGER"
            + ")";



    //---------------------------------------------------

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
        db.execSQL(DB_CREATE_ALLIANCES);
        db.execSQL(DB_CREATE_TASKS);
        db.execSQL(DB_CREATE_CATEGORIES);
        db.execSQL(DB_CREATE_BOSS_BATTLES);
        db.execSQL(DB_CREATE_SPECIAL_MISSIONS);



    }

    //kada zelimo da izmenimo tabele, moramo pozvati drop table za sve tabele koje imamo
    //  moramo voditi računa o podacima, pa ćemo onda raditi ovde migracije po potrebi
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("REZ_DB", "ON UPGRADE SQLITE HELPER");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS); //
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALLIANCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOSS_BATTLES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SPECIAL_MISSIONS);

        onCreate(db);
    }

}
