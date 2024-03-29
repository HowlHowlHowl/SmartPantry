package com.example.smartpantry;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FragmentShowRecipe extends Fragment {
    private TextView recipeName, recipeProcedure;
    ListView ingredientsList;
    String rec_id, name, procedure, ingredientsString, type, notes;
    ImageView img;
    DBHelper db;
    private JSONArray ingredients;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe, container, false);
        recipeName = view.findViewById(R.id.recipeName);
        recipeProcedure = view.findViewById(R.id.recipeProcedure);
        ingredientsList = view.findViewById(R.id.ingredientsList);
        img = view.findViewById(R.id.typeImg);

        TextView ratingVal = view.findViewById(R.id.ratingVal);
        SeekBar ratingBar = view.findViewById(R.id.ratingRecipe);
        Button rateBtn = view.findViewById(R.id.rateButton);

        name = this.getArguments().getString("name");
        procedure = this.getArguments().getString("procedure");
        ingredientsString = this.getArguments().getString("ingredients");
        type = this.getArguments().getString("type");
        notes = this.getArguments().getString("notes");

        try {
            ingredients = new JSONArray(ingredientsString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ratingBar.setMax(ingredients.length());


        view.findViewById(R.id.fragmentRecipe).setOnClickListener(v->{
            closeFragment();
        });
        view.findViewById(R.id.bgPopUp).setOnClickListener(v->{});
        db = new DBHelper(getContext());

        rateBtn.setOnClickListener(v->{
            int rating = ratingBar.getProgress();
            int percentageRating = (rating * 100)/ingredientsList.getCount();
            float expected = this.getArguments().getInt("expected");
            ((ActivityRecipes)getActivity()).sendRecipeRating(expected, percentageRating, rec_id);
            rateBtn.setEnabled(false);
            ratingBar.setEnabled(false);
            db.insertRating(rec_id, rating);
        });

        rec_id = this.getArguments().getString("rec_id");
        Integer existingRating = db.getRating(rec_id);
        Log.println(Log.ASSERT,"rating found", existingRating +"");
        if (existingRating != null) {
            ratingBar.setProgress(existingRating);
            ratingBar.setEnabled(false);
            rateBtn.setEnabled(false);
            ratingVal.setText(existingRating+"/"+ratingBar.getMax());
        } else {
            ratingVal.setText("0/"+ratingBar.getMax());
        }

        ratingBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                ratingVal.setText(i+"/"+ratingBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Log.println(Log.ASSERT, "RECIPE FRAGMENT", "CREATED");
        return view;
    }
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recipeName.setText(name);
        recipeProcedure.setText(procedure);
        if(notes!=null && !notes.equals("-")) {
            recipeProcedure.append(view.getContext().getString(R.string.recipeNotes, notes));
        }
        //Load and show icon
        try {
            AssetManager assetManager = view.getContext().getAssets();
            InputStream ims = assetManager.open(Global.RECIPES_PATH + type + ".png");
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            img.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Ingredient> recipeIngredients = new ArrayList<>();
        if(ingredients!=null && ingredients.length()>0){
            for(int i=0; i<ingredients.length(); i++) {
                try {
                    JSONObject item = ingredients.getJSONObject(i);
                    Ingredient ingredient = new Ingredient(
                            item.getString("name"),
                            item.getString("quantity"),
                            item.getString("ingredient_info"),
                            item.getInt("avail")
                    );
                    recipeIngredients.add(ingredient);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            AdapterIngredients adapter = new AdapterIngredients(recipeIngredients, getContext());
            ingredientsList.setAdapter(adapter);
        }
    }

    public void closeFragment() {
        db.close();
        FragmentManager fm = getActivity()
                .getSupportFragmentManager();
        fm.popBackStack();
    }
}
