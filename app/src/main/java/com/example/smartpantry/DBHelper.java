package com.example.smartpantry;

import static android.util.Log.ASSERT;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    //TABLE OF PREFERENCES AND COLUMNS
    public static final String TABLE_PREFERENCES = "preferences";
    public static final String COLUMN_PREFERENCE_PRODUCT_ID = "_prefID";
    public static final String COLUMN_PREFERENCE_PRODUCT_PREFERENCE = "productPreferenceVote";

    //TABLE OF PRODUCTS AND COLUMNS
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PRODUCT_ID = "_id";
    public static final String COLUMN_PRODUCT_BARCODE = "barcode";
    public static final String COLUMN_PRODUCT_NAME = "productName";
    public static final String COLUMN_PRODUCT_DESCRIPTION = "productDescription";
    public static final String COLUMN_PRODUCT_EXPIRE_DATE = "expireDate";
    public static final String COLUMN_PRODUCT_ICON = "icon";
    public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
    public static final String COLUMN_PRODUCT_IN_PANTRY = "inPantry";
    public static final String COLUMN_PRODUCT_IS_FAVORITE = "favorite";

    private static final String DATABASE_NAME = "products.db";
    private static final int DATABASE_VERSION = 13;

    // Products Database creation sql statement
    private static final String PRODUCTS_DATABASE_CREATE = "create table "
            + TABLE_PRODUCTS + "( "
            + COLUMN_PRODUCT_ID + " integer primary key autoincrement, "
            + COLUMN_PRODUCT_BARCODE + " text not null, "
            + COLUMN_PRODUCT_NAME	+ " text not null, "
            + COLUMN_PRODUCT_DESCRIPTION + " text not null, "
            + COLUMN_PRODUCT_QUANTITY + " integer not null, "
            + COLUMN_PRODUCT_EXPIRE_DATE + " text, "
            + COLUMN_PRODUCT_ICON + " text not null, "
            + COLUMN_PRODUCT_IN_PANTRY + " integer not null default 1, "
            + COLUMN_PRODUCT_IS_FAVORITE + " integer not null default 0);";

    // Ratings Database creation sql statement
    private static final String  PREFERENCES_DATABASE_CREATE = "create table "
            + TABLE_PREFERENCES + "( "
            + COLUMN_PREFERENCE_PRODUCT_ID + " text primary key, "
            + COLUMN_PREFERENCE_PRODUCT_PREFERENCE + " integer not null)";

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
        Log.println(ASSERT, DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
        onCreate(db);
    }

    public void insertNewPreference(String id, int rating) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PREFERENCE_PRODUCT_ID, id);
        cv.put(COLUMN_PREFERENCE_PRODUCT_PREFERENCE, rating);

        getWritableDatabase().insert(TABLE_PREFERENCES, null, cv);
    }

    public Integer getPreference(String id) {
        Cursor cursor = getReadableDatabase().query(TABLE_PREFERENCES, null,
                COLUMN_PREFERENCE_PRODUCT_ID + "='" + id + "'", null,
                null, null, null);
        if(cursor.getCount() > 0){
            cursor.moveToFirst();
            return cursor.getInt(cursor.getColumnIndex(COLUMN_PREFERENCE_PRODUCT_PREFERENCE));
        } else {
            cursor.close();
            return null;
        }
    }

    public long insertNewProduct(String barcode, String name, String description, String expire, String quantity, String icon, boolean addToPantry) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_BARCODE, barcode);
        cv.put(COLUMN_PRODUCT_NAME, name);
        cv.put(COLUMN_PRODUCT_DESCRIPTION, description);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, expire);
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);
        cv.put(COLUMN_PRODUCT_ICON, icon);
        cv.put(COLUMN_PRODUCT_IN_PANTRY, addToPantry ? 1: 0);

        return getWritableDatabase().insert(TABLE_PRODUCTS, null, cv);
    }

    public void changeExpireDate(String id, String expire) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, expire);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?", new String[] {id});
    }

    public void changeQuantity(String id, int quantity) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?", new String[] {id});
    }

    public void deleteProductFromPantry(String id) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_IN_PANTRY, 0);
        cv.put(COLUMN_PRODUCT_QUANTITY, 0);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?", new String[] {id});
    }

    public void deleteAllProductsFromPantry() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_IN_PANTRY, 0);
        cv.put(COLUMN_PRODUCT_QUANTITY, 0);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, null, null);
    }

    public Cursor getPantryProducts(String order, String flow) {
        //If the ordering is EXPIRE DATE ASC the NULL values are displayed after the others
        if(order.equals(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE) &&
           flow.equals(Global.ASC_ORDER)) {
            order = "CASE WHEN "+ DBHelper.COLUMN_PRODUCT_EXPIRE_DATE +
                    " IS NULL THEN 1 ELSE 0 END, "+ DBHelper.COLUMN_PRODUCT_EXPIRE_DATE;
        }
        return getReadableDatabase().query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_IN_PANTRY +"=1",
                null, null, null, order + " " + flow);
    }

    public void setFavorite(boolean state, String id){
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_IS_FAVORITE, state ? 1 : 0);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?",
                new String[] {id});
    }
/*
    public Cursor searchMatchInProductsName(String selection) {
        if(selection!=null && !selection.isEmpty()) {
            return getReadableDatabase().query(
                    TABLE_PRODUCTS,
                    new String[]{COLUMN_PRODUCT_NAME, COLUMN_PRODUCT_DESCRIPTION, COLUMN_PRODUCT_ICON, COLUMN_PRODUCT_ID},
                    COLUMN_PRODUCT_IN_PANTRY + "=1 AND " + COLUMN_PRODUCT_NAME + " LIKE ?",
                    new String[]{"%" + selection + "%"}, null, null, null, null);
        } else {
            return getReadableDatabase().query(TABLE_PRODUCTS,
                    new String[]{COLUMN_PRODUCT_NAME, COLUMN_PRODUCT_DESCRIPTION, COLUMN_PRODUCT_ICON, COLUMN_PRODUCT_ID},
                    COLUMN_PRODUCT_IS_FAVORITE + "=-1", null, null, null, null);
        }
    }
*/

    public void deleteProduct(int id) {
        getWritableDatabase().delete(TABLE_PRODUCTS, COLUMN_PRODUCT_ID + COLUMN_PRODUCT_ID + "=?",
                new String[] { String.valueOf(id) });
    }

    public Cursor getFavorites() {
        return getReadableDatabase().query(TABLE_PRODUCTS, null,
                COLUMN_PRODUCT_IS_FAVORITE + "= 1", null,
                null, null, null);
    }

    public void dropAllTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
        onCreate(db);
    }
}
