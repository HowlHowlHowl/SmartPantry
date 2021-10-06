package com.example.smartpantry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BroadcastReceiverBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO TEST
        Toast.makeText(context, "Broadcast Started", Toast.LENGTH_LONG).show();
        Log.println(Log.ASSERT, "BC RECEIVER", "INTENT RECEIVED");
        Intent intentServiceSetAlarm = new Intent(context, IntentServiceSetAlarm.class);
        context.startService(intentServiceSetAlarm);

    }
}
