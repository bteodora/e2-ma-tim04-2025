package com.example.rpgapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpecialMission {

    private String missionId;
    private String allianceId; // DODATO
    private int maxBossHP;
    private int bossHP;
    private int userProgress;
    private int allianceProgress;
    private Map<String, Integer> userTaskProgress = new HashMap<>();
    private List<MissionTask> tasks = new ArrayList<>();
    private long startTime;
    private long durationMillis = 14L * 24 * 60 * 60 * 1000; // 2 nedelje
    private boolean isActive;
    private int completedBossCount = 0;


    public SpecialMission() {}

    public SpecialMission(int allianceMemberCount) {
        this.maxBossHP = 100 * allianceMemberCount;
        this.bossHP = maxBossHP;
        this.isActive = true;
        this.startTime = System.currentTimeMillis();
        generateTasks();
    }

    private void generateTasks() {
        tasks.add(new MissionTask("Kupovina u prodavnici", 2, 5*14, 2));
        tasks.add(new MissionTask("Udarac u regularnoj borbi", 2, 10*14, 10));
        tasks.add(new MissionTask("Laki/Normalni/Važni zadaci", 1, 10*14, 10));
        tasks.add(new MissionTask("Ostali zadaci", 4, 6*14, 6));
        tasks.add(new MissionTask("Bez nerešenih zadataka", 10, 1*14, 1));
        tasks.add(new MissionTask("Poruka u savezu", 4, 14*14, 4));
    }

    public void reduceBossHP(int amount) { bossHP = Math.max(0, bossHP - amount); }
    public void increaseUserProgress(String userId, int amount) {
        if (userId == null) return;
        int current = userTaskProgress.getOrDefault(userId, 0);
        userTaskProgress.put(userId, current + amount);
        userProgress += amount; // ukupni progres
    }

    public void increaseAllianceProgress(int amount) { allianceProgress += amount; }
    public void endMission() { isActive = false; }

    // ---------- GETTERS / SETTERS ----------
    public String getMissionId() { return missionId; }
    public void setMissionId(String missionId) { this.missionId = missionId; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public int getBossHP() { return bossHP; }
    public void setBossHP(int bossHP) { this.bossHP = bossHP; }

    public int getMaxBossHP() { return maxBossHP; }
    public void setMaxBossHP(int maxBossHP) { this.maxBossHP = maxBossHP; }

    public int getUserProgress() { return userProgress; }
    public void setUserProgress(int userProgress) { this.userProgress = userProgress; }

    public int getAllianceProgress() { return allianceProgress; }
    public void setAllianceProgress(int allianceProgress) { this.allianceProgress = allianceProgress; }

    public Map<String, Integer> getUserTaskProgress() { return userTaskProgress; }
    public void setUserTaskProgress(Map<String, Integer> userTaskProgress) { this.userTaskProgress = userTaskProgress; }

    public List<MissionTask> getTasks() { return tasks; }
    public void setTasks(List<MissionTask> tasks) { this.tasks = tasks; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public boolean isActive() { return isActive && System.currentTimeMillis() - startTime <= durationMillis; }
    public void setActive(boolean active) { isActive = active; }

    public int getCompletedBossCount() { return completedBossCount; }
    public void incrementCompletedBossCount() { completedBossCount++; }
}
