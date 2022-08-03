package com.example.smartpantry;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.Calendar;
import java.util.Date;

public class IntentServiceSetAlarm extends JobIntentService {
    public static final int JOB_ID = 1;
    //Since oreo background services are automatically dismissed in certain circums.
    //so a WorkManager is better if you need to set constraints on the scheduled job
    //A JobIntentService is a service that enqueues work into a JobScheduler
    //which guarantees the execution of the service even if the app is closed by the user while the WorkManager doesn't
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, IntentServiceSetAlarm.class, JOB_ID, work);
    }
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        setFavoritesAlarm(4, 8, 30, 0);
                      setExpiringAlarm(0, 15, 0);
    }

    public void setExpiringAlarm(int delay_h, int delay_m, int delay_s) {
        Intent alarmBroadcastIntent = new Intent(this, BroadcastReceiverExpireCheck.class);
        alarmBroadcastIntent.setAction(Global.EXPIRED_INTENT_ACTION);
        //The warning for the last parameter is due to a bug resolved in the new alpha release but the alpha it's unstable
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                Global.REQUEST_CODE_CHECK_EXPIRED,
                alarmBroadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //calendar will hold the time for the alarm to fire
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //Set hour minute and seconds
        calendar.set(Calendar.HOUR_OF_DAY, delay_h);
        calendar.set(Calendar.MINUTE, delay_m);
        calendar.set(Calendar.SECOND, delay_s);
        //If current hour is past delay_h:delay_n:delay_s
        //set the date to next day
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

    private void setFavoritesAlarm(int delay_days, int delay_h, int delay_m, int delay_s) {
        Intent alarmBroadcastIntent = new Intent(this, BroadcastReceiverFavoritesCheck.class);
        alarmBroadcastIntent.setAction(Global.FAVORITES_INTENT_ACTION);

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                Global.REQUEST_CODE_CHECK_FAVORITES,
                alarmBroadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //calendar will hold the time for the alarm to fire
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //Set hour minutes and seconds
        calendar.set(Calendar.HOUR_OF_DAY, delay_h);
        calendar.set(Calendar.MINUTE, delay_m);
        calendar.set(Calendar.SECOND, delay_s);
        //If current hour is past delay_h:delay_n:delay_s
        //set the date to next day
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        //Hour of the day
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * delay_days,
                pendingIntent
        );
    }
}
