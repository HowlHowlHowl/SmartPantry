package com.example.smartpantry;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FragmentAlreadySavedBarcode extends Fragment implements AdapterAlreadySavedList.onCardClicked {
    private Button confirmBtn, cancelBtn;
    private RecyclerView alreadySavedRecycler;
    private List<ProductAlreadySaved> alreadySavedList;
    private String barcode;
    private onConfirmSearchPressedListener confirmSearchPressedListener;

    public interface onConfirmSearchPressedListener {
        void onConfirmSearchPressed(String barcode);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_already_saved_barcode, container, false);
        confirmBtn = view.findViewById(R.id.searchAnywayBtn);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        alreadySavedRecycler = view.findViewById(R.id.alreadySavedList);
        view.findViewById(R.id.bgPopUp).setOnClickListener(v-> closeFragment());
        view.findViewById(R.id.windowPopUp).setOnClickListener(v->{});
        barcode = getArguments().getString("barcode");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setAlreadySavedRecycler(view.getContext());
        confirmBtn.setOnClickListener(v->{
            closeFragment();
            confirmSearchPressedListener.onConfirmSearchPressed(barcode);
        });
        cancelBtn.setOnClickListener(v-> closeFragment());
    }

    @Override
    public void cardClicked(int position) {
        closeFragment();
        Intent showProductsIntent =  new Intent(getActivity(), ActivityShowProducts.class);
        showProductsIntent.putExtra("position", position);
        startActivityForResult(showProductsIntent, Global.PRODUCTS_ACTIVITY);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            confirmSearchPressedListener = (FragmentAlreadySavedBarcode.onConfirmSearchPressedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onConfirmSearchPressedListener");
        }
    }

    private void setAlreadySavedRecycler(Context context) {
        alreadySavedRecycler.setLayoutManager(new LinearLayoutManager(context));
        alreadySavedRecycler.setItemAnimator(null);
        alreadySavedList = new ArrayList<>();

        SharedPreferences sp = context.getSharedPreferences(Global.LISTS_ORDER, MODE_PRIVATE);
        sp.edit().putBoolean(Global.NOT_IN_PANTRY, false).apply();

        boolean productsNotInPantry = sp.getBoolean(Global.NOT_IN_PANTRY, false);
        String productsOrder = sp.getString(Global.TEMP_ORDER, sp.getString(Global.ORDER, DBHelper.COLUMN_PRODUCT_IS_FAVORITE));
        String productsFlow = sp.getString(Global.TEMP_FLOW, sp.getString(Global.FLOW, Global.DESC_ORDER));

        populateAlreadySavedList(productsNotInPantry, productsOrder, productsFlow, context);

        AdapterAlreadySavedList alreadySavedAdapter = new AdapterAlreadySavedList(alreadySavedList, this);
        alreadySavedRecycler.setAdapter(alreadySavedAdapter);
    }

    private void populateAlreadySavedList(boolean notInPantry, String order, String flow, Context context) {
        alreadySavedList.clear();
        DBHelper database = new DBHelper(context);
        Cursor cursor = database.getAllProducts(notInPantry, order, flow);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if(barcode.equals(
                    cursor.getString(
                            cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_BARCODE)
                    )
            )) {
                alreadySavedList.add(new ProductAlreadySaved(
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_NAME)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_ICON)),
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_EXPIRE_DATE)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_IN_PANTRY)),
                    cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_PRODUCT_QUANTITY)),
                    cursor.getPosition()
                ));
            }
            cursor.moveToNext();
        }
        database.close();
        cursor.close();
    }

    public void closeFragment() {
        FragmentManager fm = getActivity()
                .getSupportFragmentManager();
        fm.popBackStack();
    }
}
