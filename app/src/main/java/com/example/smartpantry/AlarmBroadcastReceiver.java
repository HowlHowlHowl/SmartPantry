package com.example.smartpantry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmBroadcastReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Global.EXPIRED_INTENT_ACTION:
                DBHelper db = new DBHelper(context);
                int expiredProductsCount = db.getExpiredProductsCount();
                db.close();
                if(expiredProductsCount > 0) {
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.createNotification(expiredProductsCount);
                }
                break;
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_REBOOT:
                //TODO: ASK WHAT TO DO OR ELSE IGNORE
                break;
        }


    }



}
