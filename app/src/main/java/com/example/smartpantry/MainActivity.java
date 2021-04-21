package com.example.smartpantry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public TextView pantryTitle;
    public TextView shoppingLabel;
    public TextView recipesLabel;
    public TextView statisticsLabel;
    public TextView prodLabel;
    public EditText searchProd;

    private List<ProductPantryItem> pantryProducts;
    public RecyclerView rv;

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

        rv = (RecyclerView)findViewById(R.id.pantryRecycler);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        populatePantry();
        RVAdapter adapter = new RVAdapter(pantryProducts);
        rv.setAdapter(adapter);
        setLanguage();
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