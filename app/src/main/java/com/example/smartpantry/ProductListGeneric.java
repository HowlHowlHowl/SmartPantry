package com.example.smartpantry;

import android.view.LayoutInflater;
import android.view.View;

public interface ProductListGeneric {
    int getViewType();
    View getView(LayoutInflater inflater, View convertView);
}
