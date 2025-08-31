package com.example.rpgapp.model;

import com.google.firebase.firestore.Exclude;

public class Task {

    private String title;
    private String description;
    private String category; // naziv kategorije
    private String color;    // boja kategorije, npr. "#FF0000"
    private String frequency; // "jednokratni" ili "ponavljajući"
    private int interval;    // npr. 1, 2, 3
    private String intervalUnit; // "dan", "nedelja"
    private String startDate;
    private String endDate;
    private String time;     // vreme izvršenja
    private int difficultyXp;
    private int importanceXp;
    private int totalXp;
    private String status;   // "aktivan", "urađen", "otkazan", "pauziran", "neurađen"


    private String dueDate;
    @Exclude
    private String taskId;

    public Task() {}

    public Task(String title, String description, String category, String color, String frequency,
                int interval, String intervalUnit, String startDate, String endDate, String time,
                int difficultyXp, int importanceXp, String status, String dueDate) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.color = color;
        this.frequency = frequency;
        this.interval = interval;
        this.intervalUnit = intervalUnit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.time = time;
        this.difficultyXp = difficultyXp;
        this.importanceXp = importanceXp;
        this.totalXp = difficultyXp + importanceXp;
        this.status = status;
        this.dueDate = dueDate;
    }

    // Getter & Setter metode

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public String getIntervalUnit() { return intervalUnit; }
    public void setIntervalUnit(String intervalUnit) { this.intervalUnit = intervalUnit; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getDifficultyXp() { return difficultyXp; }
    public void setDifficultyXp(int difficultyXp) { this.difficultyXp = difficultyXp; }

    public int getImportanceXp() { return importanceXp; }
    public void setImportanceXp(int importanceXp) { this.importanceXp = importanceXp; }

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) { this.dueDate = dueDate; }


    @Exclude
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

}
