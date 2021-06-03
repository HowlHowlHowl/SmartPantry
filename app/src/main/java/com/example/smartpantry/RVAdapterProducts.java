package com.example.smartpantry;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RVAdapterProducts extends RecyclerView.Adapter<RVAdapterProducts.ProductsItemViewHolder>{
    public List<ProductListItem> productsList;
    RVAdapterProducts(List<ProductListItem> products) {
        this.productsList = products;
    }

    @NonNull
    @Override
    public RVAdapterProducts.ProductsItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_products_card, parent, false);
        return new RVAdapterProducts.ProductsItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductsItemViewHolder holder, int position) {
        holder.name.setText(productsList.get(position).name);
        holder.id = productsList.get(position).id;
        holder.userId = productsList.get(position).userId;
        holder.description.setText(productsList.get(position).description);
        holder.createDate.append(productsList.get(position).createdAt);
        holder.updateDate.append(productsList.get(position).updatedAt);
        holder.barcode.append(productsList.get(position).barcode);

    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }

    public class ProductsItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name;
        TextView updateDate;
        TextView createDate;
        TextView description;
        TextView barcode;
        String userId;
        String id;
        ConstraintLayout card;

        ProductsItemViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.listCardItem);
            name = (TextView)itemView.findViewById(R.id.productName);
            description = (TextView)itemView.findViewById(R.id.productDescription);
            createDate = (TextView)itemView.findViewById(R.id.productCreated);
            updateDate = (TextView)itemView.findViewById(R.id.productUpdated);
            barcode = (TextView)itemView.findViewById(R.id.productBarcode);
            card = (ConstraintLayout) itemView.findViewById(R.id.clickableCard);
            card.setOnClickListener(v -> {
                Log.println(Log.ASSERT, "CLICK", "CLICKED " + id + " BY USER" + userId );
            });


        }
    }


}
