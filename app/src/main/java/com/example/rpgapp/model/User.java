package com.example.rpgapp.model;

import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class User {

    private String username;
    private String avatarId;
    private int level;
    private String title;
    private long xp;
    private List<String> badges;
    private Map<String, UserItem> userItems;
    private Map<String, UserWeapon> userWeapons;
    private Map<String, UserItem> equipped;

    private int powerPoints;
    private long coins;
    @Exclude
    private String userId;

    private List<String> friendIds;
    private List<String> friendRequests;

    private long registrationTimestamp;

    private String allianceId;
    private List<String> allianceInvites;
    private String fcmToken;


    public void reduceLifespan() {
        userItems.entrySet().removeIf(entry -> {
            UserItem item = entry.getValue();
            if (item.bonusType != BonusType.PERMANENT_PP) {
                item.lifespan--;
                return item.lifespan == 0;
            }
            return false;
        });
    }

    public long getRequiredXpForNextLevel() {
        if (level == 1) {
            return 200;
        }

        double requiredXp = 200;

        for (int i = 3; i <= this.level + 1; i++) {
            requiredXp = requiredXp * 2.5;
            requiredXp = Math.ceil(requiredXp / 100.0) * 100;
        }

        return (long) requiredXp;
    }

    public void addXp(int xp){
        this.xp = this.xp + xp;
        checkLevelUp();
    }

    public int calculatePrizeFormula() {
        return (int) (200 * Math.pow(1.2, level - 1));
    }

    public int calculatePreviosPrizeFormula() {
        if(level == 1){
            return 200;
        }
        return (int) (200 * Math.pow(1.2, level - 2));
    }

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public User() {
        this.equipped = new HashMap<>();
        this.userItems = new HashMap<>();
        this.userWeapons = new HashMap<>();
    }

    public User(String username, String avatarId) {
        this.username = username;
        this.avatarId = avatarId;

        this.level = 1;
        this.title = "Begginer";
        this.xp = 0;
        this.powerPoints = 10;
        this.coins = 0;

        this.badges = null;
        this.userItems = null;
        this.userWeapons = null;
        this.equipped = null;
    }

    public List<String> getFriendIds() {
        return friendIds;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public List<String> getAllianceInvites() {
        return allianceInvites;
    }

    public void setAllianceInvites(List<String> allianceInvites) {
        this.allianceInvites = allianceInvites;
    }

    public void setFriendIds(List<String> friendIds) {
        this.friendIds = friendIds;
    }

    public List<String> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(List<String> friendRequests) {
        this.friendRequests = friendRequests;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public List<String> getBadges() {
        return badges;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges;
    }

    public int getPowerPoints() {
        return powerPoints;
    }

    public void setPowerPoints(int powerPoints) {
        this.powerPoints = powerPoints;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public Map<String, UserItem> getUserItems() {
        return userItems;
    }

    public void setUserItems(Map<String, UserItem> userItems) {
        this.userItems = userItems;
    }

    public Map<String, UserWeapon> getUserWeapons() {
        return userWeapons;
    }

    public void setUserWeapons(Map<String, UserWeapon> userWeapons) {
        this.userWeapons = userWeapons;
    }

    public Map<String, UserItem> getEquipped() {
        return equipped;
    }

    public void setEquipped(Map<String, UserItem> equipped) {
        this.equipped = equipped;
    }

    @Exclude
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int calculateTaskXp(Task task) {
        // XP težine + bitnosti
        return task.getDifficultyXp() + task.getImportanceXp();
    }



    public boolean increaseXp(Task task, List<Task> userTasks) {
        if (task == null) return false;

        int taskXp = calculateTaskXp(task);

        // Trenutni datum
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfWeek = new SimpleDateFormat("yyyy-ww", Locale.getDefault()); // nedelja
        SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        String taskDay = task.getDueDate() != null ? task.getDueDate() : task.getStartDate();
        String taskDayFormatted = taskDay; // assume "yyyy-MM-dd"

        // Brojanje zadataka iste težine i bitnosti u kvoti
        int dailyCount = 0;
        int weeklyCount = 0;
        int monthlyCount = 0;

        Calendar cal = Calendar.getInstance();
        for (Task t : userTasks) {
            if ("urađen".equalsIgnoreCase(t.getStatus())) {
                String tDay = t.getDueDate() != null ? t.getDueDate() : t.getStartDate();
                if (tDay == null) continue;

                try {
                    // dnevna kvota
                    if (sdfDay.format(sdfDay.parse(tDay)).equals(taskDayFormatted) &&
                            t.getDifficultyXp() == task.getDifficultyXp() &&
                            t.getImportanceXp() == task.getImportanceXp()) {
                        dailyCount++;
                    }

                    // nedeljna kvota
                    if (sdfWeek.format(sdfDay.parse(tDay)).equals(sdfWeek.format(sdfDay.parse(taskDayFormatted))) &&
                            t.getDifficultyXp() == task.getDifficultyXp()) {
                        weeklyCount++;
                    }

                    // mesečna kvota
                    if (sdfMonth.format(sdfDay.parse(tDay)).equals(sdfMonth.format(sdfDay.parse(taskDayFormatted))) &&
                            t.getImportanceXp() == 100) { // specijalan
                        monthlyCount++;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Provera kvota
        if ((task.getDifficultyXp() == 1 && task.getImportanceXp() == 1 && dailyCount >= 5) ||
                (task.getDifficultyXp() == 3 && task.getImportanceXp() == 3 && dailyCount >= 5) ||
                (task.getDifficultyXp() == 7 && task.getImportanceXp() == 10 && dailyCount >= 2) ||
                (task.getDifficultyXp() == 20 && weeklyCount >= 1) ||
                (task.getImportanceXp() == 100 && monthlyCount >= 1)) {
            return false; // prekoračeno
        }


        int finalTaskXp = getFinalXpForTask(task);
        addXp(finalTaskXp);
        return true;
    }
    public int checkLevelUp() {
        int totalPpRewardGained = 0;

        while (this.xp >= getRequiredXpForNextLevel()) {
            this.level++;

            int ppRewardForThisLevel = calculatePowerPointsRewardForLevel(this.level);

            this.powerPoints += ppRewardForThisLevel;

            totalPpRewardGained += ppRewardForThisLevel;

            if (this.level >= 4) {
                this.title = "Master";
            } else if (this.level == 3) {
                this.title = "Professional";
            } else if (this.level == 2) {
                this.title = "Intermediate";
            }
        }

        return totalPpRewardGained;
    }

    private int calculatePowerPointsRewardForLevel(int newLevel) {
        if (newLevel < 2) {
            return 0;
        }
        if (newLevel == 2) {
            return 40;
        }

        double reward = 40.0;

        for (int i = 3; i <= newLevel; i++) {
            reward *= 1.75;
        }

        return (int) Math.round(reward);
    }

    private int calculateScaledXp(int baseValue, int userLevel) {
        if (userLevel <= 1) {
            return baseValue;
        }

        double scaledValue = baseValue;
        for (int i = 2; i <= userLevel; i++) {
            scaledValue = scaledValue + (scaledValue / 2.0);
        }

        return (int) Math.round(scaledValue);
    }

    public int getFinalXpForTask(Task task) {
        int scaledDifficultyXp = calculateScaledXp(task.getDifficultyXp(), this.level);
        int scaledImportanceXp = calculateScaledXp(task.getImportanceXp(), this.level);
        return scaledDifficultyXp + scaledImportanceXp;
    }


}