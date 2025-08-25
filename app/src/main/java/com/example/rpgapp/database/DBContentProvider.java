package com.example.rpgapp.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;


public class DBContentProvider extends ContentProvider {
    private SQLiteHelper database;
    private static final int PRODUCTS = 10;
    private static final int PRODUCT_ID = 20;
    private static final String AUTHORITY = "com.example.shopapp";
    private static final String PRODUCT_PATH = "products";
    public static final Uri CONTENT_URI_PRODUCTS = Uri.parse("content://" + AUTHORITY + "/" + PRODUCT_PATH);
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    static {
        sURIMatcher.addURI(AUTHORITY, PRODUCT_PATH, PRODUCTS);
        sURIMatcher.addURI(AUTHORITY, PRODUCT_PATH + "/#", PRODUCT_ID);
    }

    @Override
    public boolean onCreate() {
        Log.i("REZ_DB", "ON CREATE CONTENT PROVIDER");
        database = new SQLiteHelper(getContext());
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (database != null) {
            database.close();
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i("REZ_DB", "QUERY");
        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exist
        //checkColumns(projection);
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case PRODUCT_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere(SQLiteHelper.COLUMN_ID + "="  + uri.getLastPathSegment());
            case PRODUCTS:
                // Set the table
                queryBuilder.setTables(SQLiteHelper.TABLE_PRODUCTS);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        } else {
            Log.e("REZ_DB", "Cursor or ContentResolver is null.");
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

//    ContentValues - parovi kljuc vrednost.
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.i("REZ_DB", "INSERT");
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case PRODUCTS:
                id = sqlDB.insert(SQLiteHelper.TABLE_PRODUCTS, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        // resolver salje upis provideru
        // provider vraca informacije resolveru
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return Uri.parse(PRODUCT_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.i("REZ_DB", "DELETE");
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case PRODUCTS:
                rowsDeleted = sqlDB.delete(SQLiteHelper.TABLE_PRODUCTS,
                        selection,
                        selectionArgs);
                break;
            case PRODUCT_ID:
                String idPRODUCT = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SQLiteHelper.TABLE_PRODUCTS,
                            SQLiteHelper.COLUMN_ID + "=" + idPRODUCT,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SQLiteHelper.TABLE_PRODUCTS,
                            SQLiteHelper.COLUMN_ID + "=" + idPRODUCT +
                                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.i("REZ_DB", "UPDATE");
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case PRODUCTS:
                rowsUpdated = sqlDB.update(SQLiteHelper.TABLE_PRODUCTS,
                        values,
                        selection,
                        selectionArgs);
                break;
            case PRODUCT_ID:
                String idPRODUCT = uri.getLastPathSegment();
                rowsUpdated = sqlDB.update(SQLiteHelper.TABLE_PRODUCTS, values,
                        SQLiteHelper.COLUMN_ID + "=" + idPRODUCT +
                                (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
