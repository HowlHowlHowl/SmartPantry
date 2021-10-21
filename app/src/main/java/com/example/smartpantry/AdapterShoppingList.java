package com.example.smartpantry;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        holder.removeProduct.setChecked(false);
        holder.removeLayout.setOnClickListener(v->{
            holder.removeProduct.performClick();
        });

        holder.removeProduct.setOnClickListener(v->{
            if (holder.removeProduct.isChecked()) {
                shoppingList.get(holder.getAdapterPosition()).updateValue = false;
                holder.confirmProduct.setChecked(false);
            } else {
                shoppingList.get(holder.getAdapterPosition()).updateValue = null;
            }
        });

        holder.confirmLayout.setOnClickListener(v->{
            holder.confirmProduct.performClick();
        });
        holder.confirmProduct.setOnClickListener(v->{
            if (holder.confirmProduct.isChecked()) {
                shoppingList.get(holder.getAdapterPosition()).updateValue = true;
                holder.removeProduct.setChecked(false);
            } else {
                shoppingList.get(holder.getAdapterPosition()).updateValue = null;
            }
        });

        //quantity field event
        setQuantityTextObserver(holder);

        //Set name value
        holder.name.setText(shoppingList.get(holder.getAdapterPosition()).name);
        //Set quantity
        holder.quantityField.setText(
                String.valueOf(shoppingList.get(holder.getAdapterPosition()).to_buy_qnt)
        );
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

    private void setQuantityTextObserver(AdapterShoppingList.ShoppingItemViewHolder holder) {
        holder.quantityField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void afterTextChanged(Editable s) {
                if(!holder.quantityField.getText().toString().isEmpty()) {
                    shoppingList.get(holder.getAdapterPosition()).to_buy_qnt =
                        Long.parseLong(holder.quantityField.getText().toString());
                } else {
                    shoppingList.get(holder.getAdapterPosition()).to_buy_qnt = 1;
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }
    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    public static class ShoppingItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name;
        ImageView icon;
        LinearLayout removeLayout, confirmLayout;
        ImageButton clearDateButton;
        ToggleButton removeProduct, confirmProduct;
        EditText quantityField;

        ShoppingItemViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.shoppingCard);
            name = itemView.findViewById(R.id.shoppingProductName);
            icon = itemView.findViewById(R.id.shopIconView);
            clearDateButton = itemView.findViewById(R.id.cancelDateButton);
            removeProduct = itemView.findViewById(R.id.removeFromShopping);
            removeLayout = itemView.findViewById(R.id.removeFromShoppingLayout);
            confirmProduct = itemView.findViewById(R.id.confirmShopping);
            confirmLayout = itemView.findViewById(R.id.confirmShoppingLayout);
            quantityField  = itemView.findViewById(R.id.shopQuantityField);
        }
    }
}
