package com.example.smartpantry;

import static android.util.Log.ASSERT;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    //TABLE RECIPES RATINGS
    public static final String TABLE_REC_RATINGS = "recipesRatings";
    public static final String COLUMN_RATING_REC_ID = "rec_id";
    public static final String COLUMN_RATING_VAL = "rating";

    //TABLE OF PREFERENCES AND COLUMNS
    public static final String TABLE_PREFERENCES = "preferences";
    public static final String COLUMN_PREFERENCE_PRODUCT_ID = "_prefID";
    public static final String COLUMN_PREFERENCE_PRODUCT_PREFERENCE = "productPreferenceVote";

    //TABLE OF PRODUCTS AND COLUMNS
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PRODUCT_ID = "_id";
    public static final String COLUMN_PRODUCT_UNIQUE_ID = "uniqueID";
    public static final String COLUMN_PRODUCT_BARCODE = "barcode";
    public static final String COLUMN_PRODUCT_NAME = "productName";
    public static final String COLUMN_PRODUCT_DESCRIPTION = "productDescription";
    public static final String COLUMN_PRODUCT_EXPIRE_DATE = "expireDate";
    public static final String COLUMN_PRODUCT_ICON = "icon";
    public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
    public static final String COLUMN_PRODUCT_TO_BUY_QUANTITY = "toBuyQnt";
    public static final String COLUMN_PRODUCT_IN_PANTRY = "inPantry";
    public static final String COLUMN_PRODUCT_IS_FAVORITE = "favorite";

    private static final String DATABASE_NAME = "products.db";
    private static final int DATABASE_VERSION = 19;

    // Products Database creation sql statement
    private static final String PRODUCTS_DATABASE_CREATE = "create table if not exists "
            + TABLE_PRODUCTS + "( "
            + COLUMN_PRODUCT_UNIQUE_ID + "integer primary key,"
            + COLUMN_PRODUCT_ID + " text not null, "
            + COLUMN_PRODUCT_BARCODE + " text not null, "
            + COLUMN_PRODUCT_NAME	+ " text not null, "
            + COLUMN_PRODUCT_DESCRIPTION + " text not null, "
            + COLUMN_PRODUCT_QUANTITY + " integer not null, "
            + COLUMN_PRODUCT_TO_BUY_QUANTITY + " integer not null default 0, "
            + COLUMN_PRODUCT_EXPIRE_DATE + " text, "
            + COLUMN_PRODUCT_ICON + " text not null, "
            + COLUMN_PRODUCT_IN_PANTRY + " integer not null default 1, "
            + COLUMN_PRODUCT_IS_FAVORITE + " integer not null default 0);";

    // Ratings Database creation sql statement
    private static final String  PREFERENCES_DATABASE_CREATE = "create table if not exists "
            + TABLE_PREFERENCES + "( "
            + COLUMN_PREFERENCE_PRODUCT_ID + " text primary key, "
            + COLUMN_PREFERENCE_PRODUCT_PREFERENCE + " integer not null)";

    // Recipes Ratings Table creation sql statement
    private static final String RECIPES_RATINGS_DATABASE_CREATE = "create table if not exists "
            + TABLE_REC_RATINGS + "( "
            + COLUMN_RATING_REC_ID + " texts not null, "
            + COLUMN_RATING_VAL + " integer not null)";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(PREFERENCES_DATABASE_CREATE);
        database.execSQL(PRODUCTS_DATABASE_CREATE);
        database.execSQL(RECIPES_RATINGS_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.println(ASSERT, DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REC_RATINGS);
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

    public long insertNewProduct(String id, String barcode, String name, String description, String expire, long quantity, String icon, boolean addToPantry) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_ID, id);
        cv.put(COLUMN_PRODUCT_BARCODE, barcode);
        cv.put(COLUMN_PRODUCT_NAME, name);
        cv.put(COLUMN_PRODUCT_DESCRIPTION, description);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, (expire.isEmpty()? null: expire));
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);
        cv.put(COLUMN_PRODUCT_ICON, icon);
        cv.put(COLUMN_PRODUCT_IN_PANTRY, addToPantry ? 1: 0);

        return getWritableDatabase().insert(TABLE_PRODUCTS, null, cv);
    }

    public void changeExpireDate(String id, String expire) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, (expire.isEmpty()? null: expire));
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?", new String[] {id});
    }

    public void changeQuantity(String id, long quantity) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?", new String[] {id});
    }

    public void deleteProductFromPantry(String id) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_IN_PANTRY, 0);
        cv.put(COLUMN_PRODUCT_QUANTITY, 0);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, (String) null);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?", new String[] {id});
    }

    public void deleteAllProductsFromPantry() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_IN_PANTRY, 0);
        cv.put(COLUMN_PRODUCT_QUANTITY, 0);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, (String) null);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, null, null);
    }

    public Cursor getPantryProducts(String order, String flow) {
        //If the ordering is EXPIRE DATE ASC the NULL values are displayed after the others
        if(order.equals(COLUMN_PRODUCT_EXPIRE_DATE) &&
           flow.equals(Global.ASC_ORDER)) {
            order = "CASE WHEN "+ COLUMN_PRODUCT_EXPIRE_DATE +
                    " IS NULL THEN 1 ELSE 0 END, "+ COLUMN_PRODUCT_EXPIRE_DATE;
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

    public Cursor getAllProducts(boolean notInPantry, String order, String flow) {
        if(order.equals(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)){
            order = convertExpiredOrdering();
        }
        return getReadableDatabase().query(TABLE_PRODUCTS, null, notInPantry ? COLUMN_PRODUCT_IN_PANTRY +"=0" : null,
                null, null, null, order + " " + flow);
    }

    public Cursor getShoppingListProducts(String order, String flow) {
        return getReadableDatabase().query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_TO_BUY_QUANTITY + ">0",
                null, null, null, order + " " + flow);
    }

    public Cursor getFavoriteProductsOnly() {
        return getReadableDatabase().query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_IS_FAVORITE +"=1",
                null, null, null, null );
    }

    //this function transform the order part of the query if the ordering is by expireDate
    //to give the user a more user-friendly output
    private String convertExpiredOrdering() {
        return "CASE WHEN " + DBHelper.COLUMN_PRODUCT_EXPIRE_DATE + " IS NULL AND " + DBHelper.COLUMN_PRODUCT_IN_PANTRY + "=0 THEN 2 " +
                "WHEN " + DBHelper.COLUMN_PRODUCT_EXPIRE_DATE + " IS NULL AND " + DBHelper.COLUMN_PRODUCT_IN_PANTRY + "=1 THEN 1 "+
                "ELSE 0 END, " + DBHelper.COLUMN_PRODUCT_EXPIRE_DATE;

    }

    public void deleteProduct(String id) {
        getWritableDatabase().delete(TABLE_PRODUCTS, COLUMN_PRODUCT_ID + "=?",
                new String[] { id });
    }

    public void dropAllTables(boolean dropPrefs) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        if(dropPrefs)
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
        onCreate(db);
    }

    public void updateProduct(String id, boolean toAdd, long quantity, String expireDate) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_QUANTITY, quantity);
        cv.put(COLUMN_PRODUCT_IN_PANTRY, toAdd ? 1 : 0);
        cv.put(COLUMN_PRODUCT_EXPIRE_DATE, expireDate);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?",
                new String[] {id});
    }

    public void changeIcon(String icon, String id) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_ICON, icon);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?",
                new String[] {id});

    }

    public void addToShoppingList(String id, long addQuantity) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_TO_BUY_QUANTITY, addQuantity);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?",
                new String[] {id});
    }

    public void updateShoppingProduct(String id, long toBuyQnt) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PRODUCT_TO_BUY_QUANTITY, 0);
        cv.put(COLUMN_PRODUCT_QUANTITY, toBuyQnt);
        cv.put(COLUMN_PRODUCT_IN_PANTRY, toBuyQnt>0 ? 1 : 0);
        getWritableDatabase().update(TABLE_PRODUCTS, cv, COLUMN_PRODUCT_ID + "=?",
                new String[] {id});
    }

    //Check product existence by product ID
    public boolean checkProductSaved(String productID) {
        Cursor cursor =  getReadableDatabase().query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_ID + "=?", new String[] {productID},
                null, null, null);
        boolean matching = cursor.getCount() > 0;
        cursor.close();
        return matching;
    }

    //check product existence by barcode
    public int checkProductExistence(String barcode) {
        Cursor cursor =  getReadableDatabase().query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_BARCODE + "=?", new String[] {barcode},
                null, null, null);
        int matchingBarcodeCount = cursor.getCount();
        cursor.close();
        return matchingBarcodeCount;
    }

    //Get number of expired items
    public int getExpiredProductsCount() {
        String today = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.CHINA).format(Calendar.getInstance().getTime());
        Cursor cursor = getReadableDatabase().query(
                TABLE_PRODUCTS, null,
                COLUMN_PRODUCT_IN_PANTRY + "=1 AND Date(\"" +
                        COLUMN_PRODUCT_EXPIRE_DATE + " - INTERVAL 3 DAY\") <= Date(\"" + today + "\")",
                null, null, null, null);
        int expiredItemsCount = cursor.getCount();
        cursor.close();
        return expiredItemsCount;
    }

    //Get number of favorite products missing in the pantry and not in the shopping list
    public int getMissingFavoritesCount() {
        Cursor cursor = getReadableDatabase().query(
                TABLE_PRODUCTS, null,
                COLUMN_PRODUCT_IN_PANTRY + "=0 AND " +
                        COLUMN_PRODUCT_TO_BUY_QUANTITY + "<=0 AND " +
                        COLUMN_PRODUCT_IS_FAVORITE +"=1",
                null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public Integer getRating(String rec_id) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_REC_RATINGS, new String[]{COLUMN_RATING_VAL},
                COLUMN_RATING_REC_ID + "=?", new String[]{rec_id}, null, null, null, null);
        Integer rating = null;
        Log.println(ASSERT, "found ratings db", cursor.getCount()+"");
        if (cursor.getCount()>0) {
            cursor.moveToFirst();
            rating = cursor.getInt(cursor.getColumnIndex(COLUMN_RATING_VAL));
        }
        return rating;
    }

    public void insertRating(String rec_id, float rating) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_RATING_VAL, rating);
        cv.put(COLUMN_RATING_REC_ID, rec_id);
        getWritableDatabase().insert(TABLE_REC_RATINGS, null, cv);
    }

}
