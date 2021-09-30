package com.example.smartpantry;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class FragmentIconPicker extends Fragment implements AdapterIconsGrid.ProcessIconSelection {
    String selectedIcon = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_icon_picker_dialog, container, false);
        GridView gridView = view.findViewById(R.id.iconGrid);
        AdapterIconsGrid gridAdapter = (new AdapterIconsGrid(getContext(), this));
        gridView.setAdapter(gridAdapter);

        view.findViewById(R.id.setIconBtn).setOnClickListener(v -> {
            if(selectedIcon!=null) {
                Log.println(Log.ASSERT, "FRAGMENT ICON PICKER", "ICON SELECTED: " + selectedIcon);
                //Send selected icon from this fragment to the parent
                FragmentAddProduct apf = (FragmentAddProduct)getActivity()
                        .getSupportFragmentManager()
                        .findFragmentByTag("addProductFragment");
                apf.onSelectIconPressed(selectedIcon);
                getActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
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
        selectedIcon = iconName;
    }
}
