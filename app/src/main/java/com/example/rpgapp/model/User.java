package com.example.rpgapp.model;

import com.google.firebase.firestore.Exclude;
import java.util.List;
import java.util.Map;

public class User {

    private String username;
    private String avatarId;
    private int level;
    private String title;
    private long xp;
    private List<String> badges;
    private Map<String, String> equippedItems;

    private int powerPoints;
    private long coins;
    private List<String> inventory;

    @Exclude
    private String userId;

    private long registrationTimestamp;

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public User() {}

    public User(String username, String avatarId) {
        this.username = username;
        this.avatarId = avatarId;

        this.level = 1;
        this.title = "Begginer";
        this.xp = 0;
        this.powerPoints = 10;
        this.coins = 0;

        this.badges = null;
        this.equippedItems = null;
        this.inventory = null;
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

    public Map<String, String> getEquippedItems() {
        return equippedItems;
    }

    public void setEquippedItems(Map<String, String> equippedItems) {
        this.equippedItems = equippedItems;
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

    public List<String> getInventory() {
        return inventory;
    }

    public void setInventory(List<String> inventory) {
        this.inventory = inventory;
    }

    // Specijalni getter i setter za UserId koji se ne čuva u bazi
    @Exclude
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}