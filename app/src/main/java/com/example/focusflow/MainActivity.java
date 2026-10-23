package com.example.focusflow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends  AppCompatActivity {

    private TextView txtStarPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStarPoints = findViewById(R.id.txtStarPoints);

        Button btnStudy = findViewById(R.id.btnStudy);
        Button btnTasks = findViewById(R.id.btnTasks);
        Button btnRewards = findViewById(R.id.btnRewards);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnStudyHistory = findViewById(R.id.btnStudyHistory);

        btnStudy.setOnClickListener(v -> openScreen(StudySessionActivity.class));
        btnTasks.setOnClickListener(v -> openScreen(TasksActivity.class));
        btnRewards.setOnClickListener(v -> openScreen(RewardsActivity.class));
        btnSettings.setOnClickListener(v -> openScreen(SettingsActivity.class));
        btnStudyHistory.setOnClickListener(v -> openScreen(StudyHistoryActivity.class));
    }

    private void openScreen(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }

    private void updateDashboard() {
        StudyDataManager.loadStats(new StudyDataManager.StatsCallback() {
            @Override
            public void onLoaded(int stars, int minutes, int sessions) {
                if (isFinishing() || isDestroyed()) return;

                AppData.starPoints = stars;
                AppData.totalStudyMinutes = minutes;
                AppData.completedSessions = sessions;

                txtStarPoints.setText("★" + stars + " stars");
            }

            @Override
            public void onFailure(String message) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(MainActivity.this, "Could not load statistics: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (txtStarPoints != null) {
            updateDashboard();
        }
    }
}