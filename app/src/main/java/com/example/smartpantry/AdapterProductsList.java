package com.example.smartpantry;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AdapterProductsList extends ArrayAdapter<ProductListGeneric> {
    private LayoutInflater mInflater;

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    public AdapterProductsList(@NonNull Context context, List<ProductListGeneric> products) {
        super(context, 0, products);
        mInflater = LayoutInflater.from(context);
    }
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }

    @Override
    public boolean isEnabled(int position) {
        if(getItemViewType(position) == TYPE_SEPARATOR){
            return false;
        } else {
            return true;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int rowType = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            //getView returns the view created by the object itself
            switch (rowType) {
                case TYPE_ITEM:
                    convertView = mInflater.inflate(R.layout.recycler_products_card, null);
                    holder.View = getItem(position).getView(mInflater, convertView);
                    break;
                case TYPE_SEPARATOR:
                    convertView = mInflater.inflate(R.layout.recycler_products_header, null);
                    holder.View = getItem(position).getView(mInflater, convertView);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        return convertView;
    }

    public static class ViewHolder {
        public View View;
    }

}
