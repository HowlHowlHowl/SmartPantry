package com.example.smartpantry;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static final int CAMERA_REQUEST_CODE = 1;
    static final int CAMERA_ACTIVITY = 101;
    private TextView pantryTitle;
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

        pantryTitle = findViewById(R.id.pantryTitle);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case CAMERA_ACTIVITY:
                if(resultCode == Activity.RESULT_OK){
                    String barcode = data.getStringExtra("barcode");
                    Log.println(Log.ASSERT, barcode, "RESULT");
                }
                break;
            default:
        }
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
        pantryTitle.setText(R.string.pantryTitle);
        prodLabel.setText(R.string.prodText);
        searchProd.setHint(R.string.searchText);

    }


}