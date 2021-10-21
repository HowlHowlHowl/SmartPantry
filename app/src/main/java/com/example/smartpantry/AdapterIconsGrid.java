package com.example.smartpantry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class AdapterIconsGrid extends BaseAdapter {
    static final String ICONS_DIR_NAME = Global.ICON_DIRNAME;

    private final Context context;
    private String[] list;
    private Integer selectedIconIndex = null;
    private ImageView selectedIcon = null;
    private final ProcessIconSelection callbackInstance;
    public interface ProcessIconSelection {
        void onIconSelected(String iconName);
    }

    public AdapterIconsGrid(Context c, ProcessIconSelection callback) {
        context = c;
        callbackInstance = callback;
        try {
            list = context.getAssets().list(ICONS_DIR_NAME);
            Log.println(Log.ASSERT, "ICONS LIST", list.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return list.length;
    }

    public Object getItem(int position) {
        return list[position];
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView img;

        if (convertView == null) {

            img = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            img.setLayoutParams(new GridView.LayoutParams(params));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setPadding(8, -100, 8, -50);

        } else {
            img = (ImageView) convertView;
        }
        img.setOnClickListener(v -> {
            Log.println(Log.ASSERT, "SELECTED ICON", getItem(position).toString());
            if (selectedIcon!=null) {
                selectedIcon.setAlpha(.5f);
            }
            selectedIconIndex = position;
            selectedIcon = img;
            selectedIcon.setAlpha(1f);
            callbackInstance.onIconSelected(getItem(position).toString());
        });
        try {
            InputStream ims = context.getAssets().open(ICONS_DIR_NAME + File.separator + list[position]);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            img.setImageBitmap(bitmap);
            if (selectedIconIndex != null && selectedIconIndex == position){
                img.setAlpha(1f);
            } else {
                img.setAlpha(.5f);
            }

        } catch (IOException e) {
            //e.printStackTrace();
        }
        return img;
    }
}