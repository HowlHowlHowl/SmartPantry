package com.example.smartpantry;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RVAdapterPantry extends RecyclerView.Adapter<RVAdapterPantry.PantryItemViewHolder>{
    public static List<ProductPantryItem> pantryProducts;
    private  onFavoritePressed listener;
    RVAdapterPantry(List<ProductPantryItem> pantryProducts, onFavoritePressed listener) {
        this.pantryProducts = pantryProducts;
        this.listener = listener;
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
        holder.expireDate.setText(pantryProducts.get(position).expire_date);
        holder.quantity.setText("x" + pantryProducts.get(position).quantity);
        holder.fav.setChecked(pantryProducts.get(position).is_favorite);
        holder.fav.setOnClickListener(v -> {
            listener.favoritePressed(holder.fav.isChecked(), holder.cv.getId());
        });
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
        ToggleButton fav;

        PantryItemViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.pantryCardItem);
            name = (TextView)itemView.findViewById(R.id.productName);
            expireDate = (TextView)itemView.findViewById(R.id.expireText);
            quantity  = (TextView)itemView.findViewById(R.id.productQuantity);
            fav = (ToggleButton) itemView.findViewById(R.id.favCheckbox);
        }
    }
    public void addItemToPantry(){

    }
}