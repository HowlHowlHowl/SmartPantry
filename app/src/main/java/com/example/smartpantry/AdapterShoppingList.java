package com.example.smartpantry;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AdapterShoppingList extends RecyclerView.Adapter<AdapterShoppingList.ShoppingItemViewHolder>{
    private static List<ProductShopping> shoppingList;
    public AdapterShoppingList(List<ProductShopping> shoppingList) {
        AdapterShoppingList.shoppingList = shoppingList;
    }

    @NonNull
    @Override
    public AdapterShoppingList.ShoppingItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_shopping_card, parent, false);
        return new ShoppingItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingItemViewHolder holder, int position) {
        holder.confirmProduct.setChecked(false);
        holder.confirmLayout.setOnClickListener(v-> holder.confirmProduct.performClick());
        holder.confirmProduct.setOnClickListener(v->{
            if (holder.confirmProduct.isChecked()) {
                shoppingList.get(holder.getAdapterPosition()).updateValue = true;
            } else {
                shoppingList.get(holder.getAdapterPosition()).updateValue = null;
            }
        });

        //Set name value
        holder.name.setText(shoppingList.get(holder.getAdapterPosition()).name);

        //Set quantity field
        holder.quantityField.setMinValue(1);
        holder.quantityField.setMaxValue(99);
        holder.quantityField.setValue((int) shoppingList.get(holder.getAdapterPosition()).to_buy_qnt);
        holder.quantityField.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        holder.quantityField.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int prev_val, int new_val) {
                shoppingList.get(holder.getAdapterPosition()).to_buy_qnt = holder.quantityField.getValue();
            }
        });

        //Load and show icon
        try {
            AssetManager assetManager = holder.cv.getContext().getAssets();
            InputStream ims = assetManager.open(shoppingList.get(holder.getAdapterPosition()).icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            holder.icon.setImageBitmap(bitmap);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    public static class ShoppingItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name;
        ImageView icon;
        LinearLayout confirmLayout;
        ImageButton clearDateButton;
        ToggleButton confirmProduct;
        NumberPicker quantityField;

        ShoppingItemViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.shoppingCard);
            name = itemView.findViewById(R.id.shoppingProductName);
            icon = itemView.findViewById(R.id.shopIconView);
            clearDateButton = itemView.findViewById(R.id.cancelDateButton);
            confirmProduct = itemView.findViewById(R.id.confirmShopping);
            confirmLayout = itemView.findViewById(R.id.confirmShoppingLayout);
            quantityField  = itemView.findViewById(R.id.shopQuantityField);
        }
    }
}
