package com.example.smartpantry;

import static android.util.Log.ASSERT;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityMain extends AppCompatActivity
        implements
            SwipeRefreshLayout.OnRefreshListener,
            FragmentAddProduct.onProductAddedListener,
            FragmentManualEntryProduct.onManualEntryListener,
        FragmentPreviewProduct.onPreviewActionListener,
            FragmentFilters.onApplyFilters,
            FragmentAlreadySavedBarcode.onConfirmSearchPressedListener,
            NavigationView.OnNavigationItemSelectedListener,
            AdapterPantryList.onCardEvents {

    private DBHelper database;
    private AdapterPantryList pantryAdapter;
    private List<ProductPantryItem> pantryProducts;
    private RecyclerView pantryRecyclerView;
    private DrawerLayout drawerLayout;
    private SwipeRefreshLayout swipeRefresh;
    /*
    TODO:
         1. REFACTOR CODE USING THREADS FOR DB AND NETWORK OPERATIONS
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new DBHelper(getApplicationContext());
        setContentView(R.layout.activity_main);

        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        //Drawer Navigation setting
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        //Set listener for navigation view drawer's items
        navigationView.setNavigationItemSelectedListener(this);
        //Set headers for navigation view
        View headerView = navigationView.getHeaderView(0);
        //Title of drawer: if available the username it's written
        TextView navUsername = headerView.findViewById(R.id.username_view);

        swipeRefresh = findViewById(R.id.swipe_layout);
        swipeRefresh.setOnRefreshListener(this);

        navUsername.setText(getSharedPreferences(Global.USER_DATA, MODE_PRIVATE)
                .getString(Global.USERNAME, getResources().getString(R.string.accountText))
        );

        //Email written on navigation drawer
        TextView navEmail = headerView.findViewById(R.id.email_view);
        navEmail.setText(
                getSharedPreferences(Global.USER_DATA, MODE_PRIVATE).getString(Global.EMAIL,"")
        );

        //Option button to show navigation drawer
        ImageButton optionsBtn = findViewById(R.id.optionsBtn);
        optionsBtn.setOnClickListener(v-> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        //Products button
        ImageButton productsButton = findViewById(R.id.productsBtn);
        productsButton.setOnClickListener(v->{
            Intent showProductsIntent =  new Intent(this, ActivityShowProducts.class);
            startActivityForResult(showProductsIntent, Global.PRODUCTS_ACTIVITY);
        });

        //Scan central button
        ImageButton barcodeScan = findViewById(R.id.barcodeBtn);
        barcodeScan.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                enableCamera();
            } else {
                requestPermission();
            }
        });

        //Shopping list button
        ImageButton shoppingBtn =  findViewById(R.id.shoppingBtn);
        shoppingBtn.setOnClickListener(v->{
            Intent shoppingListIntent =  new Intent(this, ActivityShoppingList.class);
            startActivityForResult(shoppingListIntent, Global.SHOPPING_ACTIVITY);
        });

        //Recipes button
        ImageButton recipesButton =  findViewById(R.id.recipesBtn);
        recipesButton.setOnClickListener(v->{
            Toast.makeText(getApplicationContext(), "NOT YET AVAILABLE", Toast.LENGTH_LONG).show();
        });

        LinearLayout deleteAllProducts = findViewById(R.id.deleteAllProducts);
        deleteAllProducts.setOnClickListener(v->{
            askToDeleteAllProducts();
        });

        Button barcodeManualEntry = findViewById(R.id.manualEntryBtn);
        barcodeManualEntry.setOnClickListener(v -> {
            FragmentManualEntryProduct fragManualEntry = new FragmentManualEntryProduct();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.activity_main, fragManualEntry)
                    .addToBackStack(null)
                    .commit();
        });

        ImageButton filterButton = findViewById(R.id.imageFilterButton);
        filterButton.setOnClickListener(v->{
            FragmentFilters fragmentFilters = new FragmentFilters();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_main, fragmentFilters)
                    .addToBackStack(null)
                    .commit();
        });
        setSearchBox();
        setPantryRecyclerView();
        handleLogin();
        startPeriodicExpireCheck();

        Log.println(ASSERT, "USER ID", getSharedPreferences(Global.USER_DATA, MODE_PRIVATE).getString(Global.ID, "null"));
    }

    private void setSearchBox(){
        SearchView searchInPantryField = findViewById(R.id.searchProdField);
        SearchManager searchManager =  (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchInPantryField.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchInPantryField.setOnFocusChangeListener((view, hasFocus) -> {
            if(!hasFocus){
                //Clear Search Box
                searchInPantryField.setQuery("", false);

                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) ActivityMain.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityMain.this.getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        });

        searchInPantryField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) ActivityMain.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityMain.this.getWindow().getDecorView().getWindowToken(), 0);
                }

                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                ListView resultsListView = findViewById(R.id.searchResultsList);
                resultsListView.setDividerHeight(1);
                List<SuggestionItem> matchingProducts = new ArrayList<>();

                for(int i = 0; i < pantryProducts.size(); i++ ) {
                    ProductPantryItem item = pantryProducts.get(i);
                    String itemName = item.name;

                    if(!query.isEmpty() &&
                            itemName.toLowerCase().contains(query.toLowerCase())) {
                        matchingProducts.add(new SuggestionItem(
                                item.name,
                                item.description,
                                item.icon,
                                i));
                    }
                }

                AdapterSuggestionsList adapterSuggestionsList = (AdapterSuggestionsList) resultsListView.getAdapter();

                if (adapterSuggestionsList == null) {
                    adapterSuggestionsList = new AdapterSuggestionsList(
                            getApplicationContext(),
                            matchingProducts);
                    resultsListView.setAdapter(adapterSuggestionsList);
                } else {
                    adapterSuggestionsList.clear();
                    adapterSuggestionsList.addAll(matchingProducts);
                    adapterSuggestionsList.notifyDataSetChanged();
                }
                //Set on suggestion click event
                resultsListView.setOnItemClickListener((adapterView, view, position, id) -> {
                    //position inside the pantry
                    int positionInPantry = (int) view.getTag();
                    //Clear Search Box
                    searchInPantryField.setQuery("", false);

                    //Hide keyboard
                    InputMethodManager imm = (InputMethodManager) ActivityMain.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(ActivityMain.this.getWindow().getDecorView().getWindowToken(), 0);
                    }

                    LinearLayoutManager llm = ((LinearLayoutManager) pantryRecyclerView.getLayoutManager());

                    //If the item is fully visible flash it else scroll to it and then flash it
                    if( llm.findFirstCompletelyVisibleItemPosition() <= positionInPantry &&
                            llm.findLastCompletelyVisibleItemPosition() >= positionInPantry){
                        flashSearchedView(positionInPantry);
                    } else {
                        scrollPantryToPosition(positionInPantry);
                    }
                });
                return true;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switch (intent.getAction()) {
            case Global.EXPIRED_INTENT_ACTION: {
                //The Intent has been issued by the touch of a notification about
                //some items in the pantry are expired
                SharedPreferences.Editor ed = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE).edit();
                ed.putString(Global.TEMP_ORDER, DBHelper.COLUMN_PRODUCT_EXPIRE_DATE);
                ed.putString(Global.TEMP_FLOW, Global.ASC_ORDER);
                ed.apply();
                populatePantryList(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE, Global.ASC_ORDER);
                pantryAdapter.notifyDataSetChanged();
            }
            case Global.FAVORITES_INTENT_ACTION: {
                //The Intent has been issued by the touch of a notification about
                //some favorite items missing in the pantry
                Intent showProductsIntent =  new Intent(this, ActivityShowProducts.class);
                showProductsIntent.putExtra(Global.FAVORITES_INTENT_ACTION, true);
                startActivityForResult(showProductsIntent, Global.PRODUCTS_ACTIVITY);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch(item.getItemId()) {
            case R.id.logoutBtn:
                logout();
                break;
            case R.id.clearPantry:
                askToClearPantry();
                break;
            case R.id.manageNotifications:
                showNotificationManager();
                break;

        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case Global.CAMERA_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    Log.println(ASSERT, "ACTIVITY RESULT", "CAMERA ACTIVITY");
                    String barcode = data.getStringExtra("barcode");
                    if(productNeverSaved(barcode)) {
                        //TODO TEST AND CHANGE EVERYWHERE new Handler(Looper.getMainLooper()).post(() -> getProductsByBarcode(barcode, true));
                        getProductsByBarcode(barcode, true);

                    }
                }
                break;
            case Global.PRODUCTS_ACTIVITY:
            case Global.SHOPPING_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    Log.println(ASSERT, "ACTIVITY RESULT", "PRODUCTS ACTIVITY");
                    refreshPantry();
                }
        }
    }

    //Override of the method to refresh the SwipeRefreshLayout
    @Override
    public void onRefresh() {
        refreshPantry();
        swipeRefresh.setRefreshing(false);
    }

    //Interface Override of FragmentAddProduct method to add the product on the local db
    //and on the remote server
    @Override
    public void onProductAdded(String id, String barcode, String name, String description, String expire, long quantity,
                               String icon, boolean test, boolean addToPantry, boolean isNew) {
        if(isNew) {
            //If the item is new after being inserted in the server
            //it's added to the local db with the ID returned by the server
             addProductRemote(barcode, name, description, test, expire, quantity, icon, addToPantry);
        } else {
            //Else we already have the id and we can update the local db
            addProductLocal(id, barcode, name, description, expire, quantity, icon, addToPantry);
        }
        AdapterPantryList.pantryProducts.add(new ProductPantryItem(
                name, description, expire,
                id, icon, 0,
                quantity, 0));
        pantryAdapter.notifyItemInserted(pantryAdapter.getItemCount());
    }

    //Interface Override of FragmentManualEntryProduct method to receive the barcode from the fragment
    @Override
    public void onManualEntry(String barcode) {
        if(productNeverSaved(barcode)) {
            getProductsByBarcode(barcode, true);
        }
    }

    //Interface Override of AdapterPantryList method to open and close cards on touch
    @Override
    public void cardClicked(int position, int expandedPosition, int previouslyExpandedPosition) {
        pantryAdapter.notifyItemChanged(previouslyExpandedPosition);
        pantryAdapter.notifyItemChanged(expandedPosition);
        pantryRecyclerView.smoothScrollToPosition(position);
    }

    //Interface Override of FragmentPreviewProduct method to delete product from the server
    @Override
    public void onDeleteFromServer(String id, String barcode) {
        deleteProductFromServer(id, barcode);
    }

    @Override
    public void onVoteProduct(int preference, String id, String barcode) {
        voteProduct(preference, id, barcode);
    }

    //Interface Override of AdapterPantryList method to delete items from the pantry
    @Override
    public void deleteItem(int position) {
        pantryProducts.remove(position);
        pantryAdapter.notifyItemChanged(position);
        refreshPantry();
    }

    //Interface Override of 'apply button' pressed from FragmentFilters to apply filters to the pantry
    @Override
    public void applyFilters(boolean notInPantry, String order, String flow) {
        populatePantryList(order, flow);
        pantryAdapter.notifyDataSetChanged();
    }

    //Interface Override of 'confirm button' pressed from FragmentAlreadySavedBarcode to search the barcode
    @Override
    public void onConfirmSearchPressed(String barcode) {
        getProductsByBarcode(barcode, true);
    }

    private void refreshPantry() {
        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER,MODE_PRIVATE);
        String order = sp.getString(Global.TEMP_ORDER, sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE));
        String flow = sp.getString(Global.TEMP_FLOW, sp.getString(Global.FLOW, Global.DESC_ORDER));
        populatePantryList(order, flow);
        pantryAdapter.notifyDataSetChanged();
    }

    protected void startPeriodicExpireCheck() {
        //This method set a repeating alarm to check if inside the pantry there are expired products.
        //If so, it notify the user thanks to the broadcast receiver "AlarmBroadcastReceiver"
        IntentServiceSetAlarm.enqueueWork(getApplicationContext(), new Intent());
    }

    private boolean productNeverSaved(String barcode) {
        int matchingBarcodeCount = database.checkProductExistence(barcode);
        if(matchingBarcodeCount > 0) {
            askToPickFromProducts(barcode);
            return false;
        } else {
            return true;
        }
    }

    private void askToPickFromProducts(String barcode) {
        FragmentAlreadySavedBarcode alreadySavedBarcode = new FragmentAlreadySavedBarcode();
        Bundle bundle = new Bundle();
        bundle.putString("barcode", barcode);
        alreadySavedBarcode.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.activity_main, alreadySavedBarcode)
                .addToBackStack(null)
                .commit();
    }

    private void askToClearPantry() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        EditText confirmationEmail =  new EditText(getApplicationContext());
        confirmationEmail.setHint(R.string.emailText);
        FrameLayout frame = new FrameLayout(getApplicationContext());
        frame.addView(confirmationEmail);
        frame.setPadding(70, 15, 70, 0);
        builder.setTitle(getResources().getString(R.string.warningText));
        builder.setView(frame);
        builder.setMessage(getResources().getString(R.string.deleteItemsFromPantry))
                .setPositiveButton(
                        getResources().getString(R.string.confirmBtnText),
                        (dialog, id) -> {
                            DBHelper db = new DBHelper(getApplicationContext());
                            db.deleteAllProductsFromPantry();
                            db.close();
                            refreshPantry();

                            Toast.makeText(
                                    getApplicationContext(),
                                    getResources().getString(R.string.allProductsDeletedFromPantry),
                                    Toast.LENGTH_LONG)
                                    .show();
                        })
                .setNegativeButton(
                        getResources().getString(R.string.cancelText),
                        (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.setOnShowListener(arg0 -> {
            alert.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.button_confirm));

            alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.app_color));
            alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if(confirmationEmail.getText().toString()
                        .equals(getApplicationContext()
                                .getSharedPreferences(Global.USER_DATA, MODE_PRIVATE)
                                .getString(Global.EMAIL, ""))) {
                    DBHelper db = new DBHelper(getApplicationContext());
                    db.deleteAllProductsFromPantry();
                    db.close();
                    pantryAdapter.notifyItemRangeRemoved(0, pantryAdapter.getItemCount());
                    Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.allProductsDeletedFromPantry),
                            Toast.LENGTH_LONG)
                            .show();
                } else {
                    confirmationEmail.setError(getResources().getString(R.string.emailMissingErrorText));
                }
            });
        });
        alert.show();
    }

    private void showNotificationManager() {
        FragmentNotificationManager fragmentNotificationManager = new FragmentNotificationManager();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.activity_main, fragmentNotificationManager)
                .addToBackStack(null)
                .commit();
    }

    private void scrollPantryToPosition(int position){
        LinearLayoutManager llm = (LinearLayoutManager)pantryRecyclerView.getLayoutManager();
        RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                        llm.findFirstVisibleItemPosition() <= position &&
                        llm.findLastVisibleItemPosition() >= position) {
                    flashSearchedView(position);
                    recyclerView.removeOnScrollListener(this);
                }
            }
        };
        pantryRecyclerView.removeOnScrollListener(onScrollListener);
        pantryRecyclerView.addOnScrollListener(onScrollListener);
        pantryRecyclerView.postDelayed(()->{
            pantryRecyclerView.smoothScrollToPosition(position);
        }, 50);
    }

    public void flashSearchedView(int position){
        AnimationDrawable drawable = new AnimationDrawable();
        Handler handler = new Handler();
        drawable.addFrame(new ColorDrawable(ContextCompat
                        .getColor(getApplicationContext(), R.color.app_color_secondary)),
                1000);
        drawable.setExitFadeDuration(1000);
        drawable.addFrame(new ColorDrawable(ContextCompat
                        .getColor(getApplicationContext(), R.color.white)),
                1500);

        drawable.setOneShot(true);

        pantryRecyclerView.getLayoutManager().findViewByPosition(position).setBackground(drawable);
        handler.postDelayed(drawable::start, 100);
    }

    private void askToDeleteAllProducts() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        EditText confirmationEmail =  new EditText(getApplicationContext());
        confirmationEmail.setHint(R.string.emailText);
        FrameLayout frame = new FrameLayout(getApplicationContext());
        frame.addView(confirmationEmail);
        frame.setPadding(70, 15, 70, 0);
        builder.setTitle(getResources().getString(R.string.warningText));
        builder.setView(frame);
        builder.setMessage(getResources().getString(R.string.deleteAllProducts))
                .setPositiveButton(
                        getResources().getString(R.string.confirmBtnText), null)
                .setNegativeButton(
                        getResources().getString(R.string.cancelText),
                        (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.setOnShowListener(arg0 -> {
            alert.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.button_confirm));

            alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.app_color));
            alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if(confirmationEmail.getText().toString()
                        .equals(getApplicationContext()
                                .getSharedPreferences(Global.USER_DATA, MODE_PRIVATE)
                                .getString(Global.EMAIL, ""))) {
                    DBHelper db = new DBHelper(getApplicationContext());
                    db.dropAllTables();
                    db.close();
                    refreshPantry();
                    alert.dismiss();
                    Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.allProductsDeleted),
                            Toast.LENGTH_LONG).show();
                } else {
                    confirmationEmail.setError(getResources().getString(R.string.emailMissingErrorText));
                }
            });
        });
        alert.show();
    }

    private void addProductLocal(String id, String barcode, String name, String description,
                                 String expire, long quantity, String icon, boolean addToPantry) {
        long code = database.insertNewProduct(id, barcode, name, description, expire, quantity, icon, addToPantry);
        if (code == -1) {
            Toast.makeText(this, R.string.genericError, Toast.LENGTH_LONG).show();
            Log.wtf("ADD LOCAL", "ERROR ADDING PRODUCT TO LOCAL DB");
        }
    }

    private void handleLogin() {
        Log.println(ASSERT, "HANDLE LOGIN", "HANDLING");
        SharedPreferences sp = getSharedPreferences(Global.LOGIN, MODE_PRIVATE);
        int status = status(sp);
        switch (status) {
            case Global.LOGIN_STATUS_SHOW_LOGIN:
               Intent login = new Intent(this, ActivityLogin.class);
               startActivity(login);
               this.finish();
               break;
            case Global.LOGIN_STATUS_REQUEST_TOKEN:
                updateToken();
                break;
            case Global.LOGIN_STATUS_OK:
            default:
                break;
        }
    }

    public int status(@NonNull SharedPreferences sp){
        //JUST LOGGED
        if(sp.getBoolean(Global.CURRENT_SESSION, false)) {
            sp.edit()
                .putBoolean(Global.CURRENT_SESSION, false)
                .apply();
            Log.println(ASSERT, "LOGIN", "OK - JUST LOGGED");
            return Global.LOGIN_STATUS_OK;
        }
        //STAY LOGGED
        else if(sp.getBoolean(Global.STAY_LOGGED, false)) {
            //TOKEN IS VALID
            if(Global.isDateBeforeToday(sp.getString(Global.VALID_DATE, null))) {
                Log.println(ASSERT, "LOGIN", "OK - REMEMBERED USER WITH VALID TOKEN");
                return Global.LOGIN_STATUS_OK;
            }
            //TOKEN IS NOT VALID
            else {
                Log.println(ASSERT, "LOGIN", "AUTOMATICALLY REQUEST NEW TOKEN");
                return Global.LOGIN_STATUS_REQUEST_TOKEN;
            }
        }
        //DON'T STAY LOGGED
        else {
            Log.println(ASSERT, "LOGIN", "ASK TO LOGIN AS USER DIDN'T REQUEST TO STAY LOGGED");
            return Global.LOGIN_STATUS_SHOW_LOGIN;
        }
    }

    private void logout() {
        SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.LOGIN, MODE_PRIVATE);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putBoolean(Global.STAY_LOGGED, false);
        Ed.apply();
        Intent login = new Intent(this, ActivityLogin.class);
        startActivity(login);
        this.finish();
    }

    private void showMatchProducts(String barcode, @NonNull JSONArray products) {
        FragmentBarcodeListProducts oldFragment =
                (FragmentBarcodeListProducts) getSupportFragmentManager()
                        .findFragmentByTag("fragmentBarcodeListProducts");
        if(oldFragment != null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(oldFragment)
                    .commit();
        }
        Bundle bundle = new Bundle();
        bundle.putString("barcode", barcode);
        bundle.putString("productsString", products.toString());
        FragmentBarcodeListProducts fragmentBarcodeListProducts = new FragmentBarcodeListProducts();
        fragmentBarcodeListProducts.setArguments(bundle);
        getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.activity_main, fragmentBarcodeListProducts, "fragmentBarcodeListProducts")
            .addToBackStack(null)
            .commit();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.CAMERA},
            Global.CAMERA_REQUEST_CODE
        );
    }

    private void enableCamera() {
        Intent intent = new Intent(this, ActivityCamera.class);
        startActivityForResult(intent, Global.CAMERA_ACTIVITY);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void setPantryRecyclerView() {
        pantryRecyclerView = findViewById(R.id.pantryRecycler);
        pantryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pantryRecyclerView.setItemAnimator(null);
        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE);
        //Pick last saved order and flow from preferences and use it to display items
        //if no order has been specified the default is [favorites - DESC]
        pantryProducts = new ArrayList<>();
        populatePantryList(
                sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE),
                sp.getString(Global.FLOW, Global.DESC_ORDER)
        );
        pantryAdapter = new AdapterPantryList(pantryProducts, this);
        pantryRecyclerView.setAdapter(pantryAdapter);
    }

    private void populatePantryList(String order, String flow) {
        pantryProducts.clear();
        Cursor cursor = database.getPantryProducts(order, flow);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            pantryProducts.add(new ProductPantryItem(
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ID)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ICON)),
                cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_IS_FAVORITE)),
                cursor.getLong(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY)),
                cursor.getLong(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_TO_BUY_QUANTITY))
            ));
            cursor.moveToNext();
        }
        cursor.close();
    }

    /** HTTP CALLS */

    //UPDATE ACCESS TOKEN
    private void updateToken() {
        if (Global.checkConnectionAvailability(getApplicationContext())) {
            Log.println(ASSERT, "UPDATE TOKEN", "WE CONNECTED");
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest loginRequest = new StringRequest(Request.Method.POST, Global.LOGIN_URL,
                    response -> {
                        try {
                            Log.println(ASSERT, "UPDATE TOKEN", "TRYING");
                            JSONObject credentials = new JSONObject(response);
                            String token = credentials.get(Global.ACCESS_TOKEN).toString();
                            SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.LOGIN, MODE_PRIVATE);
                            SharedPreferences.Editor Ed = sp.edit();
                            Ed.putString(Global.ACCESS_TOKEN, token);
                            Ed.putString(Global.VALID_DATE, Global.getNewValidDate());
                            Ed.apply();

                            Log.println(ASSERT, "UPDATE TOKEN", "TOKEN " + token + " VALID UNTIL " + Global.getNewValidDate());
                        } catch (JSONException e) {
                            Log.println(ASSERT, "UPDATE TOKEN", "EXCEPTION");
                            e.printStackTrace();
                        }
                    },
                    Throwable::printStackTrace) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.USER_DATA, MODE_PRIVATE);
                    params.put(Global.EMAIL, sp.getString(Global.EMAIL, null));
                    params.put(Global.PASSWORD, sp.getString(Global.PASSWORD, null));
                    return params;
                }
            };
            queue.add(loginRequest);
        } else {
            logout();
        }
    }

    //IF SHOW IS TRUE GET AND SHOW PRODUCTS BY BARCODE
    //ELSE IT'S USED TO GET A NEW SESSION TOKEN
    private void getProductsByBarcode(String barcode, boolean show) {
        if (Global.checkConnectionAvailability(getApplicationContext())) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest showProductsByCodeRequest =
                    new StringRequest(Request.Method.GET, Global.LIST_PRODUCTS_URL + barcode,
                            response -> {
                                try {
                                    JSONObject responseObject = new JSONObject(response);
                                    String sessionToken = responseObject.getString("token");
                                    Log.println(ASSERT,"PRODS BY BARCODE","responseOBJECT " + responseObject.toString() );
                                    Log.println(ASSERT,"PRODS BY BARCODE","SESSION TOKEN " + sessionToken );

                                    JSONArray productsList = responseObject.getJSONArray("products");
                                    getApplicationContext()
                                            .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                                            .edit()
                                            .putString(Global.SESSION_TOKEN, sessionToken)
                                            .apply();
                                    if(show) {
                                        showMatchProducts(barcode, productsList);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }, Throwable::printStackTrace) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer "
                                    +getApplicationContext()
                                    .getSharedPreferences(Global.LOGIN, MODE_PRIVATE)
                                    .getString(Global.ACCESS_TOKEN, null));
                            return headers;
                        }
                    };
            queue.add(showProductsByCodeRequest);
        } else {
            Log.println(ASSERT, "PRODS BY BARCODE", "NO CONNECTION");
            Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
        }
    }

    //VOTE PRODUCT GIVING RATING, PRODUCT ID AND BARCODE
    public void voteProduct(int rating, String productID, String barcode) {
        if (Global.checkConnectionAvailability(getApplicationContext())) {
            JSONObject params = new JSONObject();
            try {
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                params.put("token", getApplicationContext()
                        .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                        .getString(Global.SESSION_TOKEN, null)
                );
                params.put("rating", rating);
                params.put("productId", productID);
                final String requestBody = params.toString();

                FragmentPreviewProduct preview = (FragmentPreviewProduct) getSupportFragmentManager()
                                .findFragmentByTag("previewFragment");
                StringRequest voteProductRequest = new StringRequest(Request.Method.POST, Global.VOTE_PRODUCT_URL,
                    response -> {
                        Log.println(ASSERT, "VOTE RESPONSE", response);
                        try {
                            JSONObject respObj = new JSONObject(response);
                            if(preview != null) {
                                preview.showRatingResult(respObj.getInt("rating"));
                            }
                            //This call with parameter 'show=false' refresh the session token
                            getProductsByBarcode(barcode, false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        if(preview != null) {
                            preview.handleError();
                        }
                    }
                ) {
                    @Override
                    public String getBodyContentType() {
                        return "application/json; charset=utf-8";
                    }

                    @Override
                    public byte[] getBody() {
                        return requestBody.getBytes(StandardCharsets.UTF_8);
                    }

                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer "
                                +getApplicationContext()
                                .getSharedPreferences(Global.LOGIN, MODE_PRIVATE)
                                .getString(Global.ACCESS_TOKEN, null));
                        return headers;
                    }

                };
                queue.add(voteProductRequest);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
        }
    }

    //METHOD TO DELETE PRODUCT FROM SERVER
    private void deleteProductFromServer(String id, String barcode) {
        Log.println(ASSERT, "DELETING", "id=" + id);
        if(Global.checkConnectionAvailability(getApplicationContext())){
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            StringRequest deleteProductRequest = new StringRequest(Request.Method.DELETE, Global.DELETE_PRODUCT_URL + id,
                    response -> {
                        getProductsByBarcode(barcode, true);
                    }, Throwable::printStackTrace)
            {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer "
                            +getApplicationContext()
                            .getSharedPreferences(Global.LOGIN, MODE_PRIVATE)
                            .getString(Global.ACCESS_TOKEN, null));
                    return headers;
                }
            };
            queue.add(deleteProductRequest);
        } else {
            Toast.makeText(this, getResources().getString(R.string.productDeletedError), Toast.LENGTH_LONG).show();
            Log.println(ASSERT, "REMOTE DELETE ERROR", "NO CONNECTION AVAILABLE");
        }
    }

    //SEND PRODUCT TO ADD TO THE SERVER
    private void addProductRemote(String barcode, String name, String description, boolean test,
                                  String expire, long quantity, String icon, boolean addToPantry) {
        if(Global.checkConnectionAvailability(getApplicationContext())){
            JSONObject params = new JSONObject();
            try {
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                //Add session token
                params.put("token",
                        getApplicationContext()
                                .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                                .getString(Global.SESSION_TOKEN, null));

                //Delete used session token
                getApplicationContext()
                        .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                        .edit()
                        .putString(Global.SESSION_TOKEN, null)
                        .apply();

                //add the remaining fields of the body
                params.put("name", name);
                params.put("description", description);
                params.put("barcode", barcode);
                params.put("test", test);

                JsonObjectRequest addProductRequest = new JsonObjectRequest(Request.Method.POST, Global.ADD_PRODUCT_URL, params,
                        response -> {
                            try {
                                String newID = response.getString("id");
                                Log.println(ASSERT, "REMOTE ADD RESPONSE", newID);
                                addProductLocal(newID, barcode, name, description, expire, quantity, icon, addToPantry);
                            } catch (JSONException e) {
                                //the upload of the new product has failed
                                Toast.makeText(this, R.string.genericError, Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }

                        }, Throwable::printStackTrace)
                {
                    @Override
                    public String getBodyContentType() {
                        return "application/json; charset=utf-8";
                    }
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        Log.println(ASSERT, "PRODUCT ADD REMOTE AUTH", getApplicationContext()
                                .getSharedPreferences(Global.LOGIN, MODE_PRIVATE)
                                .getString(Global.ACCESS_TOKEN, null));
                        headers.put("Authorization", "Bearer "
                                +getApplicationContext()
                                .getSharedPreferences(Global.LOGIN, MODE_PRIVATE)
                                .getString(Global.ACCESS_TOKEN, null));
                        return headers;
                    }
                };
                queue.add(addProductRequest);

            } catch (JSONException e) {
                e.printStackTrace();
                Log.println(ASSERT, "REMOTE ADD ERROR", "EXCEPTION");
            }


        } else {
            Toast.makeText(this, getResources().getString(R.string.noProductAddedError), Toast.LENGTH_LONG).show();
            Log.println(ASSERT, "REMOTE ADD ERROR", "NO CONNECTION AVAILABLE");
        }
    }

/*
    DELETE PRODUCT FROM SERVER
    public void deleteFromServer() {
    }
 */
}