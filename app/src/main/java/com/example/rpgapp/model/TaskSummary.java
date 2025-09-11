package com.example.rpgapp.model;

import com.github.mikephil.charting.data.PieData;

public class TaskSummary {
    public final PieData pieData;
    public final int totalTasks;

    public TaskSummary(PieData pieData, int totalTasks) {
        this.pieData = pieData;
        this.totalTasks = totalTasks;
    }
}