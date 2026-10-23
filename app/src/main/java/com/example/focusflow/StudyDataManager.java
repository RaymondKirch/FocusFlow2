package com.example.focusflow;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import java.util.HashMap;
import java.util.Map;

public class StudyDataManager {

    private static final String DATABASE_URL =
            "https://focus-flow-90c9c-default-rtdb.europe-west1.firebasedatabase.app";

    public interface ResultCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public interface StatsCallback {
        void onLoaded(int stars, int minutes, int sessions);
        void onFailure(String message);
    }

    public interface SettingsCallback {
        void onLoaded(int duration, boolean reminders);
        void onFailure(String message);
    }

    public interface ReminderSettingsCallback {
        void onLoaded(
                int duration,
                boolean reminders,
                int hour,
                int minute
        );

        void onFailure(String message);
    }

    private static DatabaseReference root() {
        return FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference();
    }

    private static DatabaseReference userRef() {
        return root().child("users")
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid());
    }

    public static void ensureSignedIn(ResultCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            callback.onSuccess();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error ->
                        callback.onFailure(error.getMessage()));
    }

    private static long number(MutableData data, String key) {
        Object value = data.child(key).getValue();

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return 0L;
    }

    private static int readInt(DataSnapshot data, String key,
                               int defaultValue) {
        Object value = data.child(key).getValue();

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    public static void addClockInReward(ResultCallback callback) {
        ensureSignedIn(new ResultCallback() {
            @Override
            public void onSuccess() {
                updateStats(5, 0, 0, callback);
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }

    private static void updateStats(
            int points,
            int minutes,
            int sessions,
            ResultCallback callback) {

        userRef().child("stats").runTransaction(
                new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(
                            MutableData data) {

                        long stars = number(data, "starPoints");
                        long totalMinutes =
                                number(data, "totalStudyMinutes");
                        long totalSessions =
                                number(data, "completedSessions");

                        data.child("starPoints")
                                .setValue(stars + points);

                        data.child("totalStudyMinutes")
                                .setValue(totalMinutes + minutes);

                        data.child("completedSessions")
                                .setValue(totalSessions + sessions);

                        return Transaction.success(data);
                    }

                    @Override
                    public void onComplete(
                            DatabaseError error,
                            boolean committed,
                            DataSnapshot snapshot) {

                        if (error != null) {
                            callback.onFailure(error.getMessage());
                        } else if (committed) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure(
                                    "Could not save study statistics.");
                        }
                    }
                });
    }

    public static void saveCompletedSession(
            int plannedMinutes,
            int actualMinutes,
            long startedAt,
            long completedAt,
            ResultCallback callback) {

        ensureSignedIn(new ResultCallback() {
            @Override
            public void onSuccess() {
                DatabaseReference user = userRef();

                String sessionId = user.child("studySessions")
                        .push().getKey();

                if (sessionId == null) {
                    callback.onFailure(
                            "Could not create a session ID.");
                    return;
                }

                user.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(
                            MutableData data) {

                        MutableData stats = data.child("stats");

                        long stars = number(stats, "starPoints");
                        long minutes =
                                number(stats, "totalStudyMinutes");
                        long sessions =
                                number(stats, "completedSessions");

                        stats.child("starPoints").setValue(stars + 5);

                        stats.child("totalStudyMinutes")
                                .setValue(minutes + actualMinutes);

                        stats.child("completedSessions")
                                .setValue(sessions + 1);

                        MutableData session = data
                                .child("studySessions")
                                .child(sessionId);

                        session.child("plannedMinutes")
                                .setValue(plannedMinutes);

                        session.child("actualMinutes")
                                .setValue(actualMinutes);

                        session.child("startedAt").setValue(startedAt);
                        session.child("completedAt").setValue(completedAt);
                        session.child("pointsEarned").setValue(5);
                        session.child("status").setValue("completed");

                        return Transaction.success(data);
                    }

                    @Override
                    public void onComplete(
                            DatabaseError error,
                            boolean committed,
                            DataSnapshot snapshot) {

                        if (error != null) {
                            callback.onFailure(error.getMessage());
                        } else if (committed) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure(
                                    "Could not save completed session.");
                        }
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }

    public static void loadStats(StatsCallback callback) {
        ensureSignedIn(new ResultCallback() {
            @Override
            public void onSuccess() {
                userRef().child("stats").get()
                        .addOnSuccessListener(data -> {
                            int stars = readInt(
                                    data, "starPoints", 0);

                            int minutes = readInt(
                                    data, "totalStudyMinutes", 0);

                            int sessions = readInt(
                                    data, "completedSessions", 0);

                            callback.onLoaded(
                                    stars, minutes, sessions);
                        })
                        .addOnFailureListener(error ->
                                callback.onFailure(error.getMessage()));
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }

    public static void loadSettings(SettingsCallback callback) {
        ensureSignedIn(new ResultCallback() {
            @Override
            public void onSuccess() {
                userRef().child("settings").get()
                        .addOnSuccessListener(data -> {
                            int duration = readInt(
                                    data,
                                    "preferredDurationMinutes",
                                    60);

                            Boolean reminders = data.child(
                                            "remindersEnabled")
                                    .getValue(Boolean.class);

                            callback.onLoaded(
                                    duration,
                                    reminders == null || reminders);
                        })
                        .addOnFailureListener(error ->
                                callback.onFailure(error.getMessage()));
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }

    public static void loadReminderSettings(
            ReminderSettingsCallback callback) {

        ensureSignedIn(new ResultCallback() {
            @Override
            public void onSuccess() {
                userRef().child("settings").get()
                        .addOnSuccessListener(data -> {
                            int duration = readInt(
                                    data,
                                    "preferredDurationMinutes",
                                    60);

                            Boolean reminders = data.child(
                                            "remindersEnabled")
                                    .getValue(Boolean.class);

                            int hour = readInt(
                                    data, "reminderHour", 9);

                            int minute = readInt(
                                    data, "reminderMinute", 0);

                            callback.onLoaded(
                                    duration,
                                    reminders == null || reminders,
                                    hour,
                                    minute);
                        })
                        .addOnFailureListener(error ->
                                callback.onFailure(error.getMessage()));
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }

    public static void saveSettings(
            int duration,
            boolean reminders,
            ResultCallback callback) {

        saveSettings(duration, reminders, 9, 0, callback);
    }

    public static void saveSettings(
            int duration,
            boolean reminders,
            int hour,
            int minute,
            ResultCallback callback) {

        ensureSignedIn(new ResultCallback() {
            @Override
            public void onSuccess() {
                Map<String, Object> values = new HashMap<>();

                values.put("preferredDurationMinutes", duration);
                values.put("remindersEnabled", reminders);
                values.put("reminderHour", hour);
                values.put("reminderMinute", minute);

                userRef().child("settings").updateChildren(values)
                        .addOnSuccessListener(unused ->
                                callback.onSuccess())
                        .addOnFailureListener(error ->
                                callback.onFailure(error.getMessage()));
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }
}