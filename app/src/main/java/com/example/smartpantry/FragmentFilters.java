package com.example.smartpantry;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class FragmentFilters extends Fragment {

    private String selectedOrderFlow;
    private String selectedOrder;
    private boolean notInPantryOnly;
    private ImageView flowIcon;
    private SwitchCompat orderFlowSwitch;
    private TextView flowLabel;
    private RadioGroup sortOptions;
    private Button applyBtn;
    private Button saveApplyBtn;
    private CheckBox showNotInPantryOnly;

    onApplyFilters onApplyFiltersListener;

    public interface onApplyFilters {
        void applyFilters(boolean notInPantry, String order, String flow);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_filters, container, false);
        getCurrentOrderPreferences();

        view.findViewById(R.id.bgPopUp).setOnClickListener(v-> closeFragment());
        view.findViewById(R.id.windowPopUp).setOnClickListener(v->{});

        orderFlowSwitch = view.findViewById(R.id.orderFlowSwitch);
        flowLabel = view.findViewById(R.id.orderFlowLabel);
        sortOptions = view.findViewById(R.id.sortingGroup);
        flowIcon = view.findViewById(R.id.flowIcon);
        applyBtn = view.findViewById(R.id.applyFiltersBtn);
        saveApplyBtn = view.findViewById(R.id.saveAndApplyBtn);
        showNotInPantryOnly = view.findViewById(R.id.showOnlyMissingProds);

        if(getArguments()!=null) {
            //If calling activity is SHOPPING_ACT the order by expire date is hidden
            if(getArguments().getInt("activity",0) == Global.SHOPPING_ACTIVITY) {
                view.findViewById(R.id.expireSortBy).setVisibility(View.GONE);
            }
            //If calling activity is SHOW PRODS the option to show only not in pantry items is shown
            if(getArguments().getInt("activity",0) == Global.PRODUCTS_ACTIVITY) {
                showNotInPantryOnly.setVisibility(View.VISIBLE);
                showNotInPantryOnly.setChecked(
                        getContext()
                                .getSharedPreferences(Global.LISTS_ORDER, Context.MODE_PRIVATE)
                                .getBoolean(Global.NOT_IN_PANTRY, false)
                );
            }
        }
        setOrderFlowEventsListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences sp = getContext().getSharedPreferences(Global.LISTS_ORDER, Context.MODE_PRIVATE);
        //If during this session the order fragment has already appeared the parameters are loaded,
        //else the saved sorting preferences are loaded
        setFragmentAppearance(
                sp.getString(Global.TEMP_ORDER, selectedOrder),
                sp.getString(Global.TEMP_FLOW, selectedOrderFlow)
        );

        //Apply button event to sort the list in the calling activity
        applyBtn.setOnClickListener(v->{
            setTempOrderPreferences();
            closeFragment();
            onApplyFiltersListener.applyFilters(
                    showNotInPantryOnly.isChecked(),
                    selectedOrder,
                    selectedOrderFlow
            );
        });

        //Apply and Save button to sort list in calling activity
        //and permanently save the sorting preferences
        saveApplyBtn.setOnClickListener(v->{
            setTempOrderPreferences();
            setOrderPreferences();
            closeFragment();
            onApplyFiltersListener.applyFilters(
                    showNotInPantryOnly.isChecked(),
                    selectedOrder,
                    selectedOrderFlow);
        });

    }

    private void setTempOrderPreferences() {
        SharedPreferences.Editor ed = getContext()
                .getSharedPreferences(Global.LISTS_ORDER, Context.MODE_PRIVATE).edit();
        ed.putString(Global.TEMP_FLOW, selectedOrderFlow);
        ed.putString(Global.TEMP_ORDER, selectedOrder);
        ed.putBoolean(Global.NOT_IN_PANTRY, notInPantryOnly);
        ed.apply();
    }

    private void setOrderFlowEventsListeners() {
        showNotInPantryOnly.setOnClickListener(v->{
            notInPantryOnly= showNotInPantryOnly.isChecked();
        });
        orderFlowSwitch.setOnClickListener(v->{
            if(orderFlowSwitch.isChecked()){
                flowLabel.setText(getResources().getString(R.string.DESCText));
                selectedOrderFlow = Global.DESC_ORDER;
                flowIcon.setImageResource(R.drawable.descending_sorting);
            } else {
                flowLabel.setText(getResources().getString(R.string.ASCText));
                selectedOrderFlow = Global.ASC_ORDER;
                flowIcon.setImageResource(R.drawable.ascending_sorting);
            }
        });
        sortOptions.setOnCheckedChangeListener((radioGroup, checkedID) -> {
            switch (checkedID) {
                case R.id.expireSortBy:
                    selectedOrder = DBHelper.COLUMN_PRODUCT_EXPIRE_DATE;
                    break;
                case R.id.favoriteSortBy:
                    selectedOrder = DBHelper.COLUMN_PRODUCT_IS_FAVORITE;
                    break;
                case R.id.iconSortBy:
                    selectedOrder = DBHelper.COLUMN_PRODUCT_ICON;
                    break;
                case R.id.nameSortBy:
                    selectedOrder = DBHelper.COLUMN_PRODUCT_NAME;
                    break;
                case R.id.quantitySortBy:
                    selectedOrder = DBHelper.COLUMN_PRODUCT_QUANTITY;
                    break;
            }
        });
    }

    public void setFragmentAppearance(String order, String flow) {
        flowLabel.setText(
                flow.equals(Global.DESC_ORDER) ?
                R.string.DESCText : R.string.ASCText
        );

        flowIcon.setImageResource(
                flow.equals(Global.DESC_ORDER) ?
                R.drawable.descending_sorting : R.drawable.ascending_sorting
        );

        orderFlowSwitch.setChecked(flow.equals(Global.DESC_ORDER));

        switch (order) {
            case DBHelper.COLUMN_PRODUCT_EXPIRE_DATE:
                ((RadioButton) sortOptions.findViewById(R.id.expireSortBy)).setChecked(true);
                break;
            case DBHelper.COLUMN_PRODUCT_IS_FAVORITE:
                ((RadioButton) sortOptions.findViewById(R.id.favoriteSortBy)).setChecked(true);
                break;
            case DBHelper.COLUMN_PRODUCT_ICON:
                ((RadioButton) sortOptions.findViewById(R.id.iconSortBy)).setChecked(true);
                break;
            case DBHelper.COLUMN_PRODUCT_NAME:
                ((RadioButton) sortOptions.findViewById(R.id.nameSortBy)).setChecked(true);
                break;
            case DBHelper.COLUMN_PRODUCT_QUANTITY:
                ((RadioButton) sortOptions.findViewById(R.id.quantitySortBy)).setChecked(true);
                break;
        }
    }


    public void setOrderPreferences() {
        SharedPreferences.Editor ed =
                getContext().getSharedPreferences(Global.LISTS_ORDER, Context.MODE_PRIVATE).edit();
        ed.putString(Global.ORDER, selectedOrder);
        ed.putString(Global.FLOW, selectedOrderFlow);
        ed.apply();
    }

    public void closeFragment() {
        getActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }

    public void getCurrentOrderPreferences(){
        SharedPreferences sp = getContext().getSharedPreferences(Global.LISTS_ORDER, Context.MODE_PRIVATE);
        selectedOrder = sp.getString(Global.ORDER,  DBHelper.COLUMN_PRODUCT_IS_FAVORITE);
        selectedOrderFlow = sp.getString(Global.FLOW, Global.DESC_ORDER);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onApplyFiltersListener = (FragmentFilters.onApplyFilters) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onApplyFilters");
        }
    }

}
