package com.example.rpgapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProductRepository {

    private static final String TAG = "ProductRepository";
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private FirebaseFirestore dbFirebase;

    // NOVO: Jednostavan interfejs da znamo kada je sinhronizacija gotova.
    public interface SyncCompleteListener {
        void onSyncComplete();
    }

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

    // Insert metoda - upisuje na oba mesta
    public long insertData(String title, String description, String image) {
        Map<String, Object> productForFirebase = new HashMap<>();
        productForFirebase.put("title", title);
        productForFirebase.put("description", description);
        productForFirebase.put("image", image);

        dbFirebase.collection("products")
                .add(productForFirebase)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Proizvod uspešno sačuvan u Firebase."))
                .addOnFailureListener(e -> Log.w(TAG, "Greška pri čuvanju proizvoda u Firebase", e));

        Log.i("REZ_DB", "insertData to database");
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_TITLE, title);
        values.put(SQLiteHelper.COLUMN_DESCRIPTION, description);
        values.put(SQLiteHelper.COLUMN_IMAGE, image);
        return database.insert(SQLiteHelper.TABLE_PRODUCTS, null, values);
    }

    // Get metoda - VRAĆENA NA STARO! Radi samo sa SQLite bazom.
    // Brza je, jednostavna i vraća Cursor odmah.
    public Cursor getData(Long id, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i("REZ_DB", "getData from LOCAL database - queryBuilder");
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        if(id != null){
            queryBuilder.appendWhere(SQLiteHelper.COLUMN_ID + "="  + id);
        }
        queryBuilder.setTables(SQLiteHelper.TABLE_PRODUCTS);
        return queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
    }

    // --- NOVA METODA SAMO ZA SINHRONIZACIJU ---
    public void syncFirebaseData(SyncCompleteListener listener) {
        Log.d(TAG, "Pokrenuta sinhronizacija sa Firebase-om...");
        dbFirebase.collection("products").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Uspešno preuzeto " + queryDocumentSnapshots.size() + " proizvoda sa Firebase-a.");
                    try {
                        open();
                        database.delete(SQLiteHelper.TABLE_PRODUCTS, null, null);
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            ContentValues values = new ContentValues();
                            values.put(SQLiteHelper.COLUMN_TITLE, document.getString("title"));
                            values.put(SQLiteHelper.COLUMN_DESCRIPTION, document.getString("description"));
                            values.put(SQLiteHelper.COLUMN_IMAGE, document.getString("image"));
                            database.insert(SQLiteHelper.TABLE_PRODUCTS, null, values);
                        }
                        Log.d(TAG, "Lokalna baza je uspešno sinhronizovana.");
                    } finally {
                        close();
                        if (listener != null) {
                            listener.onSyncComplete(); // Javi da je gotovo!
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri sinhronizaciji sa Firebase-om.", e);
                    if (listener != null) {
                        listener.onSyncComplete(); // Javi da je gotovo, čak i ako je neuspešno, da se UI ne bi zaglavio
                    }
                });
    }


    // Update metoda - radi samo sa LOKALNOM bazom za sada
    public int updateData(long id, String title, String description, String image) {
        // TODO: Implementirati update i na Firebase-u
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_TITLE, title);
        values.put(SQLiteHelper.COLUMN_DESCRIPTION, description);
        values.put(SQLiteHelper.COLUMN_IMAGE, image);
        String whereClause = SQLiteHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        return database.update(SQLiteHelper.TABLE_PRODUCTS, values, whereClause, whereArgs);
    }

    // Delete metoda - radi samo sa LOKALNOM bazom za sada
    public int deleteData(long id) {
        // TODO: Implementirati delete i sa Firebase-a
        String whereClause = SQLiteHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        return database.delete(SQLiteHelper.TABLE_PRODUCTS, whereClause, whereArgs);
    }
    // NOVA, JEDNOSTAVNIJA VERZIJA METODE
    // Nju ćemo zvati iz SyncService-a, kada nas ne zanima direktan odgovor
    public void syncFirebaseData() {
        // Pozivamo našu originalnu metodu, ali joj prosleđujemo 'null' kao listener
        syncFirebaseData(null);
    }
}