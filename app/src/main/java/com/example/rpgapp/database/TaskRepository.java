package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.model.Task;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {

    private static final String TAG = "TaskRepository";
    private static volatile TaskRepository INSTANCE;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SQLiteHelper dbHelper;
    private final Context context;

    // LiveData that holds the PROCESSED list for UI display
    private final MutableLiveData<List<Task>> processedTasksLiveData = new MutableLiveData<>();
    // LiveData that holds the RAW, unprocessed list for internal use
    private final MutableLiveData<List<Task>> allTasksLiveData = new MutableLiveData<>();

    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

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

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    // --- NEW MAIN METHODS ---
    // This method is called by TaskListFragment to display the OPTIMIZED list
    public LiveData<List<Task>> getProcessedTasksLiveData() {
        loadAndProcessTasks();
        return processedTasksLiveData;
    }

    // --- OLD METHODS (now using background thread) ---
    // This can still be used by other parts of the app that need the raw list
    public LiveData<List<Task>> getAllTasksLiveData() {
        loadTasksFromSQLite(); // Loads raw list into allTasksLiveData
        loadTasksFromFirestore(); // Syncs with Firestore
        return allTasksLiveData;
    }

    public boolean areAllTasksCompletedDuringMission() {
        List<Task> tasks = allTasksLiveData.getValue();
        if (tasks == null || tasks.isEmpty()) return false;

        for (Task task : tasks) {
            if (!"urađen".equalsIgnoreCase(task.getStatus())) {
                return false;
            }
        }
        return true;
    }

    // --- MAIN PROCESSING LOGIC (EXECUTED IN BACKGROUND) ---
    private void loadAndProcessTasks() {
        databaseExecutor.execute(() -> {
            List<Task> allTasks = loadTasksFromSQLiteInternal();
            Map<String, String> categoryColorMap = loadCategoryColorsInternal();

            List<Task> finalTaskList = new ArrayList<>();
            long threeDaysMillis = 3L * 24 * 60 * 60 * 1000;

            for (Task task : allTasks) {
                try {
                    boolean isPast = false;
                    if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                        long dueMillis = dateFormat.parse(task.getDueDate()).getTime();
                        if (System.currentTimeMillis() > dueMillis + threeDaysMillis) isPast = true;
                    }
                    if (!isPast && task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                        long endMillis = dateFormat.parse(task.getEndDate()).getTime();
                        if (System.currentTimeMillis() > endMillis + threeDaysMillis) isPast = true;
                    }
                    if (isPast) continue;

                    if (task.getCategory() != null) {
                        String color = categoryColorMap.get(task.getCategory().toLowerCase());
                        task.setColor(color != null ? color : "#9E9E9E");
                    }
                    finalTaskList.add(task);
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date for task: " + task.getTitle(), e);
                }
            }
            processedTasksLiveData.postValue(finalTaskList);
        });
    }

    // --- ORIGINAL METHODS, NOW ADAPTED ---

    public void loadTasksFromFirestore() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        db.collection("tasks").whereEqualTo("userId", currentUserId).get()
                .addOnSuccessListener(querySnapshot -> databaseExecutor.execute(() -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setTaskId(doc.getId());
                            // Immediately write to SQLite
                            cacheSingleTaskToSQLite(task);
                        }
                    }
                    // After sync, refresh both lists from the local database
                    loadAndProcessTasks(); // Refreshes the processed list for UI
                    allTasksLiveData.postValue(loadTasksFromSQLiteInternal()); // Refreshes the raw list
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Error loading tasks from Firestore", e));
    }

    public LiveData<List<Task>> getAllTasksForUser(String userId) {
        MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();
        if (userId == null) {
            tasksLiveData.setValue(new ArrayList<>());
            return tasksLiveData;
        }
        db.collection("tasks").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setTaskId(doc.getId());
                            tasks.add(task);
                        }
                    }
                    tasksLiveData.postValue(tasks);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading tasks from Firestore for statistics", e));
        return tasksLiveData;
    }

    public LiveData<Task> getTaskById(String taskId) {
        MutableLiveData<Task> taskLiveData = new MutableLiveData<>();
        databaseExecutor.execute(() -> {
            // Search local database for the most recent data
            List<Task> tasks = loadTasksFromSQLiteInternal();
            for (Task task : tasks) {
                if (taskId.equals(task.getTaskId())) {
                    taskLiveData.postValue(task);
                    return;
                }
            }
        });
        return taskLiveData;
    }

    public void addTask(Task task) {
        if (task.getTaskId() == null) task.setTaskId(UUID.randomUUID().toString());
        long currentTime = System.currentTimeMillis();
        task.setCreationTimestamp(currentTime);
        task.setLastActionTimestamp(currentTime);

        db.collection("tasks").document(task.getTaskId()).set(task);

        databaseExecutor.execute(() -> {
            cacheSingleTaskToSQLite(task);
            loadAndProcessTasks();
            allTasksLiveData.postValue(loadTasksFromSQLiteInternal());
        });
    }

    public void updateTask(Task task) {
        if (task.getTaskId() == null) return;
        db.collection("tasks").document(task.getTaskId()).set(task);

        databaseExecutor.execute(() -> {
            cacheSingleTaskToSQLite(task);
            loadAndProcessTasks();
            allTasksLiveData.postValue(loadTasksFromSQLiteInternal());
        });
    }

    public void deleteTask(Task task) {
        if (task.getTaskId() == null) return;
        db.collection("tasks").document(task.getTaskId()).delete();

        databaseExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                db.delete(SQLiteHelper.TABLE_TASKS, SQLiteHelper.COLUMN_TASK_ID + " = ?", new String[]{task.getTaskId()});
            } finally {
                if (db != null) db.close();
            }
            loadAndProcessTasks();
            allTasksLiveData.postValue(loadTasksFromSQLiteInternal());
        });
    }

    public void deleteFutureRecurringTasks(Task task) {
        if (!task.isRecurring()) return;
        databaseExecutor.execute(() -> {
            List<Task> currentTasks = loadTasksFromSQLiteInternal();
            long now = System.currentTimeMillis();

            for (Task t : currentTasks) {
                if (t.getRecurringId() != null && t.getRecurringId().equals(task.getRecurringId())) {
                    try {
                        long taskTime = t.getDueDate() != null ? dateFormat.parse(t.getDueDate()).getTime() : dateFormat.parse(t.getStartDate()).getTime();
                        if (taskTime >= now) {
                            db.collection("tasks").document(t.getTaskId()).delete();
                            SQLiteDatabase sqlDb = null;
                            try {
                                sqlDb = dbHelper.getWritableDatabase();
                                sqlDb.delete(SQLiteHelper.TABLE_TASKS, SQLiteHelper.COLUMN_TASK_ID + " = ?", new String[]{t.getTaskId()});
                            } finally {
                                if (sqlDb != null) sqlDb.close();
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            loadAndProcessTasks();
            allTasksLiveData.postValue(loadTasksFromSQLiteInternal());
        });
    }

    public void updateFutureRecurringTasks(Task task) {
        if (!task.isRecurring()) return;
        databaseExecutor.execute(() -> {
            List<Task> currentTasks = loadTasksFromSQLiteInternal();
            long now = System.currentTimeMillis();

            for (Task t : currentTasks) {
                if (t.getRecurringId() != null && t.getRecurringId().equals(task.getRecurringId())) {
                    try {
                        long taskTime = t.getDueDate() != null ? dateFormat.parse(t.getDueDate()).getTime() : dateFormat.parse(t.getStartDate()).getTime();
                        if (taskTime >= now) {
                            t.setTitle(task.getTitle());
                            t.setDescription(task.getDescription());
                            t.setStatus(task.getStatus());
                            t.setCategory(task.getCategory());
                            t.setLastActionTimestamp(System.currentTimeMillis());
                            cacheSingleTaskToSQLite(t);
                            db.collection("tasks").document(t.getTaskId()).set(t);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            loadAndProcessTasks();
            allTasksLiveData.postValue(loadTasksFromSQLiteInternal());
        });
    }

    public void updateTaskStatus(Task task, String selectedStatus, Context context) {
        if (task == null) return;
        databaseExecutor.execute(() -> {
            String currentStatus = task.getStatus().toLowerCase();
            String selectedStatusLower = selectedStatus.toLowerCase();

            if ("urađen".equals(currentStatus) || "neurađen".equals(currentStatus) || "otkazan".equals(currentStatus) || isTaskPast(task)) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "This task cannot be changed", Toast.LENGTH_SHORT).show());
                return;
            }

            switch (currentStatus) {
                case "aktivan":
                    if (!("urađen".equals(selectedStatusLower) || "otkazan".equals(selectedStatusLower) || ("pauziran".equals(selectedStatusLower) && task.isRecurring()) || "aktivan".equals(selectedStatusLower))) {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Invalid status change", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    break;
                case "pauziran":
                    if (!("aktivan".equals(selectedStatusLower) || "pauziran".equals(selectedStatusLower))) {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Invalid status change", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    break;
            }
            task.setStatus(selectedStatusLower);
            task.setLastActionTimestamp(System.currentTimeMillis());

            if ("urađen".equals(selectedStatusLower)) {
                UserRepository userRepo = UserRepository.getInstance(context);
                if (userRepo != null) {
                    User currentUser = userRepo.getLoggedInUser();
                    if (currentUser != null) {
                        List<Task> allUserTasks = loadTasksFromSQLiteInternal();
                        int finalXpGained = currentUser.getFinalXpForTask(task);
                        task.setTotalXp(finalXpGained);
                        boolean xpWasAdded = currentUser.increaseXp(task, allUserTasks);
                        userRepo.updateUser(currentUser);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (xpWasAdded) {
                                Toast.makeText(context, "You earned " + finalXpGained + " XP!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "XP quota exceeded", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                // Special mission – automatically reduce boss HP
                String userId = getCurrentUserId();
                if (userId != null && context instanceof ViewModelStoreOwner) {
                     new Handler(Looper.getMainLooper()).post(() -> {
                        SpecialMissionViewModel vm = new ViewModelProvider((ViewModelStoreOwner) context).get(SpecialMissionViewModel.class);
                        vm.handleNormalTaskCompletion(task, userId);
                    });
                }
            }

            updateTask(task); // Calls the method that already works asynchronously and refreshes lists
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Task status updated", Toast.LENGTH_SHORT).show());
        });
    }

    public List<String> getAllCategories() {
        Log.w(TAG, "Inefficient method getAllCategories() was called. Should be avoided.");
        // This method would require synchronous database access, blocking the UI.
        // The logic has been moved to loadAndProcessTasks instead.
        return new ArrayList<>();
    }

    private void loadTasksFromSQLite() {
        databaseExecutor.execute(() -> {
            allTasksLiveData.postValue(loadTasksFromSQLiteInternal());
        });
    }

    // --- INTERNAL (HELPER) METHODS ---

    private List<Task> loadTasksFromSQLiteInternal() {
        List<Task> tasks = new ArrayList<>();
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return tasks;
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = dbHelper.getReadableDatabase();
            String selection = SQLiteHelper.COLUMN_TASK_USER_ID + "=?";
            String[] selectionArgs = new String[]{currentUserId};
            cursor = database.query(SQLiteHelper.TABLE_TASKS, null, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do { tasks.add(mapCursorToTask(cursor)); } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            if (database != null) database.close();
        }
        return tasks;
    }

    private Map<String, String> loadCategoryColorsInternal() {
        Map<String, String> categoryMap = new HashMap<>();
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return categoryMap;
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = dbHelper.getReadableDatabase();
            String selection = SQLiteHelper.COLUMN_CATEGORY_USER_ID + "=?";
            String[] selectionArgs = new String[]{currentUserId};
            cursor = database.query(SQLiteHelper.TABLE_CATEGORIES, new String[]{SQLiteHelper.COLUMN_CATEGORY_NAME, SQLiteHelper.COLUMN_CATEGORY_COLOR}, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_NAME));
                    String color = cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_COLOR));
                    if (name != null) categoryMap.put(name.toLowerCase(), color);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            if (database != null) database.close();
        }
        return categoryMap;
    }

    private void cacheSingleTaskToSQLite(Task task) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.COLUMN_TASK_ID, task.getTaskId());
            values.put(SQLiteHelper.COLUMN_TITLE_TASK, task.getTitle());
            values.put(SQLiteHelper.COLUMN_DESCRIPTION_TASK, task.getDescription());
            values.put(SQLiteHelper.COLUMN_STATUS, task.getStatus());
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
            values.put(SQLiteHelper.COLUMN_DIFFICULTY_TEXT, task.getDifficultyText());
            values.put(SQLiteHelper.COLUMN_IMPORTANCE_TEXT, task.getImportanceText());
            values.put(SQLiteHelper.COLUMN_RECURRING, task.isRecurring() ? 1 : 0);
            values.put(SQLiteHelper.COLUMN_RECURRING_ID, task.getRecurringId());
            values.put(SQLiteHelper.COLUMN_TASK_USER_ID, getCurrentUserId());
            values.put(SQLiteHelper.COLUMN_CREATION_TIMESTAMP, task.getCreationTimestamp());
            values.put(SQLiteHelper.COLUMN_LAST_ACTION_TIMESTAMP, task.getLastActionTimestamp());
            database.insertWithOnConflict(SQLiteHelper.TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            if (database != null) database.close();
        }
    }

    private Task mapCursorToTask(Cursor cursor) {
        Task task = new Task();
        task.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TASK_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_TITLE_TASK)));
        task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_DESCRIPTION_TASK)));
        task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_STATUS)));
        task.setDifficultyText(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_DIFFICULTY_TEXT)));
        task.setImportanceText(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_IMPORTANCE_TEXT)));
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
        task.setCreationTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CREATION_TIMESTAMP)));
        task.setLastActionTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_LAST_ACTION_TIMESTAMP)));
        return task;
    }

    private boolean isTaskPast(Task task) {
        try {
            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                // Consider the task "past" only after the end of its due day
                return dateFormat.parse(task.getDueDate()).getTime() < (System.currentTimeMillis() - 24*60*60*1000);
            }
            if (task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                return dateFormat.parse(task.getEndDate()).getTime() < (System.currentTimeMillis() - 24*60*60*1000);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}