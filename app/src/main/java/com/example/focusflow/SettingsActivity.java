package com.example.focusflow;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private EditText edtPreferredDuration;
    private CheckBox chkReminders;
    private Button btnSave;
    private Button btnReminderTime;

    private int reminderHour = 9;
    private int reminderMinute = 0;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtPreferredDuration =
                findViewById(R.id.edtPreferredDuration);
        chkReminders = findViewById(R.id.chkReminders);
        btnSave = findViewById(R.id.btnSaveSettings);
        btnReminderTime = findViewById(R.id.btnReminderTime);

        Button btnBack = findViewById(R.id.btnBack);

        edtPreferredDuration.setText(
                String.valueOf(AppData.preferredDurationMinutes));

        chkReminders.setChecked(AppData.remindersEnabled);
        updateReminderTimeButton();

        notificationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> saveSettingsNow());

        btnReminderTime.setOnClickListener(v ->
                new TimePickerDialog(
                        SettingsActivity.this,
                        (view, hour, minute) -> {
                            reminderHour = hour;
                            reminderMinute = minute;
                            updateReminderTimeButton();
                        },
                        reminderHour,
                        reminderMinute,
                        false
                ).show());

        btnSave.setOnClickListener(v -> saveSettings());
        btnBack.setOnClickListener(v -> finish());

        loadSettings();
    }

    private void loadSettings() {
        btnSave.setEnabled(false);

        StudyDataManager.loadReminderSettings(
                new StudyDataManager.ReminderSettingsCallback() {
                    @Override
                    public void onLoaded(
                            int duration,
                            boolean reminders,
                            int hour,
                            int minute) {

                        if (isFinishing() || isDestroyed()) return;

                        AppData.preferredDurationMinutes = duration;
                        AppData.remindersEnabled = reminders;

                        edtPreferredDuration.setText(
                                String.valueOf(duration));
                        chkReminders.setChecked(reminders);

                        reminderHour = hour;
                        reminderMinute = minute;
                        updateReminderTimeButton();

                        saveTimeForReceiver();

                        if (reminders) {
                            ReminderScheduler.schedule(
                                    SettingsActivity.this,
                                    reminderHour,
                                    reminderMinute);
                        } else {
                            ReminderScheduler.cancel(
                                    SettingsActivity.this);
                        }

                        btnSave.setEnabled(true);
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isFinishing() || isDestroyed()) return;

                        btnSave.setEnabled(true);

                        Toast.makeText(
                                SettingsActivity.this,
                                "Could not load settings: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateReminderTimeButton() {
        String time = String.format(
                Locale.getDefault(),
                "%d:%02d %s",
                reminderHour % 12 == 0
                        ? 12 : reminderHour % 12,
                reminderMinute,
                reminderHour < 12 ? "AM" : "PM");

        btnReminderTime.setText(time);
    }

    private void saveSettings() {
        String input =
                edtPreferredDuration.getText().toString().trim();

        if (input.isEmpty()) {
            edtPreferredDuration.setError("Enter a duration.");
            return;
        }

        int duration;

        try {
            duration = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            edtPreferredDuration.setError("Enter a valid number.");
            return;
        }

        if (duration < 1 || duration > 240) {
            edtPreferredDuration.setError(
                    "Choose 1 to 240 minutes.");
            return;
        }

        btnSave.setEnabled(false);

        if (chkReminders.isChecked()
                && Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS);
        } else {
            saveSettingsNow();
        }
    }

    private void saveSettingsNow() {
        if (isFinishing() || isDestroyed()) return;

        String input =
                edtPreferredDuration.getText().toString().trim();

        int duration;

        try {
            duration = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            edtPreferredDuration.setError("Enter a valid duration.");
            btnSave.setEnabled(true);
            return;
        }

        if (duration < 1 || duration > 240) {
            edtPreferredDuration.setError(
                    "Choose 1 to 240 minutes.");
            btnSave.setEnabled(true);
            return;
        }

        boolean reminders = chkReminders.isChecked();

        StudyDataManager.saveSettings(
                duration,
                reminders,
                reminderHour,
                reminderMinute,
                new StudyDataManager.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;

                        AppData.preferredDurationMinutes = duration;
                        AppData.remindersEnabled = reminders;

                        saveTimeForReceiver();

                        if (reminders) {
                            ReminderScheduler.schedule(
                                    SettingsActivity.this,
                                    reminderHour,
                                    reminderMinute);
                        } else {
                            ReminderScheduler.cancel(
                                    SettingsActivity.this);
                        }

                        btnSave.setEnabled(true);

                        Toast.makeText(
                                SettingsActivity.this,
                                reminders
                                        ? "Settings saved. Reminder scheduled."
                                        : "Settings saved. Reminders disabled.",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isFinishing() || isDestroyed()) return;

                        btnSave.setEnabled(true);

                        Toast.makeText(
                                SettingsActivity.this,
                                "Could not save settings: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void saveTimeForReceiver() {
        getSharedPreferences(
                "focus_flow_reminders",
                MODE_PRIVATE)
                .edit()
                .putInt("hour", reminderHour)
                .putInt("minute", reminderMinute)
                .putBoolean("enabled", chkReminders.isChecked())
                .apply();
    }

}