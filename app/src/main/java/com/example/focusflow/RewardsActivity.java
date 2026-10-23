package com.example.focusflow;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RewardsActivity extends AppCompatActivity {

    private TextView txtStars;
    private TextView txtMinutes;
    private TextView txtSessions;
    private TextView txtAchievement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        txtStars = findViewById(R.id.txtStars);
        txtMinutes = findViewById(R.id.txtMinutes);
        txtSessions = findViewById(R.id.txtSessions);
        txtAchievement = findViewById(R.id.txtAchievement);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadRewards() {
        StudyDataManager.loadStats(
                new StudyDataManager.StatsCallback() {
                    @Override
                    public void onLoaded(int stars, int minutes, int sessions) {
                        if (isFinishing() || isDestroyed()) return;

                        AppData.starPoints = stars;
                        AppData.totalStudyMinutes = minutes;
                        AppData.completedSessions = sessions;

                        txtStars.setText("?" + stars + "stars");
                        txtMinutes.setText(minutes + "minutes");
                        txtSessions.setText(String.valueOf(sessions));

                        if (sessions >= 5) {
                            txtAchievement.setText("Achievement unlocked: Focus Champion!");
                        } else if (sessions >= 1) {
                            txtAchievement.setText("Achievement unlocked: First Study Session!");
                        } else {
                            txtAchievement.setText("Complete a study session to unlock your first achievement.");
                        }

                    }

                    @Override
                    public void onFailure(String message) {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(RewardsActivity.this, "Could not load rewards:" + message, Toast.LENGTH_SHORT) .show();
                        }
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRewards();
    }
}