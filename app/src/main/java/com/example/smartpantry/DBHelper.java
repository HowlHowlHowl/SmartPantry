package com.example.smartpantry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {


    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PRODUCT_ID = "_id";
    public static final String COLUMN_PRODUCT_BARCODE = "barcode";
    public static final String COLUMN_PRODUCT_NAME = "productName";
    public static final String COLUMN_PRODUCT_DESCRIPTION = "productDescription";
    public static final String COLUMN_PRODUCT_EXPIRE_DATE = "expireDate";
    public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
    public static final String COLUMN_IS_FAVORITE = "favorite";

    private static final String DATABASE_NAME = "products.db";
    private static final int DATABASE_VERSION = 4;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PRODUCTS + "( "
            + COLUMN_PRODUCT_ID + " integer primary key autoincrement, "
            + COLUMN_PRODUCT_BARCODE + " text not null, "
            + COLUMN_PRODUCT_NAME	+ " text not null, "
            + COLUMN_PRODUCT_DESCRIPTION + " text not null, "
            + COLUMN_PRODUCT_QUANTITY + " integer not null, "
            + COLUMN_PRODUCT_EXPIRE_DATE + " text, "
            + COLUMN_IS_FAVORITE + " integer not null default 0);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }


    public long insertNewProduct(String barcode, String name, String description, String expire, String quantity) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_BARCODE, barcode);
        cv.put(COLUMN_PRODUCT_NAME, name);
        cv.put(COLUMN_PRODUCT_DESCRIPTION, description);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, expire);
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);

        long code = getWritableDatabase().insert(TABLE_PRODUCTS, null, cv);
        return code;
    }

    public Cursor getProducts() {
        return getWritableDatabase().query(TABLE_PRODUCTS, null, null,
                null, null, null, null);
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
