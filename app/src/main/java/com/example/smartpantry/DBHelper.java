package com.example.smartpantry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    public static final String TABLE_PREFERENCES = "preferences";
    public static final String COLUMN_PREFERENCE_PRODUCT_ID = "_prefID";
    public static final String COLUMN_PREFERENCE_PRODUCT_RATING = "productPreferenceVote";

    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PRODUCT_ID = "_id";
    public static final String COLUMN_PRODUCT_BARCODE = "barcode";
    public static final String COLUMN_PRODUCT_NAME = "productName";
    public static final String COLUMN_PRODUCT_DESCRIPTION = "productDescription";
    public static final String COLUMN_PRODUCT_EXPIRE_DATE = "expireDate";
    public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
    public static final String COLUMN_IS_FAVORITE = "favorite";

    private static final String DATABASE_NAME = "products.db";
    private static final int DATABASE_VERSION = 5;

    // Products Database creation sql statement
    private static final String PRODUCTS_DATABASE_CREATE = "create table "
            + TABLE_PRODUCTS + "( "
            + COLUMN_PRODUCT_ID + " integer primary key autoincrement, "
            + COLUMN_PRODUCT_BARCODE + " text not null, "
            + COLUMN_PRODUCT_NAME	+ " text not null, "
            + COLUMN_PRODUCT_DESCRIPTION + " text not null, "
            + COLUMN_PRODUCT_QUANTITY + " integer not null, "
            + COLUMN_PRODUCT_EXPIRE_DATE + " text, "
            + COLUMN_IS_FAVORITE + " integer not null default 0);";

    // Ratings Database creation sql statement
    private static final String  PREFERENCES_DATABASE_CREATE = "create table "
            + TABLE_PREFERENCES + "( "
            + COLUMN_PREFERENCE_PRODUCT_ID + " text primary key, "
            + COLUMN_PREFERENCE_PRODUCT_RATING + " integer not null);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(PREFERENCES_DATABASE_CREATE);
        database.execSQL(PRODUCTS_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
        onCreate(db);
    }

    public long insertNewPreference(String id, int rating) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PREFERENCE_PRODUCT_ID, id);
        cv.put(COLUMN_PREFERENCE_PRODUCT_RATING, rating);

        return getWritableDatabase().insert(TABLE_PREFERENCES, null, cv);
    }
    public Integer isAlreadyRated(String id) {
        Cursor cursor = getWritableDatabase().query(TABLE_PREFERENCES, null,
                COLUMN_PREFERENCE_PRODUCT_ID + "='" + id + "'", null,
                null, null, null);
        if(cursor.getCount() > 0){
            return cursor.getInt( cursor.getColumnIndex(COLUMN_PREFERENCE_PRODUCT_RATING));
        } else {
            cursor.close();
            return null;
        }
    }
    public long insertNewProduct(String barcode, String name, String description, String expire, String quantity) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_BARCODE, barcode);
        cv.put(COLUMN_PRODUCT_NAME, name);
        cv.put(COLUMN_PRODUCT_DESCRIPTION, description);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, expire);
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);

        return getWritableDatabase().insert(TABLE_PRODUCTS, null, cv);
    }

    public Cursor getProducts() {
        return getWritableDatabase().query(TABLE_PRODUCTS, null, null,
                null, null, null, COLUMN_IS_FAVORITE + " ASC");
    }
    public void setFavorite(boolean state, int id){
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_FAVORITE, state ? 1 : 0);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, "_id=?",
                new String[] {Integer.toString(id)});
    }

    public void deleteProduct(int id) {
        getWritableDatabase().delete(TABLE_PRODUCTS, COLUMN_PRODUCT_ID + "_id=?",
                new String[] { String.valueOf(id) });
    }
    public Cursor getFavorites() {
        return getWritableDatabase().query(TABLE_PRODUCTS, null,
                COLUMN_IS_FAVORITE + "= 1", null,
                null, null, null);
    }
}
