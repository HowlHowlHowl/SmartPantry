package com.example.smartpantry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverFavoritesCheck extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean notify = context.getSharedPreferences(Global.UTILITY, Context.MODE_PRIVATE).getBoolean(Global.NOTIFY_FAVORITES, false);
        if(notify) {
            DBHelper database =  new DBHelper(context);
            int missingFavoritesCount = database.getMissingFavoritesCount();
            database.close();
            if (missingFavoritesCount > 0) {
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.createFavNotification(missingFavoritesCount);
            }
        }
    }
}
