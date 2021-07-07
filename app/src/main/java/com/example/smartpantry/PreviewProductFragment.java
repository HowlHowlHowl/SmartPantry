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
    private TextView name, description, voteValue, voteLabel, errorMsg;
    private Button addBtn, voteBtn;
    private ToggleButton voteUp, voteDown;
    private Integer vote = 0;
    private DBHelper db;
    private Integer ratingOnDB = null;
    private String productID;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview_found_product, container, false);

        db = new DBHelper(getActivity().getApplicationContext());
        name = view.findViewById(R.id.previewProductName);
        description = view.findViewById(R.id.previewProductDescription);
        voteValue = view.findViewById(R.id.previewProdVoteVal);
        voteLabel = view.findViewById(R.id.previewProductVoteLabel);
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
           ratingOnDB = db.isAlreadyRated(productID);
            if(ratingOnDB != null) {
                setViewAsAlreadyRated(ratingOnDB);
            }
            name.setText(this.getArguments().getString("name"));
            description.setText(this.getArguments().getString("description"));
        }

        voteUp.setOnClickListener(v -> {
            voteDown.setChecked(false);
            vote = 1;
            voteValue.setText("+"+vote);
        });

        voteDown.setOnClickListener(v -> {
            voteUp.setChecked(false);
            vote = -1;
            voteValue.setText(vote);
        });
        voteBtn.setOnClickListener(v -> {
            if(vote!=0){
                disableVote();
                ((MainActivity)getActivity()).voteProduct(vote, productID);
            } else {
                errorMsg.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setViewAsAlreadyRated(Integer vote) {
        voteValue.setText((vote > 0 ? "+" : "") + vote);
        errorMsg.setVisibility(View.VISIBLE);
        errorMsg.setText(R.string.alreadyRatedProduct);
    }

    public void handleError() {
        //TODO
        // Da rimuovere insertNP poiché serve a testare la logica per prodotti votati
        // prima dell'implementazione della table preferences
        db.insertNewPreference(productID, vote);
        setViewAsAlreadyRated(vote);
    }
    public void showRatingResult(Integer rating) {
        //TODO: SALVO E QUINDI MOSTRO IL VOTO UTENTE E NON I VOTI TOTALI, OK?
        db.insertNewPreference(productID, vote);
        voteLabel.setText(getResources().getString(R.string.ratingResult));
        voteValue.setText(rating.toString());
    }
    public void disableVote() {
        errorMsg.setVisibility(View.INVISIBLE);
        voteDown.setClickable(false);
        voteUp.setClickable(false);
        voteBtn.setEnabled(false);

    }
}
