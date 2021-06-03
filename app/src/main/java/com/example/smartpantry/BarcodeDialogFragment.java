package com.example.smartpantry;

import android.app.Activity;
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
import androidx.fragment.app.DialogFragment;

public class BarcodeDialogFragment extends DialogFragment {
    EditText barcode;
    Button retryBtn;
    Button confirmBtn;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_scan_result, container, false);
        barcode = view.findViewById(R.id.scanResultCode);
        retryBtn = view.findViewById(R.id.retryBtn);
        confirmBtn = view.findViewById(R.id.confirmBtn);
        Log.println(Log.ASSERT, "FRAGMENT", "CREATED");
    return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barcode.setText(this.getArguments().getString("barcode"));
        retryBtn.setOnClickListener(v -> {
            closeFragment();
        });

        confirmBtn.setOnClickListener(v -> {
            String correctBarcode = barcode.getText().toString();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("barcode", correctBarcode);
            getActivity().setResult(Activity.RESULT_OK, returnIntent);
            closeFragment();
            getActivity().finish();
        });
    }
    public void closeFragment() {
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .remove(BarcodeDialogFragment.this)
                .commit();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((CameraActivity)getActivity()).toggleCaptureBtn();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((CameraActivity)getActivity()).toggleCaptureBtn();
    }
}
