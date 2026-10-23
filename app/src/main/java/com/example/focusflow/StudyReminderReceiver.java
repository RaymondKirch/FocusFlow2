package com.example.focusflow;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class StudyReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "study_reminders";
    private static final int NOTIFICATION_ID = 9002;

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        boolean canNotify = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        if (canNotify){
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(
                    android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Time to focus!")
                    .setContentText("Take a step towards your study goals.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID, notification.build());
        }

        // Schedule the next day's reminder
        android.content.SharedPreferences preferences = context.getSharedPreferences("focus_flow_reminders", Context.MODE_PRIVATE);

        int hour = preferences.getInt("hour", 9);
        int minute = preferences.getInt("minute", 0);
        ReminderScheduler.schedule(context, hour, minute);
    }

    private void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Daily reminders for your study goals.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}