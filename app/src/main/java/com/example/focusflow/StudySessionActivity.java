package com.example.focusflow;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class StudySessionActivity extends AppCompatActivity {

    private EditText edtDuration;
    private TextView txtTimer, txtStatus;
    private Button btnClockIn, btnClockOut;

    private CountDownTimer countDownTimer;
    private long startedAtMillis;
    private boolean studying = false;
    private boolean saving = false;
    private int plannedMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_session);

        edtDuration = findViewById(R.id.edtDuration);
        txtTimer = findViewById(R.id.txtTimer);
        txtStatus = findViewById(R.id.txtStatus);
        btnClockIn = findViewById(R.id.btnClockIn);
        btnClockOut = findViewById(R.id.btnClockOut);

        Button btnBack = findViewById(R.id.btnBack);

        // Display the current default duration first.
        edtDuration.setText(
                String.valueOf(AppData.preferredDurationMinutes));

        btnClockOut.setEnabled(false);

        // Load the saved duration and reminder preference from Firebase.
        StudyDataManager.loadSettings(
                new StudyDataManager.SettingsCallback() {
                    @Override
                    public void onLoaded(
                            int duration, boolean reminders) {

                        if (isFinishing() || isDestroyed()) return;

                        AppData.preferredDurationMinutes = duration;
                        AppData.remindersEnabled = reminders;

                        if (!studying) {
                            edtDuration.setText(
                                    String.valueOf(duration));
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        // Keep the default duration if loading fails.
                    }
                });

        btnClockIn.setOnClickListener(v -> startSession());
        btnClockOut.setOnClickListener(v -> finishSession());

        btnBack.setOnClickListener(v -> {
            if (studying || saving) {
                Toast.makeText(this,
                        "Please finish your study session first.",
                        Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        });
    }

    private void startSession() {
        if (studying || saving) return;

        String input = edtDuration.getText().toString().trim();

        if (input.isEmpty()) {
            edtDuration.setError("Enter a duration.");
            return;
        }

        int minutes;

        try {
            minutes = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            edtDuration.setError("Enter a valid number.");
            return;
        }

        if (minutes < 1 || minutes > 240) {
            edtDuration.setError("Choose 1 to 240 minutes.");
            return;
        }

        btnClockIn.setEnabled(false);
        txtStatus.setText("Connecting to your account...");

        StudyDataManager.ensureSignedIn(
                new StudyDataManager.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;

                        plannedMinutes = minutes;
                        AppData.preferredDurationMinutes = minutes;
                        startedAtMillis = System.currentTimeMillis();
                        studying = true;

                        edtDuration.setEnabled(false);
                        btnClockOut.setEnabled(true);

                        StudyDataManager.addClockInReward(
                                new StudyDataManager.ResultCallback() {
                                    @Override
                                    public void onSuccess() {
                                        if (isFinishing()
                                                || isDestroyed()) return;

                                        Toast.makeText(
                                                StudySessionActivity.this,
                                                "Clocked in! +5 stars",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        if (isFinishing()
                                                || isDestroyed()) return;

                                        Toast.makeText(
                                                StudySessionActivity.this,
                                                "Could not save clock-in reward: "
                                                        + message,
                                                Toast.LENGTH_LONG).show();
                                    }
                                });

                        txtStatus.setText(
                                "You're clocked in. Stay focused!");

                        startTimer();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isFinishing() || isDestroyed()) return;

                        btnClockIn.setEnabled(true);
                        txtStatus.setText("Sign-in failed.");

                        Toast.makeText(StudySessionActivity.this,
                                "Could not sign in: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(
                plannedMinutes * 60L * 1000L, 1000L) {

            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                long minutesLeft = seconds / 60L;
                long secondsLeft = seconds % 60L;

                txtTimer.setText(String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        minutesLeft,
                        secondsLeft));
            }

            @Override
            public void onFinish() {
                txtTimer.setText("00:00");
                finishSession();

                Toast.makeText(StudySessionActivity.this,
                        "Planned study time completed!",
                        Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void finishSession() {
        if (!studying || saving) return;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        long completedAt = System.currentTimeMillis();

        int actualMinutes = (int) Math.max(
                1L, (completedAt - startedAtMillis) / 60000L);

        studying = false;
        saving = true;

        btnClockIn.setEnabled(false);
        btnClockOut.setEnabled(false);
        edtDuration.setEnabled(false);

        txtStatus.setText("Saving your study session...");

        StudyDataManager.saveCompletedSession(
                plannedMinutes,
                actualMinutes,
                startedAtMillis,
                completedAt,
                new StudyDataManager.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;

                        saving = false;

                        btnClockIn.setEnabled(true);
                        btnClockOut.setEnabled(false);
                        edtDuration.setEnabled(true);

                        txtStatus.setText(
                                "Session completed: "
                                        + actualMinutes + " minute(s).");

                        txtTimer.setText("Session finished");

                        Toast.makeText(StudySessionActivity.this,
                                "Session saved! +5 stars",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isFinishing() || isDestroyed()) return;

                        saving = false;

                        btnClockIn.setEnabled(true);
                        btnClockOut.setEnabled(false);
                        edtDuration.setEnabled(true);

                        txtStatus.setText(
                                "Session could not be saved.");

                        Toast.makeText(StudySessionActivity.this,
                                "Save failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        super.onDestroy();
    }
}
