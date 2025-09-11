package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.model.Task;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

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
    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }


    // --- LISTA SVIH TASKOVA ---
    public LiveData<List<Task>> getAllTasksLiveData() {
        loadTasksFromSQLite();
        loadTasksFromFirestore(); //filter po useru
        return allTasksLiveData;
    }

    public void loadTasksFromFirestore() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        db.collection("tasks")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setTaskId(doc.getId()); // osigurava da svaki task ima taskId
                            tasks.add(task);
                        }
                    }

                    allTasksLiveData.postValue(tasks);

                    // Opcionalno: sinhronizuj sa SQLite
                    for (Task t : tasks) {
                        cacheTaskToSQLite(t);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading tasks from Firestore", e));
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
            values.put(SQLiteHelper.COLUMN_START_DATE, task.getStartDate());
            values.put(SQLiteHelper.COLUMN_END_DATE, task.getEndDate());
            // recurring
            values.put(SQLiteHelper.COLUMN_RECURRING, task.isRecurring() ? 1 : 0);
            values.put(SQLiteHelper.COLUMN_RECURRING_ID, task.getRecurringId());
            // user_id
            values.put(SQLiteHelper.COLUMN_TASK_USER_ID, getCurrentUserId());


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
            String selection = null;
            String[] selectionArgs = null;

            String currentUserId = getCurrentUserId();
            if (currentUserId != null) {
                selection = SQLiteHelper.COLUMN_TASK_USER_ID + "=?";
                selectionArgs = new String[]{currentUserId};
            }

            Cursor cursor = database.query(SQLiteHelper.TABLE_TASKS, null, selection, selectionArgs, null, null, null);
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
        task.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_START_DATE)));
        task.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_END_DATE)));
        task.setRecurring(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_RECURRING)) == 1);
        task.setRecurringId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_RECURRING_ID)));
        task.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TASK_USER_ID)));


        return task;
    }
    public void addTask(Task task) {
        if (task.getTaskId() == null) task.setTaskId(UUID.randomUUID().toString());

        // Firestore
        db.collection("tasks").document(task.getTaskId()).set(task);

        // SQLite
        cacheTaskToSQLite(task);

        // Osveži LiveData
        loadTasksFromSQLite();
    }

    public void deleteFutureRecurringTasks(Task task) {
        if (!task.isRecurring()) return;

        long now = System.currentTimeMillis();
        List<Task> currentTasks = allTasksLiveData.getValue();
        if (currentTasks == null) return;

        List<Task> updatedTasks = new ArrayList<>(currentTasks);
        List<Task> tasksToDelete = new ArrayList<>();

        for (Task t : updatedTasks) {
            if (t.getRecurringId() != null && t.getRecurringId().equals(task.getRecurringId())) {
                try {
                    long taskTime = t.getDueDate() != null
                            ? dateFormat.parse(t.getDueDate()).getTime()
                            : dateFormat.parse(t.getStartDate()).getTime();

                    if (taskTime >= now) {
                        tasksToDelete.add(t);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (Task t : tasksToDelete) {
            deleteTaskFromSQLite(t.getTaskId());
            db.collection("tasks").document(t.getTaskId()).delete();
            updatedTasks.remove(t);
        }

        allTasksLiveData.setValue(updatedTasks);
    }


    public void updateFutureRecurringTasks(Task task) {
        if (!task.isRecurring()) return;

        long now = System.currentTimeMillis();
        List<Task> currentTasks = allTasksLiveData.getValue();
        if (currentTasks == null) return;

        List<Task> updatedTasks = new ArrayList<>(currentTasks);

        for (Task t : updatedTasks) {
            if (t.getRecurringId() != null && t.getRecurringId().equals(task.getRecurringId())) {
                try {
                    long taskTime = t.getDueDate() != null
                            ? dateFormat.parse(t.getDueDate()).getTime()
                            : dateFormat.parse(t.getStartDate()).getTime();

                    if (taskTime >= now) {
                        t.setTitle(task.getTitle());
                        t.setDescription(task.getDescription());
                        t.setStatus(task.getStatus());
                        t.setCategory(task.getCategory());
                        cacheTaskToSQLite(t); // sačuvaj izmene u SQLite
                        db.collection("tasks").document(t.getTaskId()).set(t); // Firestore update
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        allTasksLiveData.setValue(updatedTasks);
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        List<Task> tasks = allTasksLiveData.getValue();
        if (tasks != null) {
            for (Task task : tasks) {
                String cat = task.getCategory();
                if (cat != null && !cat.isEmpty() && !categories.contains(cat)) {
                    categories.add(cat);
                }
            }
        }
        return categories;
    }
    // --- UPDATE STATUSA TASKA ---
    public void updateTaskStatus(Task task, String selectedStatus, Context context) {
        if (task == null) return;

        String currentStatus = task.getStatus().toLowerCase();
        String selectedStatusLower = selectedStatus.toLowerCase();

        if ("urađen".equals(currentStatus) || "neurađen".equals(currentStatus) ||
                "otkazan".equals(currentStatus) || isTaskPast(task)) {
            if (context != null) {
                Toast.makeText(context, "Ovaj zadatak se ne može menjati", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Pravila promene statusa
        switch (currentStatus) {
            case "aktivan":
                if ("urađen".equals(selectedStatusLower) || "otkazan".equals(selectedStatusLower) ||
                        ("pauziran".equals(selectedStatusLower) && task.isRecurring()) ||
                        "aktivan".equals(selectedStatusLower)) {
                    task.setStatus(selectedStatusLower);
                } else {
                    if (context != null) {
                        Toast.makeText(context, "Nevažeća promena statusa", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                break;
            case "pauziran":
                if ("aktivan".equals(selectedStatusLower) || "pauziran".equals(selectedStatusLower)) {
                    task.setStatus(selectedStatusLower);
                } else {
                    if (context != null) {
                        Toast.makeText(context, "Nevažeća promena statusa", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                break;
        }

        // Ako je urađen, dodeli XP korisniku
        if ("urađen".equals(selectedStatusLower)) {
            UserRepository userRepo = UserRepository.getInstance(context);
            if (userRepo != null) {
                User currentUser = userRepo.getLoggedInUser();
                if (currentUser != null) {
                    List<Task> todaysTasks = getAllTasksLiveData().getValue(); // svi zadaci korisnika
                    boolean xpAdded = currentUser.increaseXp(task, todaysTasks);
                    userRepo.updateUser(currentUser);

                    if (xpAdded) {
                        Toast.makeText(context, "Osvojili ste " + task.getTotalXp() + " XP!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Kvota za XP je prekoračena", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }


        // Update u Firestore i SQLite
        updateTask(task);

        if (context != null) {
            Toast.makeText(context, "Status zadatka ažuriran", Toast.LENGTH_SHORT).show();
        }
    }

    // --- PRIVATNA POMOĆNA METODA ---
    private boolean isTaskPast(Task task) {
        try {
            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                return dateFormat.parse(task.getDueDate()).getTime() < System.currentTimeMillis();
            }
            if (task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                return dateFormat.parse(task.getEndDate()).getTime() < System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
