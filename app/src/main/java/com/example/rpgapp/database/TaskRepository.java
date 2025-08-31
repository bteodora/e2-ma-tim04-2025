package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.model.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    private static final String TAG = "TaskRepository";
    private static volatile TaskRepository INSTANCE;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SQLiteHelper dbHelper;

    private MutableLiveData<List<Task>> allTasksLiveData = new MutableLiveData<>();
    private Context context;

    private TaskRepository(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = new SQLiteHelper(this.context);
    }

    public static TaskRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TaskRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TaskRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // --- LISTA SVIH TASKOVA ---
    public LiveData<List<Task>> getAllTasksLiveData() {
        loadTasksFromSQLite();
        return allTasksLiveData;
    }

    // --- JEDAN TASK PO ID ---
    public LiveData<Task> getTaskById(String taskId) {
        MutableLiveData<Task> taskLiveData = new MutableLiveData<>();
        if (allTasksLiveData.getValue() != null) {
            for (Task task : allTasksLiveData.getValue()) {
                if (taskId.equals(task.getTaskId())) {
                    taskLiveData.postValue(task);
                    break;
                }
            }
        }
        return taskLiveData;
    }

    // --- UPDATE TASKA ---
    public void updateTask(Task task) {
        if (task.getTaskId() == null) return;

        // Firestore
        DocumentReference docRef = db.collection("tasks").document(task.getTaskId());
        docRef.set(task);

        // SQLite
        cacheTaskToSQLite(task);
        loadTasksFromSQLite();
    }

    // --- DELETE TASKA ---
    public void deleteTask(Task task) {
        if (task.getTaskId() == null) return;

        // Firestore
        db.collection("tasks").document(task.getTaskId()).delete();

        // SQLite
        deleteTaskFromSQLite(task.getTaskId());
        loadTasksFromSQLite();
    }

    // --- POMOĆNE METODE ---
    private void cacheTaskToSQLite(Task task) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.COLUMN_TASK_ID, task.getTaskId());
            values.put(SQLiteHelper.COLUMN_TITLE_TASK, task.getTitle());
            values.put(SQLiteHelper.COLUMN_DESCRIPTION_TASK, task.getDescription());
            values.put(SQLiteHelper.COLUMN_STATUS, task.getStatus());

            // Dodatna polja
            values.put(SQLiteHelper.COLUMN_CATEGORY, task.getCategory());
            values.put(SQLiteHelper.COLUMN_COLOR, task.getColor());
            values.put(SQLiteHelper.COLUMN_FREQUENCY, task.getFrequency());
            values.put(SQLiteHelper.COLUMN_INTERVAL, task.getInterval());
            values.put(SQLiteHelper.COLUMN_INTERVAL_UNIT, task.getIntervalUnit());
            values.put(SQLiteHelper.COLUMN_DIFFICULTY_XP, task.getDifficultyXp());
            values.put(SQLiteHelper.COLUMN_IMPORTANCE_XP, task.getImportanceXp());
            values.put(SQLiteHelper.COLUMN_TOTAL_XP, task.getTotalXp());
            values.put(SQLiteHelper.COLUMN_DUE_DATE, task.getDueDate());

            database.insertWithOnConflict(SQLiteHelper.TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            if (database != null) database.close();
        }
    }

    private void deleteTaskFromSQLite(String taskId) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            database.delete(SQLiteHelper.TABLE_TASKS, SQLiteHelper.COLUMN_TASK_ID + " = ?", new String[]{taskId});

        } finally {
            if (database != null) database.close();
        }
    }

    private void loadTasksFromSQLite() {
        SQLiteDatabase database = null;
        List<Task> tasks = new ArrayList<>();
        try {
            database = dbHelper.getReadableDatabase();
            Cursor cursor = database.query(SQLiteHelper.TABLE_TASKS, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Task task = mapCursorToTask(cursor);
                    tasks.add(task);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } finally {
            if (database != null) database.close();
        }
        allTasksLiveData.postValue(tasks);
    }

    private Task mapCursorToTask(Cursor cursor) {
        Task task = new Task();
        task.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TASK_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TITLE_TASK)));
        task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_DESCRIPTION_TASK)));
        task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_STATUS)));

        // Dodatna polja
        task.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY)));
        task.setColor(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_COLOR)));
        task.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_FREQUENCY)));
        task.setInterval(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_INTERVAL)));
        task.setIntervalUnit(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_INTERVAL_UNIT)));
        task.setDifficultyXp(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_DIFFICULTY_XP)));
        task.setImportanceXp(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_IMPORTANCE_XP)));
        task.setTotalXp(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TOTAL_XP)));
        task.setDueDate(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_DUE_DATE)));

        return task;
    }

}
