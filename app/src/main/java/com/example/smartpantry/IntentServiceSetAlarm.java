package com.example.smartpantry;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

public class IntentServiceSetAlarm extends IntentService {
    public IntentServiceSetAlarm() {
        super("IntentServiceSetAlarm");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        setAlarm();
    }
    public void setAlarm() {
        Intent alarmBroadcastIntent = new Intent(this, BroadcastReceiverExpireCheck.class);
        alarmBroadcastIntent.setAction(Global.EXPIRED_INTENT_ACTION);

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                Global.REQUEST_CODE_CHECK,
                alarmBroadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT); //The warning for the last parameter is due to a bug resolved
        // in the new alpha release but the alpha it's unstable
        //calendar will hold the time for the alarm to fire
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //Set hour and minute, hardcoded values because a product always expire after midnight so there shouldn't be no need to change these numbers
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);
        //If current hour is past 00:15 set the date to next day
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        //Hour of the day
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }
}
