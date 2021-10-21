package com.example.smartpantry;


import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProductListItem implements ProductListGeneric{
    String id;
    String userId;
    String name;
    String description;
    String barcode;
    String createdAt;
    String updatedAt;
    boolean isUserOwned;

    public ProductListItem(String id, String name, String description, String barcode,
                           String userId, String createdAt, String updatedAt,
                           boolean isUserOwned) {


        this.id = id;
        this.name = name;
        this.description = description;
        this.barcode = barcode;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isUserOwned = isUserOwned;
    }

    @Override
    public int getViewType() {
        return 0;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView) {
        View view;
        if (convertView == null) {
            view = (View) inflater.inflate(R.layout.recycler_barcode_products_card, null);
        } else {
            view = convertView;
        }

        TextView nameView;
        TextView updateDateView;
        TextView createDateView;
        TextView descriptionView;
        TextView barcodeView;
        nameView = (TextView)view.findViewById(R.id.productName);
        descriptionView = (TextView)view.findViewById(R.id.productDescription);
        createDateView = (TextView)view.findViewById(R.id.productCreated);
        updateDateView = (TextView)view.findViewById(R.id.productUpdated);
        barcodeView = (TextView)view.findViewById(R.id.productBarcode);

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        DateFormat output = DateFormat.getDateInstance();

        try {
            Date created = input.parse(createdAt);
            Date updated = input.parse(updatedAt);
            createDateView.append(output.format(created));
            updateDateView.append(output.format(updated));

        } catch (ParseException e) {
            e.printStackTrace();
           createDateView.append(createdAt);
           updateDateView.append(updatedAt);
        }
        nameView.setText(name);
        descriptionView.setText(description);
        barcodeView.append(barcode);
        return view;
    }
}
