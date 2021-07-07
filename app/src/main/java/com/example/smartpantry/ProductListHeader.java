package com.example.smartpantry;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ProductListHeader implements ProductListGeneric {
    private final String title;
    public ProductListHeader(String title) {
        this.title = title;
    }
    @Override
    public int getViewType() {
        return 1;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView) {
        View view;
        if (convertView == null) {
            view = (View) inflater.inflate(R.layout.recycler_products_header, null);
            view.setClickable(false);
        } else {
            view = convertView;
        }
        TextView text = (TextView) view.findViewById(R.id.header_card_title);
        text.setText(title);

        return view;
    }
}
