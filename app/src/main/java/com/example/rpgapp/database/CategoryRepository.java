package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.model.Category;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
// <<< IZMENA: Importovani Executor-i
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private static volatile CategoryRepository INSTANCE;
    private final SQLiteHelper dbHelper;
    private final Context context;

    private final MutableLiveData<List<Category>> allCategoriesLiveData = new MutableLiveData<>();

    // <<< IZMENA: Dodat je ExecutorService za pozadinske operacije sa bazom
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private CategoryRepository(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = new SQLiteHelper(this.context);
    }

    public static CategoryRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CategoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CategoryRepository(context);
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

    public void insertCategory(Category category) {
        if (category == null) return;
        // <<< IZMENA: Operacija se izvršava na pozadinskom thread-u
        databaseExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(SQLiteHelper.COLUMN_CATEGORY_NAME, category.getName());
                values.put(SQLiteHelper.COLUMN_CATEGORY_COLOR, category.getColor());
                values.put(SQLiteHelper.COLUMN_CATEGORY_USER_ID, getCurrentUserId());
                db.insertWithOnConflict(SQLiteHelper.TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            } finally {
                if (db != null) db.close();
            }
            // Odmah nakon upisa, pokreni ponovno učitavanje (koje će takođe biti u pozadini)
            loadCategoriesFromSQLite();
        });
    }

    public void updateCategory(Category category) {
        if (category == null) return;
        databaseExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(SQLiteHelper.COLUMN_CATEGORY_NAME, category.getName());
                values.put(SQLiteHelper.COLUMN_CATEGORY_COLOR, category.getColor());
                values.put(SQLiteHelper.COLUMN_CATEGORY_USER_ID, getCurrentUserId());
                db.update(SQLiteHelper.TABLE_CATEGORIES, values,
                        SQLiteHelper.COLUMN_CATEGORY_ID + "=?",
                        new String[]{String.valueOf(category.getId())});
            } finally {
                if (db != null) db.close();
            }
            loadCategoriesFromSQLite();
        });
    }

    public void deleteCategory(int id) {
        databaseExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                db.delete(SQLiteHelper.TABLE_CATEGORIES,
                        SQLiteHelper.COLUMN_CATEGORY_ID + "=? AND " + SQLiteHelper.COLUMN_CATEGORY_USER_ID + "=?",
                        new String[]{String.valueOf(id), getCurrentUserId()});
            } finally {
                if (db != null) db.close();
            }
            loadCategoriesFromSQLite();
        });
    }

    public LiveData<List<Category>> getAllCategories() {
        loadCategoriesFromSQLite();
        return allCategoriesLiveData;
    }

    public LiveData<Category> getCategoryById(int id) {
        // Ova metoda ne pristupa bazi, može ostati ovakva
        MutableLiveData<Category> categoryLiveData = new MutableLiveData<>();
        List<Category> currentCategories = allCategoriesLiveData.getValue();
        if (currentCategories != null) {
            for (Category c : currentCategories) {
                if (c.getId() == id && c.getUserId().equals(getCurrentUserId())) {
                    categoryLiveData.postValue(c);
                    break;
                }
            }
        }
        return categoryLiveData;
    }

    public void addCategory(Category category) {
        if (category == null) return;
        databaseExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(SQLiteHelper.COLUMN_CATEGORY_NAME, category.getName());
                values.put(SQLiteHelper.COLUMN_CATEGORY_COLOR, category.getColor());
                values.put(SQLiteHelper.COLUMN_CATEGORY_USER_ID, getCurrentUserId());
                db.insert(SQLiteHelper.TABLE_CATEGORIES, null, values);
            } finally {
                if (db != null) db.close();
            }
            loadCategoriesFromSQLite();
        });
    }

    private void loadCategoriesFromSQLite() {
        loadCategoriesFromSQLite(getCurrentUserId());
    }

    private void loadCategoriesFromSQLite(String userId) {
        // <<< IZMENA: Ceo rad sa bazom se prebacuje na pozadinski thread
        databaseExecutor.execute(() -> {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            List<Category> categories = new ArrayList<>();
            try {
                db = dbHelper.getReadableDatabase();
                String selection = null;
                String[] selectionArgs = null;

                if (userId != null) {
                    selection = SQLiteHelper.COLUMN_CATEGORY_USER_ID + "=?";
                    selectionArgs = new String[]{userId};
                }

                cursor = db.query(SQLiteHelper.TABLE_CATEGORIES, null, selection, selectionArgs, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Category c = new Category();
                        c.setId(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_ID)));
                        c.setName(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_NAME)));
                        c.setColor(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_COLOR)));
                        c.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_USER_ID)));
                        categories.add(c);
                    } while (cursor.moveToNext());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }
            // <<< IZMENA: postValue() se koristi za slanje rezultata nazad na glavni thread
            allCategoriesLiveData.postValue(categories);
        });
    }
}