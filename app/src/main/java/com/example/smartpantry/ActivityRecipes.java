package com.example.smartpantry;

import static android.util.Log.ASSERT;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ActivityRecipes extends AppCompatActivity {
    private DBHelper database;
    private ExecutorService threadPool;
    private List<Recipe> recipesList;
    private AlertDialog progressAlert;
    private AdapterRecipesList recipesAdapter;
    private RecyclerView recipesRecyclerView;

    public static final String FAV = "fav", EXPIRE = "expire", ING = "ingredients";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //fixed size thread pool
        threadPool = Executors.newFixedThreadPool(2);

        //open db
        database = new DBHelper(getApplicationContext());

        setContentView(R.layout.activity_recipes);

        //Stop adjustment of views when keyboard pops up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        findViewById(R.id.backBtnRecipesLayout).setOnClickListener(v->finish());
        findViewById(R.id.backBtnRecipes).setOnClickListener(v-> finish());

        sortRecipesBy(ING);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void getRecipes(Cursor cursor, Comparator<Recipe> comparator) {
        threadPool.execute(()->{
            if (Global.checkConnectionAvailability(getApplicationContext())) {
                runOnUiThread(this::toggleProgressBar);
                StringBuilder ingredientsList = new StringBuilder();
                cursor.moveToFirst();
                if(cursor.getCount()>0) {
                    while (!cursor.isAfterLast()) {
                        ingredientsList.append(cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)));
                        if (!cursor.isLast()) {
                            ingredientsList.append("$$$");
                        }
                        cursor.moveToNext();
                    }
                    cursor.close();

                    RequestQueue queue = Volley.newRequestQueue(this);

                    StringRequest recipesRequest = new StringRequest(Request.Method.GET, Global.RECIPES_URL + ingredientsList,
                        response -> {
                            try {
                                JSONObject stringRecipesList = new JSONObject(response);
                                JSONArray keys = stringRecipesList.names();
                                recipesList = new ArrayList<>();
                                for (int i = 0; i < keys.length(); i++) {
                                    String key = keys.getString(i);
                                    String stringRecipe = stringRecipesList.getString(key);
                                    JSONObject recipeObj = new JSONObject(stringRecipe);
                                    JSONArray recipeIngredients = new JSONArray(recipeObj.getString("ing_list"));
                                    Recipe recipe = new Recipe(
                                            key,
                                            recipeObj.getString("name"),
                                            recipeObj.getString("main_ingredient"),
                                            recipeObj.getString("procedure"),
                                            recipeObj.getString("portions"),
                                            recipeObj.getString("type"),
                                            recipeObj.getString("notes"),
                                            Double.parseDouble(recipeObj.getString("score")),
                                            recipeIngredients
                                    );
                                    recipesList.add(recipe);
                                }
                                setRecipesListRecycler(comparator);
                                setSearchBox();

                            } catch (JSONException e) {
                                Log.println(ASSERT, "RECIPES", "EXCEPTION");
                                runOnUiThread(() -> {
                                    Toast.makeText(this, R.string.genericError, Toast.LENGTH_LONG).show();
                                    toggleProgressBar();
                                });
                                e.printStackTrace();
                            }
                            runOnUiThread(() -> {
                                toggleProgressBar();
                                ((TextView) findViewById(R.id.recipesCount)).setText(
                                        getString(R.string.recipesCount, (recipesList != null ? recipesList.size() : 0))
                                );
                            });
                        },
                        (error)-> {
                        runOnUiThread(()->{
                            toggleProgressBar();
                            ((TextView) findViewById(R.id.noRecipesText)).setText(
                                    getString( R.string.genericError)
                            );
                            ((TextView) findViewById(R.id.noRecipesText)).setVisibility(View.VISIBLE);
                        });

                        }

                    ) {
                        @Override
                        protected Map<String, String> getParams() {
                            return new HashMap<>();
                        }
                    };
                    //Timeout to 8s
                    recipesRequest.setRetryPolicy(new DefaultRetryPolicy(
                            8000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    queue.add(recipesRequest);
                } else {
                    runOnUiThread(() -> {
                        toggleProgressBar();
                        ((TextView)findViewById(R.id.noRecipesText)).setVisibility(View.VISIBLE);
                        Toast.makeText(this, getResources().getString(R.string.noProductsForRecipes), Toast.LENGTH_LONG).show();
                    });
                }
            } else {
                runOnUiThread(()->
                        Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    void sendRecipeRating(float rating, String rec_id) {
        threadPool.execute(()-> {
            if (Global.checkConnectionAvailability(getApplicationContext())) {
                runOnUiThread(this::toggleProgressBar);
                JSONObject ratingObj = new JSONObject();
                try {
                    ratingObj.put("rating", rating);
                    ratingObj.put("rec_id", Integer.valueOf(rec_id));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RequestQueue queue = Volley.newRequestQueue(this);
                JsonObjectRequest rateRecipeRequest = new JsonObjectRequest(Request.Method.POST, Global.POST_RECIPE_RATING +
                        getSharedPreferences(Global.USER_DATA, MODE_PRIVATE).getString(Global.ID, "-"), ratingObj,
                    response -> runOnUiThread(this::toggleProgressBar),
                    error -> runOnUiThread(()->{
                        toggleProgressBar();
                        ((TextView) findViewById(R.id.noRecipesText)).setText(
                                getString( R.string.genericError)
                        );
                    })
                ) {
                    @Override
                    protected Map<String, String> getParams() {
                        return new HashMap<>();
                    }
                };
                queue.add(rateRecipeRequest);
            } else {
                runOnUiThread(()->
                    Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void toggleProgressBar() {
        if(progressAlert==null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityRecipes.this, R.style.CustomAlertDialog);
            ProgressBar progressBar = new ProgressBar(getApplicationContext());
            progressBar.getIndeterminateDrawable()
                    .setColorFilter(
                            getColor(R.color.app_color),
                            PorterDuff.Mode.MULTIPLY
                    );
            FrameLayout frame = new FrameLayout(getApplicationContext());
            frame.setBackgroundColor(Color.TRANSPARENT);
            frame.addView(progressBar);
            frame.setPadding(70, 140, 70, 140);
            builder.setView(frame);
            progressAlert = builder.create();
            progressAlert.requestWindowFeature(Window.FEATURE_NO_TITLE);
            progressAlert.show();
            progressAlert.getWindow()
                    .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            progressAlert.setOnShowListener(arg0 -> {
                progressAlert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            });
        } else {
            progressAlert.dismiss();
            progressAlert=null;
        }
    }

    private void setRecipesListRecycler(Comparator<Recipe> comparator) {
        Collections.sort(recipesList, comparator);
        recipesRecyclerView = findViewById(R.id.recipesRecycler);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipesRecyclerView.setItemAnimator(null);
        recipesAdapter = new AdapterRecipesList(recipesList);
        recipesRecyclerView.setAdapter(recipesAdapter);
    }

    private void sortRecipesBy(String order) {
        //FIXME Per niente completo, by fav non calcola avail i prodotti non fav || per expire idem + non ordina per scadenza
        Cursor cursor;
        Comparator<Recipe> comparator;
        switch (order) {
            case FAV:
                cursor = database.getFavoriteProductsOnly();
                comparator  = (a, b) -> Double.valueOf((b.score - a.score)*1000).intValue();
                break;
            case EXPIRE:
                cursor = database.getPantryProducts(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE, Global.DESC_ORDER);
                comparator  = (a, b) -> Double.valueOf((b.score - a.score)*1000).intValue();
                break;
            case ING:
            default:
                cursor = database.getPantryProducts(DBHelper.COLUMN_PRODUCT_NAME, Global.DESC_ORDER);
                comparator  = (a, b) -> Double.valueOf((b.score - a.score)*1000).intValue();
                break;
        }
        getRecipes(cursor, comparator);

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
                InputMethodManager imm = (InputMethodManager) ActivityRecipes.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityRecipes.this.getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        });

        //When the user clicks on the search-box the products list is refreshed
        //this way the positions match
        searchInProductsField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) ActivityRecipes.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(ActivityRecipes.this.getWindow().getDecorView().getWindowToken(), 0);
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                List<Recipe> matchingProducts = new ArrayList<>();
                for(int i = 0; i < recipesList.size(); i++ ) {
                    Recipe recipe = recipesList.get(i);
                    if(recipe.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))){
                        matchingProducts.add(recipe);
                    }
                }
                if (matchingProducts.size()<=0) {
                    ((TextView)findViewById(R.id.noRecipesText)).setVisibility(View.VISIBLE);
                } else {
                    ((TextView)findViewById(R.id.noRecipesText)).setVisibility(View.GONE);
                }
                recipesAdapter = new AdapterRecipesList(matchingProducts);
                recipesRecyclerView.setAdapter(recipesAdapter);

                return true;
            }
        });
    }
}
