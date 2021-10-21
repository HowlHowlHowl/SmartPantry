package com.example.smartpantry;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class FragmentPreviewProduct extends Fragment {
    private TextView name, description,
            preferenceValue, preferenceLabel,
            errorMsg;
    private ImageButton deleteItem;
    private Button addBtn, voteBtn;
    private ToggleButton voteUp, voteDown;
    private Integer tempPreference = 0;
    private DBHelper db;
    private String productID, barcode;
    private onPreviewActionListener listener;

    public interface onPreviewActionListener {
        void onDeleteFromServer(String id, String barcode);
        void onVoteProduct(int preference, String id, String barcode);
    }
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview_found_product, container, false);

        db = new DBHelper(getActivity().getApplicationContext());
        name = view.findViewById(R.id.previewProductName);
        description = view.findViewById(R.id.previewProductDescription);
        preferenceValue = view.findViewById(R.id.previewProdVoteVal);
        preferenceLabel = view.findViewById(R.id.previewProductVoteLabel);
        errorMsg = view.findViewById(R.id.voteFirstError);
        addBtn = view.findViewById(R.id.previewProdAdd);
        voteBtn = view.findViewById(R.id.previewProdVoteBtn);
        voteUp = view.findViewById(R.id.previewProductVoteUp);
        voteDown = view.findViewById(R.id.previewProductVoteDown);
        deleteItem = view.findViewById(R.id.deleteItemFromServer);

        FrameLayout bg = view.findViewById(R.id.notificationManagerBg);
        ConstraintLayout ignoreTouch = view.findViewById(R.id.bgPopUp);


        bg.setOnClickListener(v-> {
            closeFragment();
        });
        ignoreTouch.setOnClickListener(v -> {
            //To ignore the dismiss of the fragment when the popup window is clicked
        });
        Log.println(Log.ASSERT, "FRAGMENT PREVIEW", "CREATED");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.getArguments() != null) {
            productID = this.getArguments().getString("id");
            barcode = this.getArguments().getString("barcode");
            Integer productPreference = db.getPreference(productID);
            if(productPreference != null) {
                setViewAsAlreadyRated(productPreference);
            }
            name.setText(this.getArguments().getString("name"));
            description.setText(this.getArguments().getString("description"));
        }

        if(this.getArguments().getBoolean("isUserOwned")) {
            deleteItem.setVisibility(View.VISIBLE);
            deleteItem.setOnClickListener(v->{
                askToDeleteFromServer();
            });
        }

        voteUp.setOnClickListener(v -> {
            voteDown.setChecked(false);
            errorMsg.setVisibility(View.GONE);
            if(voteUp.isChecked()) {
                tempPreference = 1;
                preferenceValue.setText("+" + tempPreference);
            } else {
                tempPreference = 0;
                preferenceValue.setText("");
            }
        });

        voteDown.setOnClickListener(v -> {
            voteUp.setChecked(false);
            errorMsg.setVisibility(View.GONE);
            if(voteDown.isChecked()){
            tempPreference = -1;
            preferenceValue.setText("" + tempPreference);
        } else {
            tempPreference = 0;
            preferenceValue.setText("");
        }
        });

        voteBtn.setOnClickListener(v -> {
            if(tempPreference!=0){
                disableVote();
                listener.onVoteProduct(tempPreference, productID, barcode);
            } else {
                errorMsg.setVisibility(View.VISIBLE);
            }
        });

        addBtn.setOnClickListener(v -> {
            FragmentAddProduct fragmentAddProduct = new FragmentAddProduct();
            Bundle bundle = getArguments();
            bundle.putBoolean("alreadyExistingProduct", true);
            fragmentAddProduct.setArguments(bundle);
            getActivity().getSupportFragmentManager().popBackStack();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_main, fragmentAddProduct, "addProductFragment")
                    .addToBackStack(null)
                    .commit();

        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (onPreviewActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement on onPreviewActionListener");
        }
    }

    private void askToDeleteFromServer() {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(context.getResources().getString(R.string.warningText))
                .setMessage(context.getResources().getString(R.string.deleteProductFromServer))
                .setPositiveButton(
                        context.getResources().getString(R.string.confirmBtnText),
                        (dialog, id) -> {
                            listener.onDeleteFromServer(productID, barcode);
                            closeFragment();
                        })
                .setNegativeButton(
                        context.getResources().getString(R.string.cancelText),
                        (dialog, id) -> dialog.cancel()
                );
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
    private void setViewAsAlreadyRated(Integer preference) {
        //Preference display
        preferenceValue.setText((preference > 0 ? "+" : "") + preference);
        voteUp.setChecked(preference==1);
        voteDown.setChecked(preference==-1);
        preferenceLabel.setText(getResources().getString(R.string.yourVote));

        disableVote();
        errorMsg.setVisibility(View.VISIBLE);
        errorMsg.setText(R.string.alreadyRatedProduct);
    }

    public void handleError() {
        setViewAsAlreadyRated(tempPreference);
    }
    public void showRatingResult(Integer rating) {
        db.insertNewPreference(productID, tempPreference);
        preferenceLabel.setText(getResources().getString(R.string.yourVote));
        preferenceValue.setText((tempPreference > 0 ? "+" : "") + tempPreference.toString());

    }
    public void disableVote() {
        errorMsg.setVisibility(View.GONE);
        voteDown.setEnabled(false);
        voteUp.setEnabled(false);
        voteBtn.setEnabled(false);

    }
    private void closeFragment() {
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .remove(FragmentPreviewProduct.this)
                .commit();
    }
}
