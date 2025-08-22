package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProductRepository {

    private static final String TAG = "ProductRepository";
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private FirebaseFirestore dbFirebase;

    public ProductRepository(Context context) {
        dbHelper = new SQLiteHelper(context);
        dbFirebase = FirebaseFirestore.getInstance();
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // Vratili smo stara 3 parametra: title, description, image
    public long insertData(String title, String description, String image) {

        // --- DEO ZA FIREBASE ---
        // Kreiramo "Map" objekat koji Firebase razume, jer više nemamo "Product" objekat.
        Map<String, Object> productForFirebase = new HashMap<>();
        productForFirebase.put("title", title);
        productForFirebase.put("description", description);
        productForFirebase.put("image", image);

        dbFirebase.collection("products")
                .add(productForFirebase)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Proizvod uspešno sačuvan u Firebase sa ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Greška pri čuvanju proizvoda u Firebase", e);
                });

        // --- DEO ZA SQLITE (Originalni kod) ---
        Log.i("REZ_DB", "insertData to database");
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_TITLE, title);
        values.put(SQLiteHelper.COLUMN_DESCRIPTION, description);
        values.put(SQLiteHelper.COLUMN_IMAGE, image);

        // Vraćamo long vrednost kao što je i originalna metoda radila
        return database.insert(SQLiteHelper.TABLE_PRODUCTS, null, values);
    }

    // Get metoda ostaje ista
    public Cursor getData(Long id, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i("REZ_DB", "getData from database - queryBuilder");
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        if(id != null){
            queryBuilder.appendWhere(SQLiteHelper.COLUMN_ID + "="  + id);
        }
        queryBuilder.setTables(SQLiteHelper.TABLE_PRODUCTS);
        return queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
    }

    // Update metoda ostaje ista
    public int updateData(long id, String title, String description, String image) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_TITLE, title);
        values.put(SQLiteHelper.COLUMN_DESCRIPTION, description);
        values.put(SQLiteHelper.COLUMN_IMAGE, image);
        String whereClause = SQLiteHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        return database.update(SQLiteHelper.TABLE_PRODUCTS, values, whereClause, whereArgs);
    }

    // Delete metoda ostaje ista
    public int deleteData(long id) {
        String whereClause = SQLiteHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        return database.delete(SQLiteHelper.TABLE_PRODUCTS, whereClause, whereArgs);
    }
}