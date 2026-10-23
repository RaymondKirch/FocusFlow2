package com.example.focusflow;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TasksActivity extends AppCompatActivity {

    private EditText edtTask;
    private LinearLayout taskList;
    private TextView txtEmpty;

    private FirebaseAuth auth;
    private DatabaseReference tasksRef;
    private ValueEventListener tasksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        edtTask = findViewById(R.id.edtTask);
        taskList = findViewById(R.id.taskList);
        txtEmpty = findViewById(R.id.txtEmpty);

        Button btnAddTask = findViewById(R.id.btnAddTask);
        Button btnBack = findViewById(R.id.btnBack);

        auth = FirebaseAuth.getInstance();

        btnAddTask.setOnClickListener(v -> addTask());
        btnBack.setOnClickListener(v -> finish());

        signInAndLoadTasks();
    }

    private void signInAndLoadTasks() {
        if (auth.getCurrentUser() != null) {
            setupTasksReference();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> setupTasksReference())
                .addOnFailureListener(error -> {
                    Toast.makeText(this,
                            "Sign-in failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setupTasksReference() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        tasksRef = FirebaseDatabase.getInstance(
                "https://focus-flow-90c9c-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("tasks").child(uid);

        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isFinishing() && !isDestroyed()) {
                    displayTasks(snapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TasksActivity.this,
                        "Could not load tasks: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        tasksRef.addValueEventListener(tasksListener);
    }

    private void addTask() {
        String title = edtTask.getText().toString().trim();

        if (title.isEmpty()) {
            edtTask.setError("Enter a task name.");
            return;
        }

        if (tasksRef == null) {
            Toast.makeText(this,
                    "Please wait for Firebase sign-in.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = tasksRef.push().getKey();

        if (taskId == null) {
            Toast.makeText(this,
                    "Could not create task.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Object> task = new java.util.HashMap<>();
        task.put("title", title);
        task.put("completed", false);

        tasksRef.child(taskId).setValue(task)
                .addOnSuccessListener(unused -> {
                    edtTask.setText("");
                    Toast.makeText(this, "Task saved.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this,
                                "Could not save task: " + error.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void displayTasks(DataSnapshot snapshot) {
        taskList.removeAllViews();

        txtEmpty.setVisibility(snapshot.hasChildren()
                ? View.GONE : View.VISIBLE);

        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
            String taskId = taskSnapshot.getKey();
            String title = taskSnapshot.child("title")
                    .getValue(String.class);
            Boolean completedValue = taskSnapshot.child("completed")
                    .getValue(Boolean.class);

            if (taskId == null || title == null) {
                continue;
            }

            boolean completed = Boolean.TRUE.equals(completedValue);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(16, 12, 16, 12);
            row.setBackgroundColor(0xFFFFFFFF);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

            rowParams.setMargins(0, 0, 0, 12);
            row.setLayoutParams(rowParams);

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(title);
            checkBox.setTextSize(16);
            checkBox.setTextColor(0xFF24213A);
            checkBox.setChecked(completed);

            checkBox.setOnCheckedChangeListener((button, checked) -> {
                tasksRef.child(taskId).child("completed")
                        .setValue(checked)
                        .addOnFailureListener(error ->
                                Toast.makeText(this,
                                        "Could not update task.",
                                        Toast.LENGTH_SHORT).show());
            });

            row.addView(checkBox);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.END);

            Button btnEdit = new Button(this);
            btnEdit.setText("Edit");
            btnEdit.setAllCaps(false);
            btnEdit.setOnClickListener(v ->
                    editTask(taskId, title));

            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setAllCaps(false);
            btnDelete.setOnClickListener(v ->
                    confirmDelete(taskId, title));

            actions.addView(btnEdit);
            actions.addView(btnDelete);
            row.addView(actions);

            taskList.addView(row);
        }
    }

    private void editTask(String taskId, String currentTitle) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentTitle);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Edit task")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String updated =
                            input.getText().toString().trim();

                    if (updated.isEmpty()) {
                        Toast.makeText(this,
                                "Task name cannot be empty.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    tasksRef.child(taskId).child("title")
                            .setValue(updated)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Task updated.",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(error ->
                                    Toast.makeText(this,
                                            "Could not update task.",
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(String taskId, String title) {
        new AlertDialog.Builder(this)
                .setTitle("Delete task?")
                .setMessage("Remove \"" + title + "\"?")
                .setPositiveButton("Delete", (dialog, which) ->
                        tasksRef.child(taskId).removeValue()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this,
                                                "Task deleted.",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(error ->
                                        Toast.makeText(this,
                                                "Could not delete task.",
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (tasksRef != null && tasksListener != null) {
            tasksRef.removeEventListener(tasksListener);
        }

        super.onDestroy();
    }
}