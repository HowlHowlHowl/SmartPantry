package com.example.smartpantry;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
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

public class AdapterPantryList extends RecyclerView.Adapter<AdapterPantryList.PantryItemViewHolder>{
    public static List<ProductPantryItem> pantryProducts;
    private onCardClicked cardClickedListener;
    private int expandedItem = -1;
    private int previouslyExpandedItem = -1;

    AdapterPantryList(List<ProductPantryItem> pantryProducts, onCardClicked cardClickedListener) {
        AdapterPantryList.pantryProducts = pantryProducts;
        this.cardClickedListener = cardClickedListener;
    }

    public interface onCardClicked {
        void cardClicked(int position, int height, int expandedItem, int previouslyExpandedItem);
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
        holder.expandableStateImage.setImageResource(isExpanded ? R.drawable.up_arrow : R.drawable.down_arrow);
        holder.cv.setActivated(isExpanded);
        if (isExpanded) {
            previouslyExpandedItem = position;
        }
        holder.cv.setOnClickListener(v-> {
            //Logic to keep just one of the cards expanded
            expandedItem = isExpanded ? -1 : position;
            //Delegate notify and scroll to the activity
            cardClickedListener.cardClicked(
                    position,
                    (isExpanded ? 0 : holder.cv.getHeight()),
                    expandedItem,
                    previouslyExpandedItem
            );
        });

        String expireDate = (pantryProducts.get(position).expire_date == null ? "" : pantryProducts.get(position).expire_date);
        String toDisplayDateLabel = "";
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
                holder.expireDate.setTextColor(
                        ContextCompat.getColor(holder.cv.getContext(),
                                R.color.design_default_color_error)
                );
            }

        } else {
            holder.expireDateField.setText("");
        }
        holder.expireDate.setText(toDisplayDateLabel);

        //Date picker event
        holder.expireDateField.setOnClickListener(v -> {
            initializeDatePicker(v.getContext(), holder, expireDate);
        });

        //Disable change date button when date isn't changed
        setDateTextObserver(holder, position);

        //Change expire date event
        holder.changeDateButton.setOnClickListener(v->{
            //Show date in localFormat from fixed format dd/MM/yyyy
            DateFormat originalFormat =  DateFormat.getDateInstance();
            DateFormat targetFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
            String formattedDate = Global.changeDateFormat(
                    holder.expireDateField.getText().toString(),
                    originalFormat,
                    targetFormat);
            holder.expireDateField.setText(holder.expireDateField.getText().toString());
            pantryProducts.get(position).expire_date = formattedDate;
            DBHelper db = new DBHelper(holder.cv.getContext());
            db.changeExpireDate(pantryProducts.get(position).id, formattedDate);
            db.close();
            notifyItemChanged(position);
        });

        //Clear date field
        holder.clearDateButton.setOnClickListener(v->{
            holder.expireDateField.setText("");
        });

        //Set quantity value
        holder.quantity.setText("x" + pantryProducts.get(position).quantity);

        //Set quantity field value
        holder.quantityField.setText(String.valueOf(pantryProducts.get(position).quantity));

        //Disable change quantity button when quantity isn't changed
        setQuantityTextObserver(holder, position);

        //Change quantity event
        holder.changeQuantityButton.setOnClickListener(v->{
            if(holder.quantityField!=null){
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) v.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                //Clear Focus
                holder.quantityField.clearFocus();
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                int quantityValue = Integer.parseInt(holder.quantityField.getText().toString());
                if(quantityValue >= 0) {
                    //Update quantity
                    pantryProducts.get(position).quantity = quantityValue;
                    holder.quantity.setText("x" + pantryProducts.get(position).quantity);
                    DBHelper db = new DBHelper(holder.cv.getContext());
                    db.changeQuantity(pantryProducts.get(position).id, pantryProducts.get(position).quantity);
                    if(quantityValue==0) {
                        db.deleteProductFromPantry(pantryProducts.get(position).id);
                    } else {
                        holder.changeQuantityButton.setEnabled(false);
                    }
                    db.close();
                }
            }
        });

        //Delete item from pantry event
        holder.deleteItemButton.setOnClickListener(v->{
            askToDeleteProductFromPantry(position, holder.cv.getContext());
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
            AssetManager assetManager = holder.cv.getContext().getAssets();
            InputStream ims = assetManager.open(pantryProducts.get(position).icon);
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

    private void setQuantityTextObserver(PantryItemViewHolder holder, int position) {
        holder.quantityField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void afterTextChanged(Editable s) {
                holder.changeQuantityButton.setEnabled(
                        !s.toString().equals(String.valueOf(pantryProducts.get(position).quantity))
                );
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void setDateTextObserver(PantryItemViewHolder holder, int position) {
        holder.expireDateField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void afterTextChanged(Editable s) {
                DateFormat originalFormat =  DateFormat.getDateInstance();
                DateFormat targetFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
                String formattedDate = Global.changeDateFormat(
                        holder.expireDateField.getText().toString(),
                        originalFormat,
                        targetFormat);
                holder.changeDateButton.setEnabled(
                        !(formattedDate.equals(pantryProducts.get(position).expire_date))
                );
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

        });

    }

    private void askToDeleteProductFromPantry(int position, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(context.getResources().getString(R.string.warningText))
                .setMessage(context.getResources().getString(R.string.deleteProductFromPantry))
                .setPositiveButton(
                        context.getResources().getString(R.string.confirmBtnText),
                        (dialog, id) -> {
                            DBHelper db  = new DBHelper(context);
                            db.deleteProductFromPantry(pantryProducts.get(position).id);
                            db.close();
                            pantryProducts.remove(position);
                            notifyItemRemoved(position);
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

    public void initializeDatePicker(Context context, PantryItemViewHolder holder, String expireDate){
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