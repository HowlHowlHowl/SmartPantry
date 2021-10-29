package com.example.smartpantry;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdapterAlreadySavedList extends RecyclerView.Adapter<AdapterAlreadySavedList.ProductAlreadySavedItem>{
    public static List<ProductAlreadySaved> productsList;
    private final onCardClicked selectionListener;
    public interface onCardClicked {
        void cardClicked(int position);
    }

    public AdapterAlreadySavedList(List<ProductAlreadySaved> alreadySavedList, onCardClicked listener) {
        productsList = alreadySavedList;
        selectionListener = listener;
    }

    @NonNull
    @Override
    public ProductAlreadySavedItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_already_saved_card, parent, false);
        return new ProductAlreadySavedItem(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAlreadySavedItem holder, int position) {
        //Set name value
        holder.name.setText(productsList.get(holder.getAdapterPosition()).name);

        holder.cv.setOnClickListener(v-> selectionListener.cardClicked(productsList.get(holder.getAdapterPosition()).position));
        //Load and show icon
        try {
            AssetManager assetManager = holder.cv.getContext().getAssets();
            InputStream ims = assetManager.open(productsList.get(holder.getAdapterPosition()).icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            holder.icon.setImageBitmap(bitmap);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        //Set quantity value
        holder.quantity.setText("x" + productsList.get(holder.getAdapterPosition()).quantity);

        //Set expire date
        String expireDate = (productsList.get(holder.getAdapterPosition()).expire_date == null ? "" :
                productsList.get(holder.getAdapterPosition()).expire_date);

        //This label is displayed  if the expire date isn't specified but the product is in pantry
        String toDisplayDateLabel = holder.cv.getContext().getString(R.string.productInPantry);

        //Write current expire date in expireDateField
        if(!expireDate.isEmpty()) {
            //Show date in localFormat from fixed db format
            DateFormat originalFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
            DateFormat targetFormat = DateFormat.getDateInstance();
            String formattedDate = Global.changeDateFormat(expireDate, originalFormat, targetFormat);
            //If an expire date is associated with the product it is written in the expire date label
            toDisplayDateLabel = (holder.cv.getContext().getResources().getString(R.string.pantryItemCardExpire) + formattedDate);

            //Set expire date color to red if expired
            if (!Global.isDateBeforeToday(expireDate)) {
                holder.infoLabel.setTextColor(
                        ContextCompat.getColor(holder.cv.getContext(),
                                R.color.design_default_color_error)
                );
            }
        } else {
            //Else, if there isn't an expire date, if the product
            //isn't in pantry the label content and color is changed
            if (!productsList.get(holder.getAdapterPosition()).in_pantry) {
                toDisplayDateLabel = holder.cv.getContext().getString(R.string.productNotInPantry);
                holder.infoLabel.setTextColor(
                        ContextCompat.getColor(holder.cv.getContext(),
                                R.color.app_color)
                );
            }
        }
        holder.infoLabel.setText(toDisplayDateLabel);
    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }
    public static class ProductAlreadySavedItem extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name, infoLabel, quantity;
        ImageView icon;

        ProductAlreadySavedItem(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.pantryCardItem);
            name = itemView.findViewById(R.id.productName);
            infoLabel = itemView.findViewById(R.id.infoLabel);
            quantity  = itemView.findViewById(R.id.productQuantity);
            icon = itemView.findViewById(R.id.iconView);
        }
    }
}
