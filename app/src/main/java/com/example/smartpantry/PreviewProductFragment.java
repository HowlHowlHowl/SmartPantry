package com.example.smartpantry;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.Objects;

public class PreviewProductFragment extends Fragment {
    private TextView name, description,
            preferenceValue, preferenceLabel,
            errorMsg;
    private Button addBtn, voteBtn;
    private ToggleButton voteUp, voteDown;
    private Integer tempPreference = 0;
    private DBHelper db;
    private String productID;
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

        FrameLayout bg = view.findViewById(R.id.previewBackground);
        ConstraintLayout ignoreTouch = view.findViewById(R.id.popUpWindow);


        bg.setOnClickListener(v-> {
            Objects.requireNonNull(getActivity())
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .remove(PreviewProductFragment.this)
                    .commit();
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
            Integer productPreference = db.getPreference(productID);
            if(productPreference != null) {
                setViewAsAlreadyRated(productPreference);
            }
            name.setText(this.getArguments().getString("name"));
            description.setText(this.getArguments().getString("description"));
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
                ((MainActivity)getActivity()).voteProduct(tempPreference, productID);
            } else {
                errorMsg.setVisibility(View.VISIBLE);
            }
        });
        addBtn.setOnClickListener(v -> {
            AddProductFragment addProductFragment = new AddProductFragment();
            Bundle bundle = getArguments();
            bundle.putBoolean("alreadyExistingProduct", true);
            addProductFragment.setArguments(bundle);
            getActivity().getSupportFragmentManager().popBackStack();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_main, addProductFragment, "addProductFragment")
                    .addToBackStack(null)
                    .commit();

        });
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
        //TODO
        // Da rimuovere insertNP poiché serve a testare la logica per prodotti votati
        // prima dell'implementazione della table preferences
        // e sostituire con la chiamata di setVAAR
        db.insertNewPreference(productID, tempPreference);
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
}
