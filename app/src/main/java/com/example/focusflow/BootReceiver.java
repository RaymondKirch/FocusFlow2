package com.example.focusflow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences(
                "focus_flow_reminders", Context.MODE_PRIVATE
        );

        boolean remindersEnabled = preferences.getBoolean(
                "enabled", false
        );

        if (!remindersEnabled) {
            return;
        }

        int hour = preferences.getInt("hour", 9);
        int minute = preferences.getInt("minute", 0);

        ReminderScheduler.schedule(context, hour, minute);
    }

}