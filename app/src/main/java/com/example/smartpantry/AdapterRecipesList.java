package com.example.smartpantry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterRecipesList extends RecyclerView.Adapter<AdapterRecipesList.RecipeItemViewHolder>{
    private static List<Recipe> recipesList;
    public AdapterRecipesList(List<Recipe> recipesList) {
        AdapterRecipesList.recipesList = recipesList;
    }

    @NonNull
    @Override
    public AdapterRecipesList.RecipeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_recipe_card, parent, false);
        return new RecipeItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeItemViewHolder holder, int position) {

        holder.cv.setOnClickListener(v->{
            Bundle bundle = new Bundle();
            bundle.putString("ingredients", recipesList.get(position).ingredients.toString());
            bundle.putString("name", recipesList.get(position).name);
            bundle.putString("procedure", recipesList.get(position).procedure);
            bundle.putString("type", recipesList.get(position).type);
            bundle.putString("notes", recipesList.get(position).notes);
            bundle.putString("rec_id", recipesList.get(position).id);
            float expected_score = (float) (((Double.valueOf(recipesList.get(position).score*100).intValue())*5)/100.0);
            bundle.putFloat("expected", expected_score);
            FragmentShowRecipe fragmentShowRecipe = new FragmentShowRecipe();
            fragmentShowRecipe.setArguments(bundle);
            ((ActivityRecipes)holder.cv.getContext()).getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_recipes, fragmentShowRecipe, Global.FRAG_RECIPE)
                    .addToBackStack(Global.FRAG_RECIPE)
                    .commit();
        });
        holder.name.setText(recipesList.get(position).name);
        holder.mainIngredient.setText(
                holder.cv.getContext().getString(R.string.mainIngredient, recipesList.get(position).main_ingredient)
        );
        holder.ingredientCount.setMax(1000);
        holder.ingredientCountLabel.setText(
                holder.cv.getContext().getString(R.string.ingredientsCount, Double.valueOf(recipesList.get(position).score*100).intValue() )
        );

        holder.ingredientCount.setProgress(Double.valueOf(recipesList.get(position).score*1000).intValue());
        holder.portions.setText(holder.cv.getContext().getString(R.string.portions, recipesList.get(position).portions));
        holder.type.setText(recipesList.get(position).type);
    }

    @Override
    public int getItemCount() {
        return recipesList.size();
    }

    public static class RecipeItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name, mainIngredient, ingredientCountLabel, portions, type;
        ProgressBar ingredientCount;

        RecipeItemViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.recipeCard);
            name = itemView.findViewById(R.id.recipeName);
            mainIngredient = itemView.findViewById(R.id.mainIngredient);
            portions = itemView.findViewById(R.id.portions);
            ingredientCountLabel = itemView.findViewById(R.id.ingredientCountLabel);
            ingredientCount = itemView.findViewById(R.id.ingredientCount);
            portions = itemView.findViewById(R.id.portions);
            type = itemView.findViewById(R.id.typeLabel);
        }
    }
}
