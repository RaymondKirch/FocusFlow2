package com.example.focusflow;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class ReminderScheduler {

    private static final int REMINDER_ID =9001;

    public static void schedule(Context context, int hour, int minute) {

        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);

        if (manager == null)
            return;

        Intent intent = new Intent(appContext, StudyReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, REMINDER_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        }
        else {
            manager.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancel(Context context) {

        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, StudyReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, REMINDER_ID, intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (manager != null) {
            manager.cancel(pendingIntent);
        }

        pendingIntent.cancel();
    }
}