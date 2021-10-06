package com.example.smartpantry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverExpireCheck extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DBHelper db = new DBHelper(context);
        int expiredProductsCount = db.getExpiredProductsCount();
        db.close();
        if(expiredProductsCount > 0) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.createNotification(expiredProductsCount);
        }
    }
}
