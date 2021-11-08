package com.example.smartpantry;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;


public class FragmentIconPicker extends Fragment implements AdapterIconsGrid.ProcessIconSelection {
    String selectedIcon = null;
    Button selectBtn;
    onIconChosen iconChosenListener;

    public interface onIconChosen {
        void iconSelected(String icon, int position);
    }

    public FragmentIconPicker(onIconChosen listener) {
        iconChosenListener = listener;
    }

    public FragmentIconPicker() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_icon_picker_dialog, container, false);
        GridView gridView = view.findViewById(R.id.iconGrid);
        AdapterIconsGrid gridAdapter = (new AdapterIconsGrid(getContext(), this));
        gridView.setAdapter(gridAdapter);

        selectBtn = view.findViewById(R.id.setIconBtn);
        selectBtn.setOnClickListener(v -> {
            if(selectedIcon!=null) {
                int adapterPosition = -1;
                if(getArguments()!=null) {
                    adapterPosition = getArguments().getInt("position");
                }
                iconChosenListener.iconSelected(selectedIcon, adapterPosition);
                closeFragment();
            }
        });

        Log.println(Log.ASSERT, "FRAGMENT ICON PICKER", "CREATED");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    //Get the icon selected from the adapter
    @Override
    public void onIconSelected(String iconName) {
        selectedIcon = Global.ICON_DIRNAME + File.separator + iconName;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(iconChosenListener==null) {
            try {
                iconChosenListener = (FragmentIconPicker.onIconChosen) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement onIconChosen");
            }
        }
    }
    public void closeFragment() {
        getActivity()
                .getSupportFragmentManager().popBackStack(Global.FRAG_ICON_PICK, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
