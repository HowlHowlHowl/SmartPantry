package com.example.smartpantry;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

public class AdapterPantryList extends RecyclerView.Adapter<AdapterPantryList.PantryItemViewHolder>
        implements FragmentAddToShoppingList.AddToShoppingListEvent{
    public static List<ProductPantryItem> pantryProducts;
    private final onCardEvents cardClickedListener;
    private int expandedItem = -1;
    private int previouslyExpandedItem = -1;
    private DBHelper database;

    AdapterPantryList(List<ProductPantryItem> pantryProducts, onCardEvents cardClickedListener, DBHelper activityDatabase) {
        AdapterPantryList.pantryProducts = pantryProducts;
        this.cardClickedListener = cardClickedListener;
        database = activityDatabase;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public void deleteProductFromPantry(int position, Context context) {
        //Delete item from pantry event
        database.deleteProductFromPantry(pantryProducts.get(position).id);
        //this line of code put -1 as the index of the expanded card so that no card appears expanded
        expandedItem = -1;
        cardClickedListener.deleteItem(position);
    }

    @Override
    public void updateProductShoppingQuantity(int position, long toBuyQnt) {
        pantryProducts.get(position).shopping_qnt = toBuyQnt;
        notifyItemChanged(position);
    }

    public interface onCardEvents {
        void cardClicked(int position, int expandedItem, int previouslyExpandedItem);
        void deleteItem(int position);
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
        //holder.cv.setId(pantryProducts.get(holder.getAdapterPosition()).id);
        //isExpanded is true if the current item is expanded
        final boolean isExpanded = (holder.getAdapterPosition() == expandedItem);
        holder.fullProduct.setVisibility(isExpanded ? VISIBLE : GONE);
        holder.expandableStateImage.setImageResource(isExpanded ? R.drawable.up_arrow : R.drawable.down_arrow);
        holder.cv.setActivated(isExpanded);
        if (isExpanded) {
            previouslyExpandedItem = holder.getAdapterPosition();
        }

        //If item already in the shopping list, an icon is showed and the button is disabled
        holder.inShoppingIcon.setVisibility(
                pantryProducts.get(holder.getAdapterPosition()).shopping_qnt > 0 ?
                        VISIBLE : GONE
        );
        holder.addToShoppingButton.setEnabled(pantryProducts.get(holder.getAdapterPosition()).shopping_qnt <= 0);

        holder.cv.setOnClickListener(v-> {
            //Logic to keep just one of the cards expanded
            expandedItem = isExpanded ? -1 : holder.getAdapterPosition();
            //Delegate notify and scroll to the activity
            cardClickedListener.cardClicked(
                    holder.getAdapterPosition(),
                    expandedItem,
                    previouslyExpandedItem
            );
        });

        String expireDate = (pantryProducts.get(holder.getAdapterPosition()).expire_date == null ? "" :
                pantryProducts.get(holder.getAdapterPosition()).expire_date);
        String toDisplayDateLabel = "";
        holder.expireDate.setTextColor(ContextCompat.getColor(holder.cv.getContext(),
                R.color.black)
        );

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
        holder.expireDateField.setOnClickListener(v -> initializeDatePicker(v.getContext(), holder, expireDate));

        //Disable change date button when date isn't changed
        setDateTextObserver(holder);

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
            pantryProducts.get(holder.getAdapterPosition()).expire_date = formattedDate;

            database.changeExpireDate(pantryProducts.get(holder.getAdapterPosition()).id, formattedDate);

            notifyItemChanged(holder.getAdapterPosition());
        });

        //Clear date field
        holder.clearDateButton.setOnClickListener(v-> holder.expireDateField.setText(""));

        //Set quantity value
        holder.quantity.setText("x" + pantryProducts.get(holder.getAdapterPosition()).quantity);

        //Set quantity field value
        holder.quantityField.setText(String.valueOf(pantryProducts.get(holder.getAdapterPosition()).quantity));

        //Disable change quantity button when quantity isn't changed
        setQuantityTextObserver(holder);

        //Change quantity event
        holder.changeQuantityButton.setOnClickListener(v->{
            if(holder.quantityField!=null){
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) v.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                //Clear Focus
                holder.quantityField.clearFocus();
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                long quantityValue = Long.parseLong(holder.quantityField.getText().toString());
                if(quantityValue >= 0) {
                    //Update quantity
                    pantryProducts.get(holder.getAdapterPosition()).quantity = quantityValue;
                    holder.quantity.setText("x" + pantryProducts.get(holder.getAdapterPosition()).quantity);

                    database.changeQuantity(pantryProducts.get(holder.getAdapterPosition()).id, pantryProducts.get(holder.getAdapterPosition()).quantity);
                    if(quantityValue==0) {
                        database.deleteProductFromPantry(pantryProducts.get(holder.getAdapterPosition()).id);
                    } else {
                        holder.changeQuantityButton.setEnabled(false);
                    }
                }
            }
        });

        //Delete item from pantry event
        holder.deleteItemButton.setOnClickListener(v->
            //Ask to add in shopping list
            askToAddInShoppingList(
                    holder.cv.getContext(),
                    pantryProducts.get(holder.getAdapterPosition()).quantity,
                    pantryProducts.get(holder.getAdapterPosition()).id,
                    holder.getAdapterPosition(),
                    true
            )
        );

        //Add to shopping list
        holder.addToShoppingButton.setOnClickListener(view ->
            askToAddInShoppingList(
                holder.cv.getContext(),
                pantryProducts.get(holder.getAdapterPosition()).quantity,
                pantryProducts.get(holder.getAdapterPosition()).id,
                holder.getAdapterPosition(),
                false
            )
        );


        //Set favorite value
        holder.fav.setChecked(pantryProducts.get(holder.getAdapterPosition()).is_favorite);
        //Change Favorite event
        holder.fav.setOnClickListener(v -> {
            database.setFavorite(holder.fav.isChecked(), pantryProducts.get(holder.getAdapterPosition()).id);
            pantryProducts.get(holder.getAdapterPosition()).is_favorite = holder.fav.isChecked();
            notifyDataSetChanged();
        });

        //Load and show icon
        try {
            AssetManager assetManager = holder.cv.getContext().getAssets();
            InputStream ims = assetManager.open(pantryProducts.get(holder.getAdapterPosition()).icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            holder.icon.setImageBitmap(bitmap);
        } catch (IOException e) {
            //e.printStackTrace();
        }

        //Set name value
        holder.name.setText(pantryProducts.get(holder.getAdapterPosition()).name);

        //Set description value and listener to ignore touch
        holder.description.setText(pantryProducts.get(holder.getAdapterPosition()).description);
        holder.description.setOnClickListener(v->{});
    }

    private void askToAddInShoppingList(Context context, long quantity, String id, int position, boolean deleteAfter) {
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        bundle.putLong("quantity", quantity);
        //If deleteAfter is true, it is transmitted to the fragment so that
        //the fragment can handle the delete of the product from the pantry
        bundle.putBoolean("delete", deleteAfter);
        bundle.putInt("position", position);
        FragmentAddToShoppingList fragmentAddToShoppingList = new FragmentAddToShoppingList(this);

        fragmentAddToShoppingList.setArguments(bundle);
        ((ActivityMain)context).getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.activity_main, fragmentAddToShoppingList, Global.FRAG_ADD_SHOP)
                .addToBackStack(Global.FRAG_ADD_SHOP)
                .commit();
    }

    private void setQuantityTextObserver(PantryItemViewHolder holder) {
        holder.quantityField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void afterTextChanged(Editable s) {
                holder.changeQuantityButton.setEnabled(
                        !holder.quantityField.getText().toString().isEmpty() &&
                        !s.toString().equals(String.valueOf(pantryProducts.get(holder.getAdapterPosition()).quantity))
                );
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void setDateTextObserver(PantryItemViewHolder holder) {
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
                        !(formattedDate.equals(pantryProducts.get(holder.getAdapterPosition()).expire_date))
                );
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

        });

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

    public static class PantryItemViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name, description, expireDate, quantity;
        ImageView icon, expandableStateImage, inShoppingIcon;
        ToggleButton fav;
        ImageButton clearDateButton, addToShoppingButton;
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
            inShoppingIcon = itemView.findViewById(R.id.inShoppingListIcon);
            fav = itemView.findViewById(R.id.favCheckbox);
            fullProduct = itemView.findViewById(R.id.fullDetails);
            clearDateButton = itemView.findViewById(R.id.cancelDateButton);
            changeDateButton = itemView.findViewById(R.id.confirmChangeDate);
            changeQuantityButton = itemView.findViewById(R.id.changeQuantityButton);
            deleteItemButton = itemView.findViewById(R.id.removeItemButton);
            expireDateField = itemView.findViewById(R.id.expireHint);
            quantityField  = itemView.findViewById(R.id.changeQuantityField);
            addToShoppingButton  = itemView.findViewById(R.id.addToShoppingBtn);
        }
    }
}