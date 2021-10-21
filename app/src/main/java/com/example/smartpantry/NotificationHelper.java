package com.example.smartpantry;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper {
    private final Context context;
    NotificationHelper(Context context) {
        this.context = context;
    }


    void createExpNotification( int expiredProductsCount) {
        //Build Intent for MainActivity showing expired items first.
        Intent expiredIntent = new Intent(context, ActivityMain.class);
        expiredIntent.setAction(Global.EXPIRED_INTENT_ACTION);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent expiredPendingIntent = PendingIntent.getActivity(
                context,
                0,
                expiredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Build notification message
        String notificationMessage = context.getString(R.string.expireProductNotificationDescription) +" "+ (expiredProductsCount > 1 ?
                (context.getString(R.string.pluralArticle) +" "+ expiredProductsCount +" "+ context.getString(R.string.productsText)) :
                (context.getString(R.string.singularArticle) +" "+ context.getString(R.string.productText)));
        //Build notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Global.NOTIFICATION_EXP_CHANNEL);
        builder
                /*.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage)) IF I'LL EVER NEED MORE TEXT*/
                .setContentTitle(context.getString(R.string.expireProductNotificationTitle))
                .setContentText(notificationMessage)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setOngoing(false)
                .setColor(ContextCompat.getColor(context, R.color.app_color))
                .setColorized(true)
                .setOnlyAlertOnce(true)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.smart_pantry_launcher))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(expiredPendingIntent);
        createExpNotificationChannel();
        //Send notification to OS
        notificationManager.notify(Global.NOTIFICATION_EXPIRED_ID, builder.build());
    }

    void createFavNotification(int missingFavCount){
        //Build Intent for MainActivity showing expired items first.
        Intent expiredIntent = new Intent(context, ActivityMain.class);
        expiredIntent.setAction(Global.FAVORITES_INTENT_ACTION);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent expiredPendingIntent = PendingIntent.getActivity(
                context,
                0,
                expiredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Build notification message
        String notificationMessage =context.getString(R.string.heyText) + missingFavCount + " "+ context.getString(R.string.missingFavNotificationDescription);
        //Build notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Global.NOTIFICATION_FAV_CHANNEL);
        builder
                .setContentTitle(context.getString(R.string.favProductNotificationTitle))
                .setContentText(notificationMessage)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setOngoing(false)
                .setColor(ContextCompat.getColor(context, R.color.app_color))
                .setColorized(true)
                .setOnlyAlertOnce(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationMessage))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.smart_pantry_launcher))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(expiredPendingIntent);
        createFavNotificationChannel();
        //Send notification to OS
        notificationManager.notify(Global.NOTIFICATION_FAVORITES_ID, builder.build());
    }

    private void createExpNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.expireProductNotificationChannelName);
            String description = context.getString(R.string.expireProductNotificationChannelDescription);
            NotificationChannel channel = new NotificationChannel(
                    Global.NOTIFICATION_EXP_CHANNEL,
                    name,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 100, 1000, 200, 2000, 500, 1000});
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void createFavNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.favProductNotificationChannelName);
            String description = context.getString(R.string.favProductNotificationChannelDescription);
            NotificationChannel channel = new NotificationChannel(
                    Global.NOTIFICATION_FAV_CHANNEL,
                    name,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 100, 1000, 200, 2000, 500, 1000});
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
