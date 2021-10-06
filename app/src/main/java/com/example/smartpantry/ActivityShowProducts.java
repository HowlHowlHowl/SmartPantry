package com.example.smartpantry;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityShowProducts extends AppCompatActivity
        implements AdapterProductsList.onCardEvents, FragmentIconPicker.onIconChosen,
        FragmentFilters.onApplyFilters {
    private RecyclerView productsRecyclerView;
    private AdapterProductsList productsAdapter;
    private List<Product> productsList;
    private String productsOrder, productsFlow;
    private boolean resultIsSet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_products);
        findViewById(R.id.backBtn).setOnClickListener(v->{
            finish();
        });
        ImageButton filterButton = findViewById(R.id.imageFilterButton);
        filterButton.setOnClickListener(v->{
            FragmentFilters fragmentFilters = new FragmentFilters();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_all_products, fragmentFilters)
                    .addToBackStack(null)
                    .commit();
        });
        setProductsRecyclerView();
        setSearchBox();
    }

    private void setProductsRecyclerView() {
        productsRecyclerView = findViewById(R.id.productsRecycler);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setItemAnimator(null);
        productsList = new ArrayList<>();

        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE);
        productsOrder = sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE);
        productsFlow = sp.getString(Global.FLOW, Global.DESC_ORDER);
        populateProductsList(productsOrder, productsFlow);

        productsAdapter = new AdapterProductsList(productsList, this);
        productsRecyclerView.setAdapter(productsAdapter);
    }

    private void populateProductsList(String order, String flow ) {
        productsList.clear();
        DBHelper database = new DBHelper(getApplicationContext());
        Cursor cursor = database.getAllProducts(order, flow);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            productsList.add(new Product(
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ID)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ICON)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_IS_FAVORITE)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_IN_PANTRY))
            ));
            cursor.moveToNext();
        }
        cursor.close();
    }

    private void setSearchBox() {
        SearchView searchInProductsField = findViewById(R.id.searchProdField);
        SearchManager searchManager =  (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchInProductsField.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchInProductsField.setOnFocusChangeListener((view, hasFocus) -> {
            if(!hasFocus){
                //Clear Search Box
                searchInProductsField.setQuery("", false);

                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) ActivityShowProducts.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityShowProducts.this.getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        });

        //When the user clicks on the search-box the products list is refreshed
        //this way the positions match


        searchInProductsField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                ListView resultsListView = findViewById(R.id.searchResultsList);
                List<SuggestionItem> matchingProducts = new ArrayList<>();
                for(int i = 0; i < productsList.size(); i++ ) {
                    Product item = productsList.get(i);
                    String itemName = item.name;
                    if(!query.isEmpty() &&
                            itemName.toLowerCase().contains(query.toLowerCase())) {
                        matchingProducts.add(new SuggestionItem(
                                item.name,
                                item.description,
                                item.icon,
                                i
                        ));
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
                    searchInProductsField.setQuery("", false);

                    //Hide keyboard
                    InputMethodManager imm = (InputMethodManager) ActivityShowProducts.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(ActivityShowProducts.this.getWindow().getDecorView().getWindowToken(), 0);
                    }

                    LinearLayoutManager llm = ((LinearLayoutManager) productsRecyclerView.getLayoutManager());

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

    private void scrollPantryToPosition(int position){
        LinearLayoutManager llm = (LinearLayoutManager)productsRecyclerView.getLayoutManager();
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
        productsRecyclerView.removeOnScrollListener(onScrollListener);
        productsRecyclerView.addOnScrollListener(onScrollListener);
        productsRecyclerView.postDelayed(()->{
            productsRecyclerView.smoothScrollToPosition(position);
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

        productsRecyclerView.getLayoutManager().findViewByPosition(position).setBackground(drawable);
        handler.postDelayed(() -> {
            drawable.start();
            // pantryRecyclerView.getLayoutManager().findViewByPosition(position).callOnClick();

        }, 100);
    }

    @Override
    public void cardClicked(int position, int expandedPosition, int previouslyExpandedPosition) {
        productsAdapter.notifyItemChanged(previouslyExpandedPosition);
        productsAdapter.notifyItemChanged(expandedPosition);

        productsRecyclerView.smoothScrollToPosition(position);
    }
    @Override
    public void productUpdated() {
        if (!resultIsSet) {
            resultIsSet = true;
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
        }
    }

    @Override
    public void deleteItem(int position) {
        productsList.remove(position);
        productsAdapter.notifyItemChanged(position);
        refreshProducts();
    }

    private void refreshProducts() {
        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER,MODE_PRIVATE);
        String order = sp.getString(Global.TEMP_ORDER, sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE));
        String flow = sp.getString(Global.TEMP_FLOW, sp.getString(Global.FLOW, Global.DESC_ORDER));
        populateProductsList(order, flow);
        productsAdapter.notifyDataSetChanged();
    }

    @Override
    public void applyFilters(String order, String flow) {
        productsOrder = order;
        productsFlow = flow;
        populateProductsList(productsOrder, productsFlow);
        productsAdapter.notifyDataSetChanged();
    }

    @Override
    public void iconSelected(String icon, int position) {
        DBHelper db  = new DBHelper(this);
        db.changeIcon(icon, productsList.get(position).id);
        db.close();
        productsList.get(position).icon = icon;
        productsAdapter.notifyItemChanged(position);
        productUpdated();
        Log.println(Log.ASSERT, "SELECTED ICON", icon + " for "+ productsList.get(position).name);
    }
}
