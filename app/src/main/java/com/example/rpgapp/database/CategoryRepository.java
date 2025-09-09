package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    private static volatile CategoryRepository INSTANCE;
    private SQLiteHelper dbHelper;
    private Context context;

    private MutableLiveData<List<Category>> allCategoriesLiveData = new MutableLiveData<>();

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

    // --- Dodaj novu kategoriju ---
    public void insertCategory(Category category) {
        if (category == null) return;

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.COLUMN_CATEGORY_NAME, category.getName());
            values.put(SQLiteHelper.COLUMN_CATEGORY_COLOR, category.getColor());

            db.insertWithOnConflict(SQLiteHelper.TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        } finally {
            if (db != null) db.close();
        }

        loadCategoriesFromSQLite();
    }

    // --- Ažuriraj kategoriju ---
    public void updateCategory(Category category) {
        if (category == null) return;

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.COLUMN_CATEGORY_NAME, category.getName());
            values.put(SQLiteHelper.COLUMN_CATEGORY_COLOR, category.getColor());

            db.update(SQLiteHelper.TABLE_CATEGORIES, values,
                    SQLiteHelper.COLUMN_CATEGORY_ID + "=?",
                    new String[]{String.valueOf(category.getId())});

        } finally {
            if (db != null) db.close();
        }

        loadCategoriesFromSQLite();
    }

    // --- Obriši kategoriju ---
    public void deleteCategory(int id) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.delete(SQLiteHelper.TABLE_CATEGORIES,
                    SQLiteHelper.COLUMN_CATEGORY_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            if (db != null) db.close();
        }

        loadCategoriesFromSQLite();
    }

    // --- LiveData za sve kategorije ---
    public LiveData<List<Category>> getAllCategories() {
        loadCategoriesFromSQLite();
        return allCategoriesLiveData;
    }

    // --- Pomoćna metoda za učitavanje iz SQLite ---
    private void loadCategoriesFromSQLite() {
        SQLiteDatabase db = null;
        List<Category> categories = new ArrayList<>();
        try {
            db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(SQLiteHelper.TABLE_CATEGORIES, null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category c = new Category();
                    c.setId(cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_ID)));
                    c.setName(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_NAME)));
                    c.setColor(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteHelper.COLUMN_CATEGORY_COLOR)));
                    categories.add(c);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } finally {
            if (db != null) db.close();
        }

        allCategoriesLiveData.postValue(categories);
    }

    // --- Jedna kategorija po ID ---
    public LiveData<Category> getCategoryById(int id) {
        MutableLiveData<Category> categoryLiveData = new MutableLiveData<>();
        List<Category> currentCategories = allCategoriesLiveData.getValue();
        if (currentCategories != null) {
            for (Category c : currentCategories) {
                if (c.getId() == id) {
                    categoryLiveData.postValue(c);
                    break;
                }
            }
        }
        return categoryLiveData;
    }
}
