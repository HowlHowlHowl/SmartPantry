package com.example.smartpantry;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class FragmentManualEntryProduct extends Fragment {
    EditText barcode;
    Button confirmBtn;
    ConstraintLayout bg, window;
    onManualEntryListener manualEntryListener;

    public interface onManualEntryListener {
        void onManualEntry(String barcodeString);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_manual_entry, container, false);
        barcode = view.findViewById(R.id.scanResultCode);
        confirmBtn = view.findViewById(R.id.addShoppingBtn);
        bg = view.findViewById(R.id.manualEntryFragment);
        window = view.findViewById(R.id.bgPopUp);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bg.setOnClickListener(v -> closeFragment());
        window.setOnClickListener(v -> {
            //Prevent window from closing when clicked
        });
        //Trigger MainActivity listener and close itself
        confirmBtn.setOnClickListener(v -> {
            String correctBarcode = barcode.getText().toString();
            if(!correctBarcode.isEmpty()) {
                closeFragment();
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
                }
                manualEntryListener.onManualEntry(correctBarcode);
            } else {
                barcode.setError(getString(R.string.barcodeError));
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            manualEntryListener = (onManualEntryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onManualEntryListener");
        }
    }

    public void closeFragment() {
        FragmentManager fm = getActivity()
                .getSupportFragmentManager();
        fm.popBackStack();

    }
}
