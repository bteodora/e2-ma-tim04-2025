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

    private void reduceLifespan() {
        userItems.entrySet().removeIf(entry -> {
            UserItem item = entry.getValue();
            if (item.bonusType != BonusType.PERMANENT_PP) {
                item.lifespan--;
                return item.lifespan == 0;
            }
            return false;
        });
    }


    private void checkIfLevelIncreased() {
        if (xp>=getRequiredXpForNextLevel()){
            level++;
            if(level == 2){
                title = "Intermediate";
            } else if (level == 3) {
                title = "Professional";
            }
        }
    }

    public int getRequiredXpForNextLevel() {
        if (level == 1) {
            return 200;
        }

        int xp = 200;
        for (int i = 2; i <= level; i++) {
            xp = (int) Math.ceil((xp * 2.5) / 100.0) * 100;
        }

        return xp;
    }

    public void addXp(int xp){
        this.xp = this.xp + xp;
        checkIfLevelIncreased();
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
}