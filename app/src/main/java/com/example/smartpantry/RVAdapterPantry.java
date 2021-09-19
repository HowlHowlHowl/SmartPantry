package com.example.smartpantry;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RVAdapterPantry extends RecyclerView.Adapter<RVAdapterPantry.PantryItemViewHolder>{
    public static List<ProductPantryItem> pantryProducts;
    private onCardClicked cardClickedListener;
    private int expandedItem = -1;
    private int previouslyExpandedItem = -1;
    RVAdapterPantry(List<ProductPantryItem> pantryProducts, onCardClicked cardClickedListener) {
        RVAdapterPantry.pantryProducts = pantryProducts;
        this.cardClickedListener = cardClickedListener;
    }
    public interface onCardClicked {
        void cardClicked(int height, int expandedItem, int previouslyExpandedItem);
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
    public void onBindViewHolder(@NonNull PantryItemViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.cv.setId(Integer.parseInt(pantryProducts.get(position).id));
        //isExpanded is true if the current item is expanded
        final boolean isExpanded = (position == expandedItem);
        holder.fullProduct.setVisibility(isExpanded ? VISIBLE : GONE);
        holder.expandableStateImage.setImageResource(isExpanded ? R.drawable.down_arrow : R.drawable.up_arrow);
        holder.cv.setActivated(isExpanded);
        if (isExpanded) {
            previouslyExpandedItem = position;
        }
        holder.cv.setOnClickListener(v-> {
            //Logic to keep just one of the cards expanded
            expandedItem = isExpanded ? -1 : position;
            //Delegate notify and scroll to the activity
            cardClickedListener.cardClicked(holder.cv.getHeight(), expandedItem, previouslyExpandedItem);
        });

        //If an expire date is associated with the product it is written in the expire date label
        String toDisplayDateLabel = (
                pantryProducts.get(position).expire_date.isEmpty() ?
                        "" :
                        holder.cv.getContext().getResources().getString(R.string.pantryItemCardExpire)
                                + pantryProducts.get(position).expire_date
        );
        holder.expireDate.setText(toDisplayDateLabel);

        //Write current expire date in expireDatefield
        if(!pantryProducts.get(position).expire_date.isEmpty()) {
            holder.expireDateField.setText(pantryProducts.get(position).expire_date);
        }

        //Date picker event
        holder.expireDateField.setOnClickListener(v -> {
            //Set date picker
            Calendar myCalendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            DatePickerDialog dpd = new DatePickerDialog(
                v.getContext(),
                R.style.MyDatePickerDialogTheme,
                (vv, year, month, day) -> {
                    myCalendar.set(year, month, day);
                    holder.expireDateField.setText(sdf.format(myCalendar.getTime()));
                },
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH));

            dpd.getDatePicker().setMinDate(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            if(!pantryProducts.get(position).expire_date.isEmpty()) {
                try {
                    //Set current expire date on date picker
                    Date date = sdf.parse(pantryProducts.get(position).expire_date);
                    Calendar currentExpireDate = Calendar.getInstance();
                    currentExpireDate.setTime(date);

                    dpd.updateDate(
                            currentExpireDate.get(Calendar.YEAR),
                            currentExpireDate.get(Calendar.MONTH),
                            currentExpireDate.get(Calendar.DAY_OF_MONTH)
                    );
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            dpd.show();
        });

        //Clear date field
        holder.clearDateButton.setOnClickListener(v->{
            holder.expireDateField.setText("");
        });

        //Change expire date event
        holder.changeDateButton.setOnClickListener(v->{
            pantryProducts.get(position).expire_date = holder.expireDateField.getText().toString();
            DBHelper db = new DBHelper(holder.cv.getContext());
            db.changeExpireDate(pantryProducts.get(position).id, pantryProducts.get(position).expire_date);
            db.close();
            notifyItemChanged(position);
        });

        //Set quantity value
        holder.quantity.setText("x" + pantryProducts.get(position).quantity);

        //Set quantity field value
        holder.quantityField.setText(String.valueOf(pantryProducts.get(position).quantity));

        //Change quantity event
        holder.changeQuantityButton.setOnClickListener(v->{
            //Hide keyboard
            InputMethodManager imm = (InputMethodManager) v.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            //Clear Focus
            holder.quantityField.clearFocus();
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            pantryProducts.get(position).quantity = Integer.parseInt(holder.quantityField.getText().toString());
            holder.quantity.setText("x" + pantryProducts.get(position).quantity);
            DBHelper db  = new DBHelper(holder.cv.getContext());
            db.changeQuantity(pantryProducts.get(position).id, pantryProducts.get(position).quantity);
            db.close();
        });

        //TODO: TEST ONCE PRODUCTS PAGE IS READY
        //Delete item from pantry event
        holder.deleteItemButton.setOnClickListener(v->{
            /*DBHelper db  = new DBHelper(holder.cv.getContext());
            db.deleteProductFromPantry(pantryProducts.get(position).id);
            db.close();
            */
        });

        //Set favorite value
        holder.fav.setChecked(pantryProducts.get(position).is_favorite);
        //Change Favorite event
        holder.fav.setOnClickListener(v -> {
            DBHelper db  = new DBHelper(holder.cv.getContext());
            db.setFavorite(holder.fav.isChecked(), pantryProducts.get(position).id);
            db.close();
        });

        //Load and show icon
        try {
            AssetManager assetsManager = holder.cv.getContext().getAssets();
            InputStream ims;
            ims = assetsManager.open(pantryProducts.get(position).icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            holder.icon.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set name value
        holder.name.setText(pantryProducts.get(position).name);

        //Set description value and listener to ignore touch
        holder.description.setText(pantryProducts.get(position).description);
        holder.description.setOnClickListener(v->{});
    }

    @Override
    public int getItemCount() {
        return pantryProducts.size();
    }

    public class PantryItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name, description, expireDate, quantity;
        ImageView icon, expandableStateImage;
        ToggleButton fav;
        ImageButton clearDateButton;
        Button changeDateButton, changeQuantityButton, deleteItemButton;
        ConstraintLayout fullProduct;
        EditText expireDateField, quantityField;

        PantryItemViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.pantryCardItem);
            name = itemView.findViewById(R.id.productName);
            description = itemView.findViewById(R.id.productCardDescription);
            expireDate = itemView.findViewById(R.id.expireText);
            quantity  = itemView.findViewById(R.id.productQuantity);
            icon = itemView.findViewById(R.id.iconView);
            expandableStateImage = itemView.findViewById(R.id.expandableStateImageView);
            fav = itemView.findViewById(R.id.favCheckbox);
            fullProduct = itemView.findViewById(R.id.fullDetails);
            clearDateButton = itemView.findViewById(R.id.cancelDateButton);
            changeDateButton = itemView.findViewById(R.id.confirmChangeDate);
            changeQuantityButton =itemView.findViewById(R.id.changeQuantityButton);
            deleteItemButton = itemView.findViewById(R.id.removeItemButton);
            expireDateField = itemView.findViewById(R.id.productExpireDateField);
            quantityField  = itemView.findViewById(R.id.changeQuantityField);
        }
    }
}