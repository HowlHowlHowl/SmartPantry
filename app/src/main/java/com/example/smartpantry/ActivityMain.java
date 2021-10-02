package com.example.smartpantry;

import static android.util.Log.ASSERT;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityMain extends AppCompatActivity
        implements
                FragmentAddProduct.onProductAddedListener,
                FragmentManuelEntryProduct.onManualEntryListener,
                FragmentFilters.onApplyFilters,
                NavigationView.OnNavigationItemSelectedListener,
                AdapterPantryList.onCardClicked {

    private DBHelper database;

    private AdapterPantryList pantryAdapter;


    private List<ProductPantryItem> pantryProducts;
    private RecyclerView pantryRecyclerView;
    private DrawerLayout drawerLayout;
    /*TODO:
        REFACTOR CODE USING THREADS FOR DB AND NETWORK OPERATIONS
         ADD IMAGE TO USER
         NOTIFY AND SET TEXT RED FOR EXPIRED ITEMS
         EVENTUALLY ASK TO PUT EXPIRED ITEMS IN SHOPPING LIST (INSTEAD OF ASKING FOR CONFIRMATION?)
         PRODUCTS ACTIVITY WITH SEARCH MECHANISM
         SHOPPING LIST ACTIVITY WITH CHECKBOX, SEARCH AND DELETE MECHANISM

    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO: DELETE THIS LINE MF.
        getSharedPreferences(Global.USER_DATA, MODE_PRIVATE).edit().putString(Global.ID, "cktx3ll3i3865020o8ook5ge5v").apply();

        database = new DBHelper(getApplicationContext());
        setContentView(R.layout.activity_main);

        Log.println(ASSERT, "USER ID", "" +
                getSharedPreferences(Global.USER_DATA, MODE_PRIVATE).getString(Global.ID, null));


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

        ImageButton barcodeScan = findViewById(R.id.barcodeBtn);
        barcodeScan.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                enableCamera();
            } else {
                requestPermission();
            }
        });

        LinearLayout deleteAllProducts = findViewById(R.id.deleteAllProducts);
        deleteAllProducts.setOnClickListener(v->{
            askToDeleteAllProducts();
        });

        Button barcodeManualEntry = findViewById(R.id.manualEntryBtn);
        barcodeManualEntry.setOnClickListener(v -> {
            FragmentManuelEntryProduct fragManualEntry = new FragmentManuelEntryProduct();
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


        SearchView searchInPantryField = findViewById(R.id.searchProdField);
        LinearLayout searchBox = findViewById(R.id.searchBox);
        searchBox.setOnClickListener(v->{
            searchInPantryField.callOnClick();
        });

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
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                ListView resultsListView = findViewById(R.id.searchResultsList);
                List<SuggestionItem> matchingProducts = new ArrayList<>();

                for(int i = 0; i<pantryProducts.size(); i++ ) {
                    ProductPantryItem item = pantryProducts.get(i);
                    String itemName = item.name;

                    if(!query.isEmpty() &&
                            itemName.toLowerCase().contains(query.toLowerCase())) {
                        matchingProducts.add(new SuggestionItem(
                                item.name, item.description, item.icon, i));
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
        setPantryRecyclerView();
        handleLogin();
        startPeriodicExpireCheck();
    }

    protected void startPeriodicExpireCheck() {
        //This method set a repeating alarm to check if inside the pantry there are expired products.
        //If so, it notify the user thanks to the broadcast receiver "AlarmBroadcastReceiver"
        Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
        intent.setAction(Global.EXPIRED_INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                Global.REQUEST_CODE_CHECK,
                intent, PendingIntent.FLAG_UPDATE_CURRENT); //The warning for the last parameter is due to a bug resolved
                                                            // in the new alpha release but the alpha it's unstable
        //calendar will hold the time for the alarm to fire
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //Set hour and minute, hardcoded values because a product always expire after midnight so there shouldn't be no need to change these numbers
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 15);
        calendar.set(Calendar.SECOND, 0);
        //If current hour is past 00:15 set the date to next day
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        //Hour of the day
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }
    @Override
    protected void onStop() {
        //Clear temporary sorting preferences
        SharedPreferences.Editor ed = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE).edit();
        ed.putString(Global.TEMP_ORDER, null);
        ed.putString(Global.TEMP_FLOW, null);
        ed.commit();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(Global.EXPIRED_INTENT_ACTION)) {
            SharedPreferences.Editor ed = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE).edit();
            ed.putString(Global.TEMP_ORDER, DBHelper.COLUMN_PRODUCT_EXPIRE_DATE);
            ed.putString(Global.TEMP_FLOW, Global.ASC_ORDER);
            ed.apply();
            populatePantryList(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE, Global.ASC_ORDER);
            pantryAdapter.notifyDataSetChanged();
        }
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
        handler.postDelayed(() -> {
            drawable.start();
            //
            // pantryRecyclerView.getLayoutManager().findViewByPosition(position).callOnClick();

        }, 100);
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

        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
    private void refreshPantry() {
        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER,MODE_PRIVATE);
        String order = sp.getString(Global.TEMP_ORDER, sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE));
        String flow = sp.getString(Global.TEMP_FLOW, sp.getString(Global.FLOW, Global.DESC_ORDER));
        populatePantryList(order, flow);
        pantryAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case Global.CAMERA_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    String barcode = data.getStringExtra("barcode");
                    Log.println(ASSERT, "RESULT BARCODE", barcode);
                    getProductsByBarcode(barcode, true);
                }
                break;
        }
    }

    //Interface implementation of method to add product from addProductActivity
    @Override
    public void productAdded(String barcode, String name, String description, String expire, String quantity,
                             String icon, boolean test, boolean addToPantry, boolean isNew) {
        if(isNew) {
            addProductRemote(barcode, name, description, test);
        }

        long id = addProductLocal(barcode, name, description, expire, quantity, icon, addToPantry);
        AdapterPantryList.pantryProducts.add(new ProductPantryItem(
                name, description, expire,
                Long.toString(id), icon, 0,
                Integer.parseInt(quantity)));
        pantryAdapter.notifyItemInserted(pantryAdapter.getItemCount());
    }

    //Interface implementation of method to receive barcode from manual entry
    @Override
    public void manualEntry(String barcode) {
        getProductsByBarcode(barcode, true);
    }

    @Override
    public void cardClicked(int position, int height, int expandedPosition, int previouslyExpandedPosition) {
        pantryAdapter.notifyItemChanged(previouslyExpandedPosition);
        pantryAdapter.notifyItemChanged(expandedPosition);

        pantryRecyclerView.smoothScrollToPosition(position);
        pantryRecyclerView.smoothScrollBy(0, height);
    }

    @Override
    public void applyFilters(String order, String flow) {
        populatePantryList(order, flow);
        pantryAdapter.notifyDataSetChanged();
    }

    private long addProductLocal(String barcode, String name, String description, String expire, String quantity, String icon, boolean addToPantry) {
        long code = database.insertNewProduct(barcode, name, description, expire, quantity, icon, addToPantry);
        if (code == -1) {
            Toast.makeText(this, R.string.genericError, Toast.LENGTH_LONG).show();
            Log.wtf("ADD LOCAL", "_____________ ERROR ADDING PRODUCT TO LOCAL DB _____________");
        }
        return code;
    }

    private void addProductRemote(String barcode, String name, String description, boolean test) {
        if(Global.checkConnectionAvailability(getApplicationContext())){
            JSONObject params = new JSONObject();
            try {
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                Log.println(ASSERT, "REMOTE ADD ", "SESSION TOKEN " + getApplicationContext()
                        .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                        .getString(Global.SESSION_TOKEN, null));
                //Add session token
                params.put("token",
                        getApplicationContext()
                                .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                                .getString(Global.SESSION_TOKEN, null));

                //Delete used session token
                getApplicationContext()
                        .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                        .edit()
                        .putString(Global.SESSION_TOKEN, "")
                        .commit();

                //add the remaining fields of the body
                params.put("name", name);
                params.put("description", description);
                params.put("barcode", barcode);
                params.put("test", test);
                Log.println(ASSERT, "REMOTE ADD ", "BODY CREATED" + params.toString());
                StringRequest addProductRequest = new StringRequest(Request.Method.POST, Global.ADD_PRODUCT_URL,
                        response -> {
                    Log.println(ASSERT, "PRODUCT ADD REMOTE RESPONSE:", response);
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

    public int status(SharedPreferences sp){
        //JUST LOGGED
        if(sp.getBoolean(Global.CURRENT_SESSION, false)) {
            sp.edit()
                .putBoolean(Global.CURRENT_SESSION, false)
                .commit();
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

    private void updateToken() {
        if (Global.checkConnectionAvailability(getApplicationContext())) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest loginRequest = new StringRequest(Request.Method.POST, Global.LOGIN_URL,
                    response -> {
                        try {
                            JSONObject credentials = new JSONObject(response);
                            String token = credentials.get(Global.ACCESS_TOKEN).toString();
                            SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.LOGIN, MODE_PRIVATE);
                            SharedPreferences.Editor Ed = sp.edit();
                            Ed.putString(Global.ACCESS_TOKEN, token);
                            Ed.putString(Global.VALID_DATE, Global.getNewValidDate());
                            Ed.commit();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                    }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.USER_DATA, MODE_PRIVATE);
                            params.put(Global.EMAIL, sp.getString(Global.EMAIL, null));
                            params.put("password", sp.getString("password", null));
                            return params;
                }
            };
            queue.add(loginRequest);
        } else {
            logout();
        }
    }

    private void logout() {
        SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.LOGIN, MODE_PRIVATE);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putBoolean(Global.STAY_LOGGED, false);
        Ed.commit();
        Intent login = new Intent(this, ActivityLogin.class);
        startActivity(login);
        this.finish();
    }

    private void getProductsByBarcode(String barcode, boolean show) {
        //TODO check in pantry and in products? Eventually show a pop up saying so
        if (Global.checkConnectionAvailability(getApplicationContext())) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest showProductsByCodeRequest =
                    new StringRequest(Request.Method.GET, Global.LIST_PRODUCTS_URL + barcode,
                    response -> {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String sessionToken = responseObject.getString("token");
                            Log.println(ASSERT,"responseOBJECT", responseObject.toString() );
                            Log.println(ASSERT,"SESSION TOKEN", sessionToken );

                            JSONArray productsList = responseObject.getJSONArray("products");
                            getApplicationContext()
                                    .getSharedPreferences(Global.UTILITY, MODE_PRIVATE)
                                    .edit()
                                    .putString(Global.SESSION_TOKEN, sessionToken)
                                    .commit();
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
            Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
        }
    }

    private void showMatchProducts(String barcode, JSONArray products) {
        Bundle bundle = new Bundle();
        bundle.putString("barcode", barcode);
        bundle.putString("productsString", products.toString());
        FragmentListProducts fragmentListProducts = new FragmentListProducts();
        fragmentListProducts.setArguments(bundle);
        getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.activity_main, fragmentListProducts)
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
                sp.getString("flow", Global.DESC_ORDER)
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
                cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY))
            ));
            cursor.moveToNext();
        }
        cursor.close();
    }

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
                            //This refresh the session token
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

}