package com.example.smartpantry;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public TextView shoppingLabel;
    public TextView recipesLabel;
    public TextView statisticsLabel;
    public TextView prodLabel;
    public EditText searchProd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        shoppingLabel = findViewById(R.id.listLabel);
        recipesLabel  = findViewById(R.id.recipesLabel);
        statisticsLabel = findViewById(R.id.statisticsLabel);
        prodLabel = findViewById(R.id.productsLabel);
        searchProd = findViewById(R.id.searchProdField);
        setLanguage();
    }
    public void setLanguage() {
        shoppingLabel.setText(R.string.shoppingText);
        recipesLabel.setText(R.string.recipesText);
        statisticsLabel.setText(R.string.statisticsText);
        prodLabel.setText(R.string.prodText);
        searchProd.setHint(R.string.searchText);
    }
}