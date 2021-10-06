package com.example.smartpantry;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterProductsList extends RecyclerView.Adapter<AdapterProductsList.ProductItemViewHolder>{
    public static List<Product> productsList;
    private AdapterProductsList.onCardEvents cardEvents;
    private int expandedItem = -1;
    private int previouslyExpandedItem = -1;

    AdapterProductsList(List<Product> productsList, onCardEvents cardEvents) {
        AdapterProductsList.productsList = productsList;
        this.cardEvents = cardEvents;
    }

    public interface onCardEvents {
        void cardClicked(int position, int expandedItem, int previouslyExpandedItem);
        void productUpdated();
        void deleteItem(int position);
    }

    @NonNull
    @Override
    public ProductItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_products_card, parent, false);
        return new ProductItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterProductsList.ProductItemViewHolder holder, int position) {
        holder.cv.setId(Integer.parseInt(productsList.get(holder.getAdapterPosition()).id));
        //isExpanded is true if the current item is expanded
        final boolean isExpanded = (holder.getAdapterPosition() == expandedItem);
        holder.fullProduct.setVisibility(isExpanded ? VISIBLE : GONE);
        holder.expandableStateImage.setImageResource(isExpanded ? R.drawable.up_arrow : R.drawable.down_arrow);
        holder.cv.setActivated(isExpanded);
        if (isExpanded) {
            previouslyExpandedItem = holder.getAdapterPosition();
        }
        holder.cv.setOnClickListener(v->{
            //Logic to keep just one of the cards expanded
            expandedItem = isExpanded ? -1 : holder.getAdapterPosition();
            //Delegate notify and scroll to the activity
            cardEvents.cardClicked(
                    holder.getAdapterPosition(),
                    expandedItem,
                    previouslyExpandedItem
            );
        });

        //Set expire date
        String expireDate = (productsList.get(holder.getAdapterPosition()).expire_date == null ? "" :
                productsList.get(holder.getAdapterPosition()).expire_date);

        //Set favorite value
        holder.fav.setChecked(productsList.get(holder.getAdapterPosition()).is_favorite);
        //Change Favorite event
        holder.fav.setOnClickListener(v -> {
            DBHelper db  = new DBHelper(holder.cv.getContext());
            db.setFavorite(holder.fav.isChecked(), productsList.get(holder.getAdapterPosition()).id);
            db.close();
            productsList.get(holder.getAdapterPosition()).is_favorite = holder.fav.isChecked();
            notifyDataSetChanged();
            cardEvents.productUpdated();
        });

        //Date picker event
        holder.expireDateField.setOnClickListener(v -> {
            initializeDatePicker(v.getContext(), holder, expireDate);
        });

        //Clear date field
        holder.clearDateButton.setOnClickListener(v->{
            holder.expireDateField.setText("");
        });

        setQuantityTextObserver(holder);
        setDateTextObserver(holder);
        holder.updateItemButton.setOnClickListener(v -> {
            long updatedQuantity = Long.parseLong(holder.quantityField.getText().toString());
            boolean toAdd = updatedQuantity != 0;
            DateFormat originalFormat = DateFormat.getDateInstance();
            DateFormat targetFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
            String updatedExpireDate = Global.changeDateFormat(
                holder.expireDateField.getText().toString(),
                originalFormat,
                targetFormat
            );
            DBHelper db  = new DBHelper(holder.cv.getContext());
            db.updateProduct(
                productsList.get(holder.getAdapterPosition()).id,
                toAdd,
                updatedQuantity,
                toAdd && !updatedExpireDate.isEmpty() ? updatedExpireDate : null
            );
            db.close();
            productsList.get(holder.getAdapterPosition()).in_pantry = toAdd;
            productsList.get(holder.getAdapterPosition()).expire_date = updatedExpireDate;
            productsList.get(holder.getAdapterPosition()).quantity = updatedQuantity;
            notifyItemChanged(holder.getAdapterPosition());
            cardEvents.productUpdated();
        });
        //Disable update button by default
        holder.updateItemButton.setEnabled(false);

        holder.deleteItemButton.setOnClickListener(v->{
            //Delete item from pantry event
            askToDeleteProduct(holder.getAdapterPosition(), holder.cv.getContext());
        });

        //Load and show icon
        try {
            AssetManager assetManager = holder.cv.getContext().getAssets();
            InputStream ims = assetManager.open(productsList.get(holder.getAdapterPosition()).icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            holder.icon.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Icon click event to change icon
        holder.icon.setOnClickListener(v->{
            Bundle bundle = new Bundle();
            bundle.putInt("position", holder.getAdapterPosition());

            FragmentIconPicker fragmentIconPicker = new FragmentIconPicker();
            fragmentIconPicker.setArguments(bundle);
            //iconPickerFragment.setArguments();
            ((AppCompatActivity)holder.cv.getContext()).getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_all_products, fragmentIconPicker)
                    .addToBackStack(null)
                    .commit();
        });

        //This label is displayed  if the expire date isn't specified but the product is in pantry
        String toDisplayDateLabel = holder.cv.getContext().getString(R.string.productInPantry);;

        //If in pantry without date color set to green
        holder.infoLabel.setTextColor(
                ContextCompat.getColor(holder.cv.getContext(),
                        R.color.black)
        );
        holder.expireDateField.setText("");
        //Write current expire date in expireDateField
        if(!expireDate.isEmpty()) {
            //Show date in localFormat from fixed db format
            DateFormat originalFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
            DateFormat targetFormat = DateFormat.getDateInstance();
            String formattedDate = Global.changeDateFormat(expireDate, originalFormat, targetFormat);
            holder.expireDateField.setText(formattedDate);
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

        //Set quantity value
        holder.quantity.setText("x" + productsList.get(holder.getAdapterPosition()).quantity);
        holder.quantityField.setText(
                String.valueOf(productsList.get(holder.getAdapterPosition()).quantity)
        );

        //Set name value
        holder.name.setText(productsList.get(holder.getAdapterPosition()).name);

        //Set description value and listener to ignore touch
        holder.description.setText(productsList.get(holder.getAdapterPosition()).description);
        holder.description.setOnClickListener(v->{});
    }

    private void askToDeleteProduct(int position, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(context.getResources().getString(R.string.warningText))
                .setMessage(context.getResources().getString(R.string.deleteProduct))
                .setPositiveButton(
                        context.getResources().getString(R.string.confirmBtnText),
                        (dialog, id) -> {
                            DBHelper db  = new DBHelper(context);
                            db.deleteProduct(productsList.get(position).id);
                            db.close();
                            //this line of code put -1 as the index of the expanded card so that no card appears expanded
                            expandedItem = -1;
                            cardEvents.deleteItem(position);
                            cardEvents.productUpdated();
                        })
                .setNegativeButton(
                        context.getResources().getString(R.string.cancelText),
                        (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.setOnShowListener(arg0 -> {
            alert.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.button_confirm));

            alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.app_color));
        });

        alert.show();
    }


    private void setQuantityTextObserver(ProductItemViewHolder holder) {
        holder.quantityField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void afterTextChanged(Editable s) {
                DateFormat originalFormat =  DateFormat.getDateInstance();
                DateFormat targetFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
                String formattedDate = Global.changeDateFormat(
                        holder.expireDateField.getText().toString(),
                        originalFormat,
                        targetFormat);
                String quantity = holder.quantityField.getText().toString();
                //If in the quantity field is written a quantity we explore the cases
                if(!quantity.isEmpty()) {
                    long quantityInt = Long.parseLong(quantity);
                    //If the quantity written is the same one memorized for the item
                    if (quantityInt == productsList.get(holder.getAdapterPosition()).quantity) {
                        //If the date is not changed nothing can be updated so the button is disabled
                        //Else the date has been changed and the button must be enabled
                        holder.updateItemButton.setEnabled(
                                (!formattedDate.equals(productsList.get(holder.getAdapterPosition()).expire_date) && !formattedDate.isEmpty())
                        );
                    }
                    //Else if the quantity has changed the update button must be enabled
                    else {
                        holder.updateItemButton.setEnabled(true);
                    }
                }
                //Else if nothing is written in the quantity field, which is a necessary value, the button mustn't be enabled
                else {
                    holder.updateItemButton.setEnabled(false);
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }
    private void setDateTextObserver(ProductItemViewHolder holder) {
        holder.expireDateField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void afterTextChanged(Editable s) {
                DateFormat originalFormat =  DateFormat.getDateInstance();
                DateFormat targetFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
                String formattedDate = Global.changeDateFormat(
                        holder.expireDateField.getText().toString(),
                        originalFormat,
                        targetFormat);
                String quantity = holder.quantityField.getText().toString();
                //If quantity isn't empty
                if(!quantity.isEmpty()) {
                    long quantityInt = Long.parseLong(quantity);
                    //If the date isn't changed
                    if(formattedDate.equals(productsList.get(holder.getAdapterPosition()).expire_date)) {
                        //If the quantity hasn't changed the update is disabled
                        //Else if the quantity has changed, the update is enabled
                        holder.updateItemButton.setEnabled(
                                quantityInt != productsList.get(holder.getAdapterPosition()).quantity
                        );
                    }
                    //Else the date has changed and quantity isn't empty
                    else {
                        //If the quantity hasn't changed and it's equal to 0 it's useless to update the item
                        //Else the quantity has changed or it's more than 0, in both cases the update must be enabled
                        holder.updateItemButton.setEnabled(quantityInt != 0);
                    }
                }
                //Else quantity is empty and the product cant be edited
                else {
                    holder.updateItemButton.setEnabled(false);
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    public void initializeDatePicker(Context context, ProductItemViewHolder holder, String expireDate){
        //Set date picker
        Calendar myCalendar = Calendar.getInstance();

        DateFormat df = DateFormat.getDateInstance();
        DatePickerDialog dpd = new DatePickerDialog(
                context,
                R.style.MyDatePickerDialogTheme,
                (vv, year, month, day) -> {
                    myCalendar.set(year, month, day);
                    holder.expireDateField.setText(df.format(myCalendar.getTime()));
                },
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH));

        dpd.getDatePicker().setMinDate(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        if(!expireDate.isEmpty()) {
            try {
                //Set current expire date on date picker
                Date date = df.parse(Global.changeDateFormat(
                        expireDate,
                        new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault()),
                        df));
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
    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }

    public class ProductItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name, description, infoLabel, quantity;
        ImageView icon, expandableStateImage;
        ToggleButton fav;
        ImageButton clearDateButton;
        Button deleteItemButton, updateItemButton;
        ConstraintLayout fullProduct;
        EditText expireDateField, quantityField;

        ProductItemViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.pantryCardItem);
            name = itemView.findViewById(R.id.productName);
            description = itemView.findViewById(R.id.productCardDescription);
            infoLabel = itemView.findViewById(R.id.infoLabel);
            quantity  = itemView.findViewById(R.id.productQuantity);
            icon = itemView.findViewById(R.id.iconView);
            expandableStateImage = itemView.findViewById(R.id.expandableStateImageView);
            fav = itemView.findViewById(R.id.favCheckbox);
            fullProduct = itemView.findViewById(R.id.fullDetails);
            clearDateButton = itemView.findViewById(R.id.cancelDateButton);
            deleteItemButton = itemView.findViewById(R.id.removeItemButton);
            updateItemButton = itemView.findViewById(R.id.updateProduct);
            expireDateField = itemView.findViewById(R.id.productExpireDateField);
            quantityField  = itemView.findViewById(R.id.changeQuantityField);
        }
    }
}
