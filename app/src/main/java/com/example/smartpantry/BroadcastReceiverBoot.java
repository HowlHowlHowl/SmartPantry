package com.example.smartpantry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Due to a recent update this only works if the app has been opened at least one time
        //if the app get force-closed by the user it has to be opened again
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent intentServiceSetAlarm = new Intent(context, IntentServiceSetAlarm.class);
            context.startService(intentServiceSetAlarm);
        }
    }
}
