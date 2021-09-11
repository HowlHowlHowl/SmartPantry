package com.example.smartpantry;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RVAdapterPantry extends RecyclerView.Adapter<RVAdapterPantry.PantryItemViewHolder>{
    public static List<ProductPantryItem> pantryProducts;
    private  onFavoritePressed listener;
    private Context context;
    RVAdapterPantry(List<ProductPantryItem> pantryProducts, onFavoritePressed listener, Context context) {
        this.pantryProducts = pantryProducts;
        this.listener = listener;
        this.context = context;
    }

    public interface onFavoritePressed {
        void favoritePressed(boolean state, int id);
    }
    @NonNull
    @Override
    public PantryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_pantry_card, parent, false);
        return new PantryItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryItemViewHolder holder, int position) {
        holder.cv.setId(Integer.parseInt(pantryProducts.get(position).id));
        holder.name.setText(pantryProducts.get(position).name);
        holder.quantity.setText("x" + pantryProducts.get(position).quantity);
        holder.fav.setChecked(pantryProducts.get(position).is_favorite);
        holder.fav.setOnClickListener(v -> {
            listener.favoritePressed(holder.fav.isChecked(), holder.cv.getId());
        });

        if(!pantryProducts.get(position).expire_date.isEmpty()) {
            holder.expireDate.append(pantryProducts.get(position).expire_date);
        } else {
            holder.expireDate.setText("");
        }

        AssetManager assetsManager = context.getAssets();
        try {
            InputStream ims;
            ims = assetsManager.open(pantryProducts.get(position).icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            holder.icon.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return pantryProducts.size();
    }

    public class PantryItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name;
        TextView expireDate;
        TextView quantity;
        ImageView icon;
        ToggleButton fav;

        PantryItemViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.pantryCardItem);
            name = (TextView)itemView.findViewById(R.id.productName);
            expireDate = (TextView)itemView.findViewById(R.id.expireText);
            quantity  = (TextView)itemView.findViewById(R.id.productQuantity);
            icon = (ImageView)itemView.findViewById(R.id.iconView);
            fav = (ToggleButton) itemView.findViewById(R.id.favCheckbox);
        }
    }
}