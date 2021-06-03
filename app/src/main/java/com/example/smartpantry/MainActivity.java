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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.util.Log.ASSERT;

public class MainActivity extends AppCompatActivity implements AddProductFragment.onProductAddedListener, RVAdapterPantry.onFavoritePressed {
    static final int CAMERA_REQUEST_CODE = 1;
    static final int CAMERA_ACTIVITY = 101;
    static final int STATUS_OK = 200;
    static final int SHOW_LOGIN = 201;
    static final int REQUEST_TOKEN = 202;
    private static final String LOGIN_URL = "https://lam21.modron.network/auth/login";
    private static final String LIST_PRODUCTS_URL = "https://lam21.modron.network/products?barcode=";
    private static final String ADD_PRODUCT_URL = "https://lam21.modron.network/products";
    private DBHelper database;
    private List<ProductPantryItem> pantryProducts;
    private RecyclerView pantryRecyclerView;
    private RVAdapterPantry pantryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new DBHelper(getApplicationContext());
        setContentView(R.layout.activity_main);
        ImageButton barcodeScan = findViewById(R.id.barcodeBtn);
        barcodeScan.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                enableCamera();
            } else {
                requestPermission();
            }
        });
        setPantryRecyclerView();
        handleLogin();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case CAMERA_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    String barcode = data.getStringExtra("barcode");
                    Log.println(ASSERT, "RESULT BARCODE", barcode);
                    getProductsByBarcode(barcode);
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

    @Override
    protected void onStop() {
        super.onStop();
        getApplicationContext()
                .getSharedPreferences("LOGIN", MODE_PRIVATE)
                .edit()
                .putBoolean("activeSession", false)
                .apply();
    }

    @Override
    public void productAdded(String barcode, String name, String description, String expire, String quantity, boolean test) {
        long id = addProductLocal(barcode, name, description, expire, quantity);
        pantryAdapter.pantryProducts.add(new ProductPantryItem(
                name, expire,
                Long.toString(id), 0,
                Integer.parseInt(quantity)));
        pantryAdapter.notifyItemInserted(pantryAdapter.getItemCount() );
        addProductRemote(barcode, name, description, test);
    }

    @Override
    public void favoritePressed(boolean state, int id) {
        database.setFavorite(state, id);
    }

    private long addProductLocal(String barcode, String name, String description, String expire, String quantity) {
        long code = database.insertNewProduct(barcode, name, description, expire, quantity);
        if (code != -1) {
            Toast.makeText(getApplicationContext(),
                "Inserimento effettuato",
                Toast.LENGTH_LONG);
        } else {
            //TODO: INSERIMENTO IN LOCALE FALLITO, PERCHE'?
            //      AVVISARE L'UTENTE E RIMEDIARE
            Log.wtf("ADD LOCAL", "ERROR");

        }
        return code;
    }

    private void addProductRemote(String barcode, String name, String description, boolean test) {
        if(checkConnectionAvailability()){
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

            StringRequest addProductRequest = new StringRequest(Request.Method.POST, ADD_PRODUCT_URL,
                response -> {
                },
                error -> {
                    if(error.networkResponse.statusCode == 0) {

                    }
                    error.printStackTrace();
                })
            {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params= new HashMap<String, String>();
                    params.put("token",
                            getApplicationContext()
                                    .getSharedPreferences("Utils", MODE_PRIVATE)
                                    .getString("sessionToken", null));
                    params.put("name", name);
                    params.put("description", description);
                    params.put("barcode", barcode);
                    params.put("test", test+"");
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Authorization", "Bearer "
                            + getApplicationContext()
                            .getSharedPreferences("Login", MODE_PRIVATE)
                            .getString("accessToken", null));
                    return headers;
                }

            };
            queue.add(addProductRequest);
        }else{
            //TODO: NO NETWORK ENQUEUE
        }
    }
    private void handleLogin() {
        Log.println(ASSERT, "HANDLE LOGIN", "HANDLING");
        SharedPreferences sp = this.getSharedPreferences("Login", MODE_PRIVATE);
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
        //STAY LOGGED
        if(sp.getBoolean("stayLogged", false)) {
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
        //DON'T STAY LOGGED BUT JUST LOGGED
        else if(sp.getBoolean("activeSession", false)) {
            Log.println(ASSERT, "LOGIN", "OK - JUST LOGGED");
            //sp.edit().putBoolean("justLoggedIn", false).commit();
            return STATUS_OK;
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
                            Map<String, String> params = new HashMap<String, String>();
                            SharedPreferences sp = getApplicationContext().getSharedPreferences("Login", MODE_PRIVATE);

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
        Ed.putBoolean("activeSession", false);
        Ed.commit();
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        this.finish();
    }

    private String getNewValidDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar c = Calendar.getInstance();
        String today = sdf.format(c.getTime());
        String validDate = today;
        try {
            c.setTime(sdf.parse(today));
            c.add(Calendar.DATE, 6);
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
        return isTokenValid;
    }

    private void getProductsByBarcode(String barcode) {
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
                            showMatchProducts(barcode, productsList);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<String, String>();
                    SharedPreferences sp = getApplicationContext().getSharedPreferences("Login", MODE_PRIVATE);
                    headers.put("Authorization", "Bearer " + sp.getString("accessToken", null));
                    return headers;
                }
            };
            queue.add(showProductsByCodeRequest);
        } else {
            //TODO NO RICHIESTA PER MANCANZA DI CONNESSIONE
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
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
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
        pantryRecyclerView = (RecyclerView)findViewById(R.id.pantryRecycler);
        pantryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        populatePantryList();
        pantryAdapter = new RVAdapterPantry(pantryProducts, this);
        pantryRecyclerView.setAdapter(pantryAdapter);
    }

    private void populatePantryList() {
        pantryProducts = new ArrayList<>();
        Cursor cursor = database.getProducts();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            pantryProducts.add(new ProductPantryItem(
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)),
                cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ID)),
                cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_IS_FAVORITE)),
                cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY))
            ));
            cursor.moveToNext();
        }
        cursor.close();
    }
}