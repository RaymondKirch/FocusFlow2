package com.example.focusflow;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import java.text.DateFormat;
import java.util.Date;

public class StudyHistoryActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://focus-flow-90c9c-default-rtdb.europe-west1.firebasedatabase.app";

    private LinearLayout historyContainer;
    private TextView txtHistoryStatus;
    private Button btnRefresh;

    private DatabaseReference userRef;
    private boolean loading = false;

    private static class Session {
        String id;
        int plannedMinutes;
        int actualMinutes;
        int pointsEarned;
        long startedAt;
        long completedAt;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_history);

        historyContainer = findViewById(R.id.historyContainer);
        txtHistoryStatus = findViewById(R.id.txtHistoryStatus);
        btnRefresh = findViewById(R.id.btnRefreshHistory);

        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadHistory());

        StudyDataManager.ensureSignedIn(
                new StudyDataManager.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;

                        String uid = FirebaseAuth.getInstance()
                                .getCurrentUser().getUid();

                        userRef = FirebaseDatabase.getInstance(DATABASE_URL)
                                .getReference("users").child(uid);

                        loadHistory();
                    }

                    @Override
                    public void onFailure(String message) {
                        showError("Could not connect to your account: " + message);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (userRef != null) {
            loadHistory();
        }
    }

    private void loadHistory() {
        if (userRef == null || loading) return;

        loading = true;
        btnRefresh.setEnabled(false);
        txtHistoryStatus.setText("Loading study history...");
        historyContainer.removeAllViews();

        userRef.child("studySessions").get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    loading = false;
                    btnRefresh.setEnabled(true);
                    historyContainer.removeAllViews();

                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        txtHistoryStatus.setText(
                                "No completed study sessions yet.");
                        return;
                    }

                    txtHistoryStatus.setText(
                            "Your completed study sessions:");

                    for (DataSnapshot item : snapshot.getChildren()) {
                        Session session = new Session();
                        session.id = item.getKey();
                        session.plannedMinutes =
                                readInt(item, "plannedMinutes", 0);
                        session.actualMinutes =
                                readInt(item, "actualMinutes", 0);
                        session.pointsEarned =
                                readInt(item, "pointsEarned", 5);
                        session.startedAt =
                                readLong(item, "startedAt", 0);
                        session.completedAt =
                                readLong(item, "completedAt", 0);

                        addSessionCard(session);
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;

                    loading = false;
                    btnRefresh.setEnabled(true);
                    showError("Could not load history: "
                            + error.getMessage());
                });
    }

    private int readInt(DataSnapshot snapshot, String key, int fallback) {
        Object value = snapshot.child(key).getValue();

        return value instanceof Number
                ? ((Number) value).intValue() : fallback;
    }

    private long readLong(DataSnapshot snapshot, String key, long fallback) {
        Object value = snapshot.child(key).getValue();

        return value instanceof Number
                ? ((Number) value).longValue() : fallback;
    }

    private void addSessionCard(Session session) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        card.setElevation(2);
        historyContainer.addView(card, cardParams);

        TextView title = new TextView(this);
        title.setText("Study session");
        title.setTextColor(0xFF24213A);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView details = new TextView(this);
        details.setText(
                "Planned: " + session.plannedMinutes + " minutes\n"
                        + "Actual: " + session.actualMinutes + " minutes\n"
                        + "Points earned: " + session.pointsEarned + "\n"
                        + "Started: " + formatDate(session.startedAt) + "\n"
                        + "Completed: " + formatDate(session.completedAt));
        details.setTextColor(0xFF6F6B80);
        details.setTextSize(14);
        details.setPadding(0, 8, 0, 12);
        card.addView(details);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        card.addView(actions);

        Button editButton = new Button(this);
        editButton.setText("Edit");
        editButton.setAllCaps(false);
        actions.addView(editButton);

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setAllCaps(false);
        actions.addView(deleteButton);

        editButton.setOnClickListener(v -> editSession(session));
        deleteButton.setOnClickListener(v -> confirmDelete(session));
    }

    private String formatDate(long millis) {
        if (millis <= 0) return "Not available";
        return DateFormat.getDateTimeInstance().format(new Date(millis));
    }

    private void editSession(Session session) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 8, 24, 0);

        EditText plannedInput = new EditText(this);
        plannedInput.setHint("Planned minutes");
        plannedInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        plannedInput.setText(String.valueOf(session.plannedMinutes));
        form.addView(plannedInput);

        EditText actualInput = new EditText(this);
        actualInput.setHint("Actual minutes");
        actualInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        actualInput.setText(String.valueOf(session.actualMinutes));
        form.addView(actualInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit study session")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(unused ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            int planned;
                            int actual;

                            try {
                                planned = Integer.parseInt(
                                        plannedInput.getText().toString().trim());
                                actual = Integer.parseInt(
                                        actualInput.getText().toString().trim());
                            } catch (NumberFormatException error) {
                                plannedInput.setError("Enter valid minutes.");
                                return;
                            }

                            if (planned < 1 || planned > 240) {
                                plannedInput.setError(
                                        "Enter 1 to 240 minutes.");
                                return;
                            }

                            if (actual < 1 || actual > 1440) {
                                actualInput.setError(
                                        "Enter 1 to 1440 minutes.");
                                return;
                            }

                            saveEditedSession(session, planned, actual, dialog);
                        }));

        dialog.show();
    }

    private void saveEditedSession(
            Session session, int planned, int actual, AlertDialog dialog) {

        userRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData data) {
                MutableData record = data.child("studySessions")
                        .child(session.id);

                if (record.getValue() == null) {
                    return Transaction.abort();
                }

                MutableData stats = data.child("stats");

                long oldMinutes = number(record, "actualMinutes");
                long totalMinutes = number(stats, "totalStudyMinutes");

                record.child("plannedMinutes").setValue(planned);
                record.child("actualMinutes").setValue(actual);

                stats.child("totalStudyMinutes").setValue(
                        Math.max(0L, totalMinutes - oldMinutes + actual));

                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (error == null && committed) {
                    dialog.dismiss();
                    Toast.makeText(StudyHistoryActivity.this,
                            "Study session updated.",
                            Toast.LENGTH_SHORT).show();
                    loadHistory();
                } else {
                    Toast.makeText(StudyHistoryActivity.this,
                            error != null ? error.getMessage()
                                    : "The session no longer exists.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void confirmDelete(Session session) {
        new AlertDialog.Builder(this)
                .setTitle("Delete study session?")
                .setMessage("This removes the session and adjusts its recorded "
                        + "study minutes, completion count and session rewards.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete",
                        (dialog, which) -> deleteSession(session))
                .show();
    }

    private void deleteSession(Session session) {
        userRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData data) {
                MutableData sessions = data.child("studySessions");
                MutableData record = sessions.child(session.id);

                if (record.getValue() == null) {
                    return Transaction.abort();
                }

                MutableData stats = data.child("stats");

                long actualMinutes = number(record, "actualMinutes");
                long pointsEarned = number(record, "pointsEarned");
                long totalMinutes = number(stats, "totalStudyMinutes");
                long totalSessions = number(stats, "completedSessions");
                long stars = number(stats, "starPoints");

                stats.child("totalStudyMinutes").setValue(
                        Math.max(0L, totalMinutes - actualMinutes));

                stats.child("completedSessions").setValue(
                        Math.max(0L, totalSessions - 1));

                // Remove completion points and the associated clock-in reward.
                stats.child("starPoints").setValue(
                        Math.max(0L, stars - pointsEarned - 5L));

                record.setValue(null);

                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (error == null && committed) {
                    Toast.makeText(StudyHistoryActivity.this,
                            "Study session deleted.",
                            Toast.LENGTH_SHORT).show();
                    loadHistory();
                } else {
                    Toast.makeText(StudyHistoryActivity.this,
                            error != null ? error.getMessage()
                                    : "The session no longer exists.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private long number(MutableData data, String key) {
        Object value = data.child(key).getValue();
        return value instanceof Number
                ? ((Number) value).longValue() : 0L;
    }

    private void showError(String message) {
        if (isFinishing() || isDestroyed()) return;

        txtHistoryStatus.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}