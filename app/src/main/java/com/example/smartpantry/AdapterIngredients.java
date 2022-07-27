package com.example.smartpantry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class AdapterIngredients extends ArrayAdapter<Ingredient> {

    public AdapterIngredients(ArrayList<Ingredient> list, Context context) {
        super(context, R.layout.ingredient_item, list);

    }
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Ingredient ingredient = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        IngredientItemViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {
            viewHolder = new IngredientItemViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.ingredient_item, parent, false);
            viewHolder.name =  convertView.findViewById(R.id.name);
            viewHolder.quantity = convertView.findViewById(R.id.quantity);
            viewHolder.info = convertView.findViewById(R.id.info);
            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (IngredientItemViewHolder) convertView.getTag();
            result=convertView;
        }
        viewHolder.name.setText(ingredient.name);

        if(!ingredient.quantity.equals("null")) {
            viewHolder.quantity.setText(ingredient.quantity);
        } else {
            viewHolder.quantity.setText("-");
        }

        if(!ingredient.info.equals("null")) {
            viewHolder.info.setText(ingredient.info);
        } else {
            viewHolder.info.setVisibility(View.GONE);
        }

        if(ingredient.avail) {
            viewHolder.name.setTextColor(
                    convertView.getContext().getResources().getColor(R.color.high_green)
            );
        } else {
            viewHolder.name.setTextColor(
                    convertView.getContext().getResources().getColor(R.color.black)
            );
        }
        // Return the completed view to render on screen
        return convertView;
    }
    public static class IngredientItemViewHolder {
        TextView name, quantity, info;

        IngredientItemViewHolder() {
            TextView name, quantity, info;
        }
    }
}
