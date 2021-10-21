package com.example.smartpantry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.List;

public class AdapterBarcodeProductsList extends ArrayAdapter<ProductListGeneric> {
    private final LayoutInflater mInflater;

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    public AdapterBarcodeProductsList(@NonNull Context context, List<ProductListGeneric> products) {
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
        return getItemViewType(position) != TYPE_SEPARATOR;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int rowType = getItemViewType(position);
        holder = new ViewHolder();
        //getView returns the view created by the object itself
        switch (rowType) {
            case TYPE_ITEM:
                convertView = mInflater.inflate(R.layout.recycler_barcode_products_card, null);
                holder.View = getItem(position).getView(mInflater, convertView);
                break;
            case TYPE_SEPARATOR:
                convertView = mInflater.inflate(R.layout.recycler_products_header, null);
                holder.View = getItem(position).getView(mInflater, convertView);
                break;
        }
        convertView.setTag(holder);
        return convertView;
    }

    public static class ViewHolder {
        public View View;
    }

}
