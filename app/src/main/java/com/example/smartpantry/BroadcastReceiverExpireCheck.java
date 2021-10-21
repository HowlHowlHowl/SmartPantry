package com.example.smartpantry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverExpireCheck extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean notify = context.getSharedPreferences(Global.UTILITY, Context.MODE_PRIVATE).getBoolean(Global.NOTIFY_EXPIRED, true);
        if(notify) {
            DBHelper db = new DBHelper(context);
            int expiredProductsCount = db.getExpiredProductsCount();
            db.close();
            if (expiredProductsCount > 0) {
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.createExpNotification(expiredProductsCount);
            }
        }
    }
}
