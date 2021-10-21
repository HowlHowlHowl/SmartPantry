package com.example.smartpantry;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class FragmentNotificationManager extends Fragment {
    private SwitchCompat expiredSwitch, favoritesSwitch;
    private Button closeFragmentBtn;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_notification_manager, container, false);
        expiredSwitch = view.findViewById(R.id.expiredNotify);
        favoritesSwitch = view.findViewById(R.id.favoritesNotify);
        closeFragmentBtn =  view.findViewById(R.id.closeNotificationM);
        view.findViewById(R.id.notificationManagerBg).setOnClickListener(v-> closeFragment());
        view.findViewById(R.id.bgPopUp).setOnClickListener(v->{});
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences sp = getActivity().getSharedPreferences(Global.UTILITY, Context.MODE_PRIVATE);

        expiredSwitch.setChecked(sp.getBoolean(Global.NOTIFY_EXPIRED, true));
        expiredSwitch.setOnClickListener(v->{
            sp.edit().putBoolean(Global.NOTIFY_EXPIRED, expiredSwitch.isChecked()).apply();
        });

        favoritesSwitch.setChecked(sp.getBoolean(Global.NOTIFY_FAVORITES, false));
        favoritesSwitch.setOnClickListener(v->{
            sp.edit().putBoolean(Global.NOTIFY_FAVORITES, favoritesSwitch.isChecked()).apply();
        });

        closeFragmentBtn.setOnClickListener(v->{
            closeFragment();
        });
    }
    public void closeFragment() {
        getActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }
}
