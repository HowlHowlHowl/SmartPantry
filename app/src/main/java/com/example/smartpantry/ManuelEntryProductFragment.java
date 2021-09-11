package com.example.smartpantry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class ManuelEntryProductFragment extends Fragment {
    EditText barcode;
    Button confirmBtn;
    ConstraintLayout bg, window;
    onManualEntryListener manualEntryListener;

    public interface onManualEntryListener {
        void manualEntry(String barcodeString);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_manual_entry, container, false);
        barcode = view.findViewById(R.id.scanResultCode);
        confirmBtn = view.findViewById(R.id.confirmBtn);
        bg = view.findViewById(R.id.manualEntryFragment);
        window = view.findViewById(R.id.popUpWindow);
        Log.println(Log.ASSERT, "FRAGMENT MANUAL ENTRY", "CREATED");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bg.setOnClickListener(v -> {
            closeFragment();
        });
        window.setOnClickListener(v -> {
            //Prevent window from closing when clicked
        });
        //Trigger MainActivity listener and close itself
        confirmBtn.setOnClickListener(v -> {
            String correctBarcode = barcode.getText().toString();
            manualEntryListener.manualEntry(correctBarcode);
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .remove(ManuelEntryProductFragment.this)
                    .commit();
        });
    }

    public void closeFragment() {
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .remove(ManuelEntryProductFragment.this)
                .commit();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            manualEntryListener = (onManualEntryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onProductAddedListener");
        }
    }
}
