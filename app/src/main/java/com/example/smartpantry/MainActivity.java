package com.example.smartpantry;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.util.Log.ASSERT;

public class MainActivity extends AppCompatActivity
        implements
                AddProductFragment.onProductAddedListener,
                ManuelEntryProductFragment.onManualEntryListener,
                NavigationView.OnNavigationItemSelectedListener,
                RVAdapterPantry.onCardClicked {
    static final int CAMERA_REQUEST_CODE = 1;
    static final int CAMERA_ACTIVITY = 101;
    static final int STATUS_OK = 200;
    static final int SHOW_LOGIN = 201;
    static final int REQUEST_TOKEN = 202;

    private static final String LOGIN_URL = Global.login_url;
    private static final String LIST_PRODUCTS_URL = Global.list_products_url;
    private static final String ADD_PRODUCT_URL = Global.add_product_url;
    private static final String VOTE_PRODUCT_URL = Global.vote_product_url;

    private final int DAYS_FOR_TOKEN_TO_EXPIRE = Global.token_valid_days;

    private DBHelper database;

    private List<ProductPantryItem> pantryProducts;
    private RecyclerView pantryRecyclerView;
    private RVAdapterPantry pantryAdapter;
    private DrawerLayout drawerLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    //TODO: ADD IMAGE TO USER & END YOU SURE ABOUT LOSING EVERYTHING? CONTROLLA SE DOPO REGISTRAZIONE C'E'  LOGIN
        super.onCreate(savedInstanceState);
        database = new DBHelper(getApplicationContext());
        setContentView(R.layout.activity_main);

        Log.println(ASSERT, "USER ID", "" +
                getSharedPreferences("UserData", MODE_PRIVATE).getString("id", null));


        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        //Drawer Navigation setting
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        //Set listener for navigation view drawer's items
        navigationView.setNavigationItemSelectedListener(this);
        //Set headers for navigation view
        View headerView = navigationView.getHeaderView(0);
        //Title of drawer: if available the username it's written, otherwise the word "Settings" tales place
        TextView navUsername = headerView.findViewById(R.id.username_view);

        navUsername.setText(getSharedPreferences("UserData", MODE_PRIVATE)
                .getString("username", getResources().getString(R.string.accountText))
        );
        //Email
        TextView navEmail = headerView.findViewById(R.id.email_view);
        navEmail.setText(
                getSharedPreferences("UserData", MODE_PRIVATE).getString("email","")
        );

        //Option button to show side drawer
        ImageButton optionsBtn = findViewById(R.id.optionsBtn);
        optionsBtn.setOnClickListener(v-> drawerLayout.openDrawer(GravityCompat.START));

        ImageButton barcodeScan = findViewById(R.id.barcodeBtn);
        barcodeScan.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                enableCamera();
            } else {
                requestPermission();
            }
        });

        Button barcodeManualEntry = findViewById(R.id.manualEntryBtn);
        barcodeManualEntry.setOnClickListener(v -> {
            ManuelEntryProductFragment fragManualEntry = new ManuelEntryProductFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.activity_main, fragManualEntry)
                    .addToBackStack(null)
                    .commit();
        });
        setPantryRecyclerView();
        handleLogin();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch(item.getItemId()) {
            case R.id.logoutBtn:
                logout();
                break;
            case R.id.clearPantry:
                //TODO clear pantry w/ pop-up
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case CAMERA_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    String barcode = data.getStringExtra("barcode");
                    Log.println(ASSERT, "RESULT BARCODE", barcode);
                    getProductsByBarcode(barcode, true);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //Interface implementation of method to add product from addProductActivity
    @Override
    public void productAdded(String barcode, String name, String description, String expire, String quantity,
                             String icon, boolean test, boolean addToPantry, boolean isNew) {

        Log.println(ASSERT, "INCOMING PRODUCT TO ADD:", "barcode " + barcode+ "\nname " + name +
                "\ndescription " + description + "\nexpire " + expire + "\n quantity " + quantity +
                "\n icon " + icon + "\ntest " + test + "\naddLocal " + addToPantry + "\nisNew " + isNew
        );

        if(isNew) {
            addProductRemote(barcode, name, description, test);
        }

        long id = addProductLocal(barcode, name, description, expire, quantity, icon, addToPantry);
        RVAdapterPantry.pantryProducts.add(new ProductPantryItem(
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
    public void cardClicked(int height, int expandedPosition, int previouslyExpandedPosition) {
        pantryAdapter.notifyItemChanged(previouslyExpandedPosition);
        pantryAdapter.notifyItemChanged(expandedPosition);
        pantryRecyclerView.smoothScrollBy(0, height);
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
        if(checkConnectionAvailability()){
            JSONObject params= new JSONObject();
            try {
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                params.put("token",
                        getApplicationContext()
                                .getSharedPreferences("Utils", MODE_PRIVATE)
                                .getString("sessionToken", null));
                params.put("name", name);
                params.put("description", description);
                params.put("barcode", barcode);
                params.put("test", test);
                Log.println(ASSERT, "REMOTE ADD ", "BODY CREATED" + params.toString());
                StringRequest addProductRequest = new StringRequest(Request.Method.POST, ADD_PRODUCT_URL,
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
                                + getApplicationContext()
                                .getSharedPreferences("Login", MODE_PRIVATE)
                                .getString("accessToken", null));
                        return headers;
                    }

                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        String responseString = "";
                        if (response != null) {
                            responseString = String.valueOf(response.statusCode);
                            Log.println(ASSERT, "REMOTE ADD RESPONSE ", response.toString() + " CODE " + responseString);
                        }

                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
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
        SharedPreferences sp = getSharedPreferences("Login", MODE_PRIVATE);
        int status = status(sp);
        switch (status) {
            case SHOW_LOGIN:
               Intent login = new Intent(this, LoginActivity.class);
               startActivity(login);
               this.finish();
               break;
            case REQUEST_TOKEN:
                updateToken();
                break;
            case STATUS_OK:
            default:
                break;
        }
    }

    public int status(SharedPreferences sp){
        //JUST LOGGED
        if(sp.getBoolean("currentSession", false)) {
            sp.edit()
                .putBoolean("currentSession", false)
                .commit();
            Log.println(ASSERT, "LOGIN", "OK - JUST LOGGED");
            return STATUS_OK;
        }
        //STAY LOGGED
        else if(sp.getBoolean("stayLogged", false)) {
            //TOKEN IS VALID
            if(isTokenDateValid(sp.getString("validDate", null))) {
                Log.println(ASSERT, "LOGIN", "OK - REMEMBERED USER WITH VALID TOKEN");
                return STATUS_OK;
            }
            //TOKEN IS NOT VALID
            else {
                Log.println(ASSERT, "LOGIN", "AUTOMATICALLY REQUEST NEW TOKEN");
                return REQUEST_TOKEN;
            }
        }
        //DON'T STAY LOGGED
        else {
            Log.println(ASSERT, "LOGIN", "ASK TO LOGIN AS USER DIDN'T REQUEST TO STAY LOGGED");
            return SHOW_LOGIN;
        }
    }

    private void updateToken() {
        if (checkConnectionAvailability()) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest loginRequest = new StringRequest(Request.Method.POST, LOGIN_URL,
                    response -> {
                        try {
                            JSONObject credentials = new JSONObject(response);
                            String token = credentials.get("accessToken").toString();
                            SharedPreferences sp = getApplicationContext().getSharedPreferences("Login", MODE_PRIVATE);
                            SharedPreferences.Editor Ed = sp.edit();
                            Ed.putString("accessToken", token);
                            Ed.putString("validDate", getNewValidDate());
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
                            SharedPreferences sp = getApplicationContext().getSharedPreferences("UserData", MODE_PRIVATE);
                            params.put("email", sp.getString("email", null));
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
        SharedPreferences sp = getApplicationContext().getSharedPreferences("Login", MODE_PRIVATE);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putBoolean("stayLogged", false);
        Ed.commit();
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        this.finish();
    }

    private String getNewValidDate() {
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar c = Calendar.getInstance();
        String today = sdf.format(c.getTime());
        String validDate = today;
        try {
            c.setTime(sdf.parse(today));
            c.add(Calendar.DATE, DAYS_FOR_TOKEN_TO_EXPIRE);
            validDate = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return validDate;
    }

    private boolean isTokenDateValid(String validString) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        boolean isTokenValid = false;
        try {
            Date validDate = sdf.parse(validString);
            Log.println(ASSERT,"TOKEN DATE", validString);
            isTokenValid = !(new Date().after(validDate) || new Date().equals(validDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.println(ASSERT, "TOKEN VALID -", Boolean.toString(isTokenValid));
        return isTokenValid;
    }

    private void getProductsByBarcode(String barcode,boolean show) {
        //TODO check in pantry and in products? Eventually show a pop up saying so
        if (checkConnectionAvailability()) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest showProductsByCodeRequest =
                    new StringRequest(Request.Method.GET, LIST_PRODUCTS_URL + barcode,
                    response -> {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            JSONArray productsList = responseObject.getJSONArray("products");
                            getApplicationContext()
                                    .getSharedPreferences("Utils", MODE_PRIVATE)
                                    .edit()
                                    .putString("sessionToken", responseObject.getString("token"))
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
                            .getSharedPreferences("Login", MODE_PRIVATE)
                            .getString("accessToken", null));
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
        ListProductsFragment listProductsFragment = new ListProductsFragment();
        listProductsFragment.setArguments(bundle);
        getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.activity_main, listProductsFragment)
            .addToBackStack(null)
            .commit();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.CAMERA},
            CAMERA_REQUEST_CODE
        );
    }

    public boolean checkConnectionAvailability() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void enableCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_ACTIVITY);
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
        populatePantryList();
        pantryAdapter = new RVAdapterPantry(pantryProducts, this);
        pantryRecyclerView.setAdapter(pantryAdapter);
    }

    private void populatePantryList() {
        pantryProducts = new ArrayList<>();
        Cursor cursor = database.getPantryProducts();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            pantryProducts.add(new ProductPantryItem(
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ID)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ICON)),
                cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_IS_FAVORITE)),
                cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY))
            ));
            cursor.moveToNext();
        }
        cursor.close();
    }

    public void voteProduct(int rating, String productID, String barcode) {
        if (checkConnectionAvailability()) {
            JSONObject params = new JSONObject();
            try {
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                params.put("token", getApplicationContext()
                        .getSharedPreferences("Utils", MODE_PRIVATE)
                        .getString("sessionToken", null)
                );
                params.put("rating", rating);
                params.put("productId", productID);
                final String requestBody = params.toString();

                PreviewProductFragment preview = (PreviewProductFragment) getSupportFragmentManager()
                                .findFragmentByTag("previewFragment");
                StringRequest voteProductRequest = new StringRequest(Request.Method.POST, VOTE_PRODUCT_URL,
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
                                .getSharedPreferences("Login", MODE_PRIVATE)
                                .getString("accessToken", null));
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