package com.example.smartpantry;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityShowProducts extends AppCompatActivity
        implements AdapterProductsList.onCardEvents, FragmentIconPicker.onIconChosen,
        FragmentFilters.onApplyFilters {
    private RecyclerView productsRecyclerView;
    private AdapterProductsList productsAdapter;
    private List<ProductComplete> productsList;
    private DBHelper database;
    private ExecutorService threadPool;
    private String productsOrder, productsFlow;
    private boolean productsNotInPantry = false;
    private boolean resultIsSet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Open dp
        database = new DBHelper(getApplicationContext());

        //fixed size thread pool
        threadPool = Executors.newFixedThreadPool(6);

        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_show_products);

        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        findViewById(R.id.backBtnLayout).setOnClickListener(v-> finish());

        findViewById(R.id.backBtn).setOnClickListener(v-> finish());

        ImageButton filterButton = findViewById(R.id.imageFilterButton);
        filterButton.setOnClickListener(v->{
            FragmentFilters fragmentFilters = new FragmentFilters();
            Bundle bundle = new Bundle();
            bundle.putInt(
                    "activity",
                    Global.PRODUCTS_ACTIVITY
            );
            fragmentFilters.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_all_products, fragmentFilters, Global.FRAG_FILTERS)
                    .addToBackStack(Global.FRAG_FILTERS)
                    .commit();
        });
        if(getIntent().getBooleanExtra(Global.FAVORITES_INTENT_ACTION, false)) {
            SharedPreferences.Editor ed = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE).edit();
            ed.putString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE);
            ed.putString(Global.FLOW, Global.DESC_ORDER);
            ed.putBoolean(Global.NOT_IN_PANTRY, true);
            ed.apply();
        }
        setProductsRecyclerView();
        setSearchBox();
        if(getIntent().getExtras() != null) {
            productsRecyclerView.postDelayed(()->{
                int pos = getIntent().getExtras().getInt("position");
                LinearLayoutManager llm = (LinearLayoutManager)productsRecyclerView.getLayoutManager();
                //If the item is fully visible flash it else scroll to it and then flash it
                if( llm.findFirstCompletelyVisibleItemPosition() <= pos &&
                        llm.findLastCompletelyVisibleItemPosition() >= pos){
                    flashSearchedView(pos);
                } else {
                    scrollPantryToPosition(pos);
                }

            }, 50);
        }
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void setProductsRecyclerView() {
        productsRecyclerView = findViewById(R.id.productsRecycler);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setItemAnimator(null);
        productsList = new ArrayList<>();

        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE);

        productsOrder = sp.getString(Global.TEMP_ORDER, sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE));
        productsFlow = sp.getString(Global.TEMP_FLOW, sp.getString(Global.FLOW, Global.DESC_ORDER));

        productsNotInPantry = sp.getBoolean(Global.NOT_IN_PANTRY, false);
        threadPool.execute(()->{
            populateProductsList(productsNotInPantry, productsOrder, productsFlow);
            runOnUiThread(()->{
                productsAdapter = new AdapterProductsList(productsList, this, database);
                productsRecyclerView.setAdapter(productsAdapter);
            });
        });
    }

    private void populateProductsList(boolean notInPantry, String order, String flow ) {
        productsList.clear();
        Cursor cursor = database.getAllProducts(notInPantry, order, flow);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            productsList.add(new ProductComplete(
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ID)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ICON)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_IS_FAVORITE)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_TO_BUY_QUANTITY)),
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
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) ActivityShowProducts.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityShowProducts.this.getWindow().getDecorView().getWindowToken(), 0);
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                ListView resultsListView = findViewById(R.id.searchResultsList);
                List<SuggestionItem> matchingProducts = new ArrayList<>();
                for(int i = 0; i < productsList.size(); i++ ) {
                    ProductComplete item = productsList.get(i);
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
        productsRecyclerView.postDelayed(()-> productsRecyclerView.smoothScrollToPosition(position), 50);
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
        handler.postDelayed(drawable::start, 100);
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
        boolean notInPantry = sp.getBoolean(Global.NOT_IN_PANTRY, false);
        threadPool.execute(()->{
            populateProductsList(notInPantry, order, flow);
            runOnUiThread(productsAdapter::notifyDataSetChanged);
        });
    }

    @Override
    public void applyFilters(boolean notInPantry, String order, String flow) {
        productsOrder = order;
        productsFlow = flow;
        productsNotInPantry = notInPantry;
        threadPool.execute(()->{
            populateProductsList(productsNotInPantry, productsOrder, productsFlow);
            runOnUiThread(productsAdapter::notifyDataSetChanged);
        });
    }

    @Override
    public void iconSelected(String icon, int position) {
        database.changeIcon(icon, productsList.get(position).id);
        productsList.get(position).icon = icon;
        productsAdapter.notifyItemChanged(position);
        productUpdated();
        Log.println(Log.ASSERT, "SELECTED ICON", icon + " for "+ productsList.get(position).name);
    }
}
