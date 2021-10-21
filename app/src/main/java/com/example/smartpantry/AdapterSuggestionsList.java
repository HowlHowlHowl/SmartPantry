package com.example.smartpantry;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AdapterSuggestionsList extends  ArrayAdapter<SuggestionItem> {
    private static final int POSITION_IN_PANTRY_TAG = 1;
    private final Context context;

    public AdapterSuggestionsList(@NonNull Context context, @NonNull List<SuggestionItem> products) {
        super(context, 0, products);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SuggestionItem suggestion = getItem(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.result_product_item, parent, false);
        }

        // Set as id the position of the item in the pantry so that,
        // once clicked on the suggestion, its position in the pantry is easy to find
        convertView.setTag(getItem(position).positionInPantry);

        //Set the views content
        TextView suggestionName = convertView.findViewById(R.id.productSuggestionName);
        suggestionName.setText(suggestion.name);

        TextView suggestionDescription = convertView.findViewById(R.id.productSuggestionDescription);
        suggestionDescription.setText(suggestion.description);

        ImageView suggestionIcon = convertView.findViewById(R.id.productSuggestionIcon);
        try {
            AssetManager assetManager = context.getAssets();
            InputStream ims = assetManager.open(suggestion.icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            suggestionIcon.setImageBitmap(bitmap);
            ims.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return convertView;
    }

    public static class ViewHolder {
        public View View;
    }

}
