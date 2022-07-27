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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityShoppingList extends AppCompatActivity implements FragmentFilters.onApplyFilters {
    private static List<ProductShopping> shoppingList;
    private RecyclerView shoppingRecyclerView;
    private String productsOrder, productsFlow;
    private AdapterShoppingList shoppingAdapter;
    private ExecutorService threadPool;
    private boolean resultIsSet = false;
    private DBHelper database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //fixed size thread pool
        threadPool = Executors.newFixedThreadPool(6);

        //open db
        database = new DBHelper(getApplicationContext());

        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_shopping_list);

        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        findViewById(R.id.backBtnLayout).setOnClickListener(v-> finish());
        findViewById(R.id.backBtn).setOnClickListener(v-> finish());

        findViewById(R.id.updateShoppingListBtn).setOnClickListener(view -> updateShoppingList());
        setShoppingListRecycler();
        setSearchBox();

        ImageButton filterButton = findViewById(R.id.imageFilterButton);
        filterButton.setOnClickListener(v->{
            Bundle bundle = new Bundle();
            bundle.putInt("activity", Global.SHOPPING_ACTIVITY);
            FragmentFilters fragmentFilters = new FragmentFilters();
            fragmentFilters.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_shopping_list, fragmentFilters, Global.FRAG_FILTERS)
                    .addToBackStack(Global.FRAG_FILTERS)
                    .commit();
        });

        //Swipe
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //Remove swiped item from list and notify the RecyclerView
                int position = viewHolder.getAdapterPosition();

                database.updateShoppingProduct(
                        shoppingList.get(position).id,
                        0
                );
                shoppingList.get(position).to_buy_qnt=0;
                updateShoppingList();

                shoppingList.get(position).updateValue=true;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(shoppingRecyclerView);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    public void updateShoppingList(){
        threadPool.execute(()-> {
            for (int i=0; i<shoppingList.size(); i++) {
                //2nd param. is the quantity to add to the pantry, if x has been selected for the prod. 0 items are added
                ProductShopping product = shoppingList.get(i);
                if(product.updateValue != null) {
                    database.updateShoppingProduct(
                            product.id,
                            product.updateValue ? product.to_buy_qnt + product.quantity : product.quantity
                    );
                }
            }
            SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE);
            productsOrder = sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE);
            productsFlow = sp.getString(Global.FLOW, Global.DESC_ORDER);

            populateShoppingList(productsOrder, productsFlow);
            runOnUiThread(shoppingAdapter::notifyDataSetChanged);
        });

        //Set the result of the activity to update the pantry list
        if (!resultIsSet) {
            resultIsSet = true;
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
        }
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
                InputMethodManager imm = (InputMethodManager) ActivityShoppingList.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityShoppingList.this.getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        });

        //When the user clicks on the search-box the products list is refreshed
        //this way the positions match
        searchInProductsField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) ActivityShoppingList.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityShoppingList.this.getWindow().getDecorView().getWindowToken(), 0);
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                ListView resultsListView = findViewById(R.id.searchResultsList);
                List<SuggestionItem> matchingProducts = new ArrayList<>();
                for(int i = 0; i < shoppingList.size(); i++ ) {
                    ProductShopping item = shoppingList.get(i);
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
                    InputMethodManager imm = (InputMethodManager) ActivityShoppingList.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(ActivityShoppingList.this.getWindow().getDecorView().getWindowToken(), 0);
                    }

                    LinearLayoutManager llm = ((LinearLayoutManager) shoppingRecyclerView.getLayoutManager());

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
        LinearLayoutManager llm = (LinearLayoutManager)shoppingRecyclerView.getLayoutManager();
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
        shoppingRecyclerView.removeOnScrollListener(onScrollListener);
        shoppingRecyclerView.addOnScrollListener(onScrollListener);
        shoppingRecyclerView.postDelayed(()-> shoppingRecyclerView.smoothScrollToPosition(position), 50);
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

        shoppingRecyclerView.getLayoutManager().findViewByPosition(position).setBackground(drawable);
        handler.postDelayed(drawable::start, 100);
    }

    private void setShoppingListRecycler() {
        shoppingRecyclerView = findViewById(R.id.shoppingRecycler);
        shoppingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shoppingRecyclerView.setItemAnimator(null);
        shoppingRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), LinearLayout.VERTICAL));
        shoppingList = new ArrayList<>();

        SharedPreferences sp = getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE);
        productsOrder = sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE);
        productsFlow = sp.getString(Global.FLOW, Global.DESC_ORDER);
        threadPool.execute(()->{
            populateShoppingList(productsOrder, productsFlow);
            runOnUiThread(()->{
                shoppingAdapter = new AdapterShoppingList(shoppingList);
                shoppingRecyclerView.setAdapter(shoppingAdapter);
            });
        });
    }
    private void populateShoppingList(String order, String flow) {
        shoppingList.clear();
        Cursor cursor = database.getShoppingListProducts(order, flow);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            shoppingList.add(new ProductShopping(
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ID)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ICON)),
                    cursor.getLong(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_TO_BUY_QUANTITY)),
                    cursor.getLong(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY))
            ));
            cursor.moveToNext();
        }
        cursor.close();
    }

    @Override
    public void applyFilters(boolean notInPantry, String order, String flow) {
        threadPool.execute(()->{
            populateShoppingList(order, flow);
            runOnUiThread(shoppingAdapter::notifyDataSetChanged);
        });
    }

}
