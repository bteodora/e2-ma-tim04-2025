package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpecialMissionRepository {

    private static volatile SpecialMissionRepository INSTANCE;
    private final SQLiteHelper helper;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SpecialMissionRepository(Context context) {
        this.helper = new SQLiteHelper(context.getApplicationContext());
    }

    public static SpecialMissionRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SpecialMissionRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SpecialMissionRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // --------------------- SAVE / UPDATE ASINHRONO ---------------------
    public void saveMission(SpecialMission mission, UserRepository.RequestCallback callback) {
        new Thread(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(SQLiteHelper.COLUMN_MISSION_ID, mission.getMissionId());
                cv.put(SQLiteHelper.COLUMN_ALLIANCE_ID_FK, mission.getAllianceId());
                cv.put(SQLiteHelper.COLUMN_BOSS_HP, mission.getBossHP());
                cv.put(SQLiteHelper.COLUMN_MAX_BOSS_HP, mission.getMaxBossHP());
                cv.put(SQLiteHelper.COLUMN_USER_PROGRESS_JSON, gson.toJson(mission.getUserTaskProgress()));
                cv.put(SQLiteHelper.COLUMN_ALLIANCE_PROGRESS, mission.getAllianceProgress());
                cv.put(SQLiteHelper.COLUMN_TASKS_JSON, gson.toJson(mission.getTasks()));
                cv.put(SQLiteHelper.COLUMN_START_TIME, mission.getStartTime());
                cv.put(SQLiteHelper.COLUMN_DURATION, mission.getDurationMillis());
                cv.put(SQLiteHelper.COLUMN_IS_ACTIVE, mission.isActive() ? 1 : 0);

                db.insertWithOnConflict(SQLiteHelper.TABLE_SPECIAL_MISSIONS, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);

                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        }).start();
    }

    public void updateMission(SpecialMission mission, UserRepository.RequestCallback callback) {
        new Thread(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(SQLiteHelper.COLUMN_BOSS_HP, mission.getBossHP());
                cv.put(SQLiteHelper.COLUMN_ALLIANCE_PROGRESS, mission.getAllianceProgress());
                cv.put(SQLiteHelper.COLUMN_USER_PROGRESS_JSON, gson.toJson(mission.getUserTaskProgress()));
                cv.put(SQLiteHelper.COLUMN_TASKS_JSON, gson.toJson(mission.getTasks()));
                cv.put(SQLiteHelper.COLUMN_IS_ACTIVE, mission.isActive() ? 1 : 0);

                db.update(SQLiteHelper.TABLE_SPECIAL_MISSIONS, cv,
                        SQLiteHelper.COLUMN_MISSION_ID + "=?",
                        new String[]{mission.getMissionId()});

                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        }).start();
    }

    // --------------------- GET MISSION AS LIVE DATA ---------------------
    public LiveData<SpecialMission> getMission(String allianceId) {
        MutableLiveData<SpecialMission> liveData = new MutableLiveData<>();
        new Thread(() -> {
            SpecialMission mission = getMissionSync(allianceId);
            mainHandler.post(() -> liveData.setValue(mission));
        }).start();
        return liveData;
    }

    // --------------------- SYNCHRONOUS GET ---------------------
    private SpecialMission getMissionSync(String allianceId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        SpecialMission mission = null;
        Cursor cursor = db.query(SQLiteHelper.TABLE_SPECIAL_MISSIONS, null,
                SQLiteHelper.COLUMN_ALLIANCE_ID_FK + "=? AND " + SQLiteHelper.COLUMN_IS_ACTIVE + "=1",
                new String[]{allianceId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int maxHP = cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_MAX_BOSS_HP));
            mission = new SpecialMission(maxHP);
            mission.setMissionId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_MISSION_ID)));
            mission.setAllianceId(allianceId);
            mission.setBossHP(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_BOSS_HP)));
            mission.setAllianceProgress(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_ALLIANCE_PROGRESS)));

            String userProgressJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_USER_PROGRESS_JSON));
            Type userProgressType = new TypeToken<Map<String, Integer>>(){}.getType();
            mission.setUserTaskProgress(gson.fromJson(userProgressJson, userProgressType));

            String tasksJson = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TASKS_JSON));
            Type tasksType = new TypeToken<List<MissionTask>>(){}.getType();
            mission.setTasks(gson.fromJson(tasksJson, tasksType));

            mission.setStartTime(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_START_TIME)));
            mission.setDurationMillis(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_DURATION)));
            mission.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_IS_ACTIVE)) == 1);
            cursor.close();
        }
        return mission;
    }

    public void deleteMission(String missionId, UserRepository.RequestCallback callback) {
        new Thread(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.delete(SQLiteHelper.TABLE_SPECIAL_MISSIONS, SQLiteHelper.COLUMN_MISSION_ID + "=?", new String[]{missionId});
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        }).start();
    }

    public LiveData<List<SpecialMission>> getActiveMissionsForUser(String userId) {
        MutableLiveData<List<SpecialMission>> liveData = new MutableLiveData<>();
        new Thread(() -> {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.query(SQLiteHelper.TABLE_SPECIAL_MISSIONS, null, SQLiteHelper.COLUMN_IS_ACTIVE + "=1", null, null, null, null);
            List<SpecialMission> missions = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SpecialMission mission = getMissionSync(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_ALLIANCE_ID_FK)));
                    if (mission != null && mission.getUserTaskProgress().containsKey(userId)) {
                        missions.add(mission);
                    }
                }
                cursor.close();
            }
            mainHandler.post(() -> liveData.setValue(missions));
        }).start();
        return liveData;
    }

    // --------------------- FIRESTORE METODE ---------------------
    public LiveData<Boolean> hasActiveMission(String allianceId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        FirebaseFirestore.getInstance()
                .collection("alliances")
                .document(allianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean started = doc.getBoolean("missionStarted");
                        result.setValue(started != null && started);
                    } else {
                        result.setValue(false);
                    }
                })
                .addOnFailureListener(e -> result.setValue(false));

        return result;
    }

    public void startMission(Alliance alliance) {
        String allianceId = alliance.getAllianceId();
        SpecialMission mission = new SpecialMission(allianceId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // upiši misiju
        db.collection("special_missions")
                .document(allianceId)
                .set(mission)
                .addOnSuccessListener(aVoid -> {
                    // obeleži da je savez zauzet
                    db.collection("alliances")
                            .document(allianceId)
                            .update("missionStarted", true);
                });
    }

    public void forceEndMission(String allianceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("special_missions")
                .document(allianceId)
                .update("isActive", false);

        db.collection("alliances")
                .document(allianceId)
                .update("missionStarted", false);
    }

    public interface MissionCallback {
        void onMissionLoaded(SpecialMission mission);
        void onError(Exception e);
    }

    public void getActiveMissionForUser(String userId, MissionCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Pronađi saveze gde je user član
        db.collection("alliances")
                .whereArrayContains("memberIds", userId)
                .get()
                .addOnCompleteListener(allianceTask -> {
                    if (!allianceTask.isSuccessful() || allianceTask.getResult().isEmpty()) {
                        callback.onMissionLoaded(null); // Nema saveza za korisnika
                        return;
                    }

                    // Pretpostavljamo da je korisnik u samo jednom savezu ili uzimamo prvi
                    String allianceId = allianceTask.getResult().getDocuments().get(0).getId();

                    // 2. Pronađi aktivnu misiju za taj savez
                    db.collection("specialMissions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnCompleteListener(missionTask -> {
                                if (missionTask.isSuccessful() && !missionTask.getResult().isEmpty()) {
                                    SpecialMission mission = missionTask.getResult().getDocuments().get(0)
                                            .toObject(SpecialMission.class);
                                    callback.onMissionLoaded(mission);
                                } else {
                                    callback.onMissionLoaded(null);
                                }
                            })
                            .addOnFailureListener(e -> callback.onError(e));
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

}
