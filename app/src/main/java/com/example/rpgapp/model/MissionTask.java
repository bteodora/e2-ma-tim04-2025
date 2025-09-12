package com.example.rpgapp.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MissionTask {
    private String taskId;
    private String name;
    private int hpReductionPerCompletion;
    private int maxCompletions; // ukupni max
    private int dailyMax; // max po danu

    // userId -> ukupni progres
    private Map<String, Integer> userTotalProgress = new HashMap<>();
    // userId -> (datum -> dnevni progres)
    private Map<String, Map<String, Integer>> userDailyProgress = new HashMap<>();
    public MissionTask(){
        this.taskId = UUID.randomUUID().toString();
    }
    public MissionTask(String name, int hpReduction, int maxCompletions, int dailyMax) {
        this.taskId = UUID.randomUUID().toString();
        this.name = name;
        this.hpReductionPerCompletion = hpReduction;
        this.maxCompletions = maxCompletions;
        this.dailyMax = dailyMax;
    }
    public String getTaskId()
    {
        return taskId;
    }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public boolean incrementProgress(String userId) {
        int total = userTotalProgress.getOrDefault(userId, 0);
        if (total >= maxCompletions) return false;

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Map<String, Integer> dailyMap = userDailyProgress.getOrDefault(userId, new HashMap<>());
        int todayProgress = dailyMap.getOrDefault(today, 0);
        if (todayProgress >= dailyMax) return false;

        // povećaj ukupni i dnevni progres
        total++;
        todayProgress++;
        userTotalProgress.put(userId, total);
        dailyMap.put(today, todayProgress);
        userDailyProgress.put(userId, dailyMap);

        return true;
    }

    public boolean isCompleted(String userId) {
        return userTotalProgress.getOrDefault(userId, 0) >= maxCompletions;
    }

    public int getCurrentCompletions(String userId) {
        return userTotalProgress.getOrDefault(userId, 0);
    }

    public int getTodayProgress(String userId) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Map<String, Integer> dailyMap = userDailyProgress.getOrDefault(userId, new HashMap<>());
        return dailyMap.getOrDefault(today, 0);
    }

    public String getName() {
        return name;
    }

    public int getHpReductionPerCompletion() {
        return hpReductionPerCompletion;
    }

    public int getMaxCompletions() {
        return maxCompletions;
    }

    public int getDailyMax() {
        return dailyMax;
    }

    public Map<String, Integer> getUserTotalProgress() {
        return userTotalProgress;
    }

    public Map<String, Map<String, Integer>> getUserDailyProgress() {
        return userDailyProgress;
    }

    public void setName(String name) { this.name = name; }
    public void setHpReductionPerCompletion(int hpReductionPerCompletion) { this.hpReductionPerCompletion = hpReductionPerCompletion; }
    public void setMaxCompletions(int maxCompletions) { this.maxCompletions = maxCompletions; }
    public void setDailyMax(int dailyMax) { this.dailyMax = dailyMax; }
    public void setUserTotalProgress(Map<String, Integer> userTotalProgress) { this.userTotalProgress = userTotalProgress; }
    public void setUserDailyProgress(Map<String, Map<String, Integer>> userDailyProgress) { this.userDailyProgress = userDailyProgress; }


    public MissionTask deepCopy() {
        MissionTask copy = new MissionTask();
        copy.setName(this.name);
        copy.setHpReductionPerCompletion(this.hpReductionPerCompletion);
        copy.setMaxCompletions(this.maxCompletions);
        copy.setDailyMax(this.dailyMax);

        // Kopiranje userTotalProgress
        copy.setUserTotalProgress(new HashMap<>(this.userTotalProgress));

        // Kopiranje userDailyProgress
        Map<String, Map<String, Integer>> dailyCopy = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : this.userDailyProgress.entrySet()) {
            dailyCopy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        copy.setUserDailyProgress(dailyCopy);

        return copy;
    }

}