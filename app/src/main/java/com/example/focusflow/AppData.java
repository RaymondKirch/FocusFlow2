package com.example.focusflow;

import java.util.ArrayList;

public class AppData {
    public static class TaskItem {
        public String title;
        public boolean completed;

        public TaskItem(String title) {

            this.title = title;
            this.completed = false;
        }
    }

    public static ArrayList<TaskItem> tasks = new ArrayList<>();

    public static int starPoints = 0;
    public static int totalStudyMinutes = 0;
    public static int completedSessions = 0;

    public static int preferredDurationMinutes = 60;
    public static boolean remindersEnabled = true;

    public static void addPoints(int points) {
        starPoints += points;
    }
}