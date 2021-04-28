package com.example.smartpantry;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static final int CAMERA_REQUEST_CODE = 1;
    static final int CAMERA_ACTIVITY = 101;
    static final int STATUS_OK = 200;
    static final int SHOW_LOGIN = 201;
    static final int REQUEST_TOKEN = 202;

    private final String loginURL = "https://lam21.modron.network/auth/login";

    private TextView shoppingLabel;
    private TextView recipesLabel;
    private TextView statisticsLabel;
    private TextView prodLabel;
    private EditText searchProd;
    private ImageButton barcodeScan;

    private List<ProductPantryItem> pantryProducts;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shoppingLabel = findViewById(R.id.listLabel);
        recipesLabel  = findViewById(R.id.recipesLabel);
        statisticsLabel = findViewById(R.id.statisticsLabel);
        prodLabel = findViewById(R.id.productsLabel);
        searchProd = findViewById(R.id.searchProdField);

        barcodeScan = findViewById(R.id.barcodeBtn);
        barcodeScan.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                enableCamera();
            } else {
                requestPermission();
            }
        });
        setRecyclerView();
        setLanguage();
        handleLogin();
    }

    private void handleLogin() {
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
        //USER LOGGED IN?
        //YES
        if (sp.getBoolean("userLogged", false)) {
            //STAY LOGGED
            if(sp.getBoolean("stayLogged", false)) {
                //TOKEN IS VALID
                if(isTokenValid(sp.getString("validDate", null))) {
                    Log.println(Log.ASSERT, "LOGIN", "OK");
                    return STATUS_OK;
                }
                //TOKEN IS NOT VALID
                else {
                    Log.println(Log.ASSERT, "LOGIN", "LOG-AUTO");
                    return REQUEST_TOKEN;
                }
            }
            //DON'T STAY LOGGED
            else {
                Log.println(Log.ASSERT, "LOGIN", "LOG1");
                return SHOW_LOGIN;
            }
        }
        //NO
        else {
            Log.println(Log.ASSERT, "LOGIN", "LOG2");
            return SHOW_LOGIN;
        }
    }

    private void updateToken() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest loginRequest = new StringRequest(Request.Method.POST, loginURL,
                    response -> {
                        try {
                            JSONObject credentials = new JSONObject(response);
                            String token = credentials.get("accessToken").toString();
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
        Ed.putBoolean("userLogged", false);
        Ed.commit();
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        this.finish();
    }


    private boolean isTokenValid(String validString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        boolean isTokenValid = false;
        try {
            Date validDate = sdf.parse(validString);
            isTokenValid = new Date().after(validDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return isTokenValid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case CAMERA_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    String barcode = data.getStringExtra("barcode");
                    Log.println(Log.ASSERT, "RESULT BARCODE", barcode);
                    getProductsByBarcode(barcode);
                }
                break;
            default:
        }
    }

    private void getProductsByBarcode(String barcode) {

    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE
        );
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


    private void setRecyclerView() {
        rv = (RecyclerView)findViewById(R.id.pantryRecycler);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        populatePantry();
        RVAdapter adapter = new RVAdapter(pantryProducts);
        rv.setAdapter(adapter);
    }

    private void populatePantry() {
        pantryProducts = new ArrayList<>();
        pantryProducts.add(new ProductPantryItem("Pane", "01/01/2022"));
        pantryProducts.add(new ProductPantryItem("Latte", "12/03/2020"));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setLanguage() {
        shoppingLabel.setText(R.string.shoppingText);
        recipesLabel.setText(R.string.recipesText);
        statisticsLabel.setText(R.string.statisticsText);
        prodLabel.setText(R.string.prodText);
        searchProd.setHint(R.string.searchText);

    }
}