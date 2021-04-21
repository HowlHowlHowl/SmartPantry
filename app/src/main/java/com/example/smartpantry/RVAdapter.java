package com.example.smartpantry;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.PantryItemViewHolder>{
    public List<ProductPantryItem> pantryProducts;
    RVAdapter(List<ProductPantryItem> pantryProducts) {
        this.pantryProducts = pantryProducts;
    }

    @NonNull
    @Override
    public PantryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_pantry_card, parent, false);
        return new PantryItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryItemViewHolder holder, int position) {
        holder.productName.setText(pantryProducts.get(position).name);
        holder.expireDate.setText(pantryProducts.get(position).expire_date);
    }

    @Override
    public int getItemCount() {
        return pantryProducts.size();
    }

    public class PantryItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView productName;
        TextView expireDate;

        PantryItemViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cardItem);
            productName = (TextView)itemView.findViewById(R.id.productText);
            expireDate = (TextView)itemView.findViewById(R.id.expireText);
        }
    }

}