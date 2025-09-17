package com.example.rpgapp.model;

import com.example.rpgapp.model.BonusType;
import com.example.rpgapp.model.Boss;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.model.UserWeapon;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Battle {

    private String battleId;
    private String userId;
    private Boss boss;
    private int remainingAttacks;
    private boolean finished;

    private Map<String, UserItem> activeItems;   // odeća / oprema korišćena u borbi
    private UserWeapon activeWeapon;             // trenutno korišćeno oružje

    private int successRate;                     // šansa da napad pogodi
    private int coinsEarned;
    private boolean droppedItem;
    private Map<String, UserItem> droppedItems; // dobijena odeća
    private UserWeapon droppedWeapon;           // dobijeno oružje

    private boolean usedShakeAttack;            // da li je napad bio shake
    private int bossHpBeforeFight;

    private transient User user; // transient da Firestore/SQLite ne pokušava direktno da sačuva

    public Battle() {} // prazni konstruktor za Firestore

    public Battle(User user, int bossLevel, int successRate) {
        this.battleId = UUID.randomUUID().toString();
        this.user = user;
        this.userId = user.getUserId();
        this.boss = new Boss(bossLevel);
        this.remainingAttacks = 5;
        this.finished = false;
        this.successRate = successRate;
        this.bossHpBeforeFight = boss.getMaxHp();
    }

    // --- GET/SET ---
    public String getBattleId() { return battleId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) {
        this.userId=userId;
    }
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getUserId();
        }
    }

    public Boss getBoss() { return boss; }
    public int getRemainingAttacks() { return remainingAttacks; }
    public boolean isFinished() { return finished; }

    public Battle(User user, Boss existingBoss) {
        this.user = user;
        this.boss = existingBoss; // HP i level se zadržavaju
        this.successRate =  successRate;
        this.remainingAttacks = 5; // reset napada
        this.finished = false;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    public void setRemainingAttacks(int remainingAttacks) {

        this.remainingAttacks = remainingAttacks;
    }



    public Map<String, UserItem> getActiveItems() { return activeItems; }
    public void setActiveItems(Map<String, UserItem> activeItems) { this.activeItems = activeItems; }

    public UserWeapon getActiveWeapon() { return activeWeapon; }
    public void setActiveWeapon(UserWeapon activeWeapon) { this.activeWeapon = activeWeapon; }

    public int getSuccessRate() { return successRate; }
    public void setSuccessRate(int successRate) { this.successRate = successRate; }

    public int getCoinsEarned() { return coinsEarned; }
    public boolean isDroppedItem() { return droppedItem; }
    public Map<String, UserItem> getDroppedItems() { return droppedItems; }
    public UserWeapon getDroppedWeapon() { return droppedWeapon; }

    public boolean isUsedShakeAttack() { return usedShakeAttack; }
    public void setUsedShakeAttack(boolean usedShakeAttack) { this.usedShakeAttack = usedShakeAttack; }

    public int getBossHpBeforeFight() { return bossHpBeforeFight; }

    // --- JSON serijalizacija za map i weapon ---
    public String activeItemsToJson() {
        return new Gson().toJson(activeItems);
    }
    public void setActiveItemsFromJson(String json) {
        Type type = new TypeToken<Map<String, UserItem>>(){}.getType();
        this.activeItems = new Gson().fromJson(json, type);
    }

    public String activeWeaponToJson() {
        return new Gson().toJson(activeWeapon);
    }
    public void setActiveWeaponFromJson(String json) {
        this.activeWeapon = new Gson().fromJson(json, UserWeapon.class);
    }

    public String droppedItemsToJson() {
        return new Gson().toJson(droppedItems);
    }
    public void setDroppedItemsFromJson(String json) {
        Type type = new TypeToken<Map<String, UserItem>>(){}.getType();
        this.droppedItems = new Gson().fromJson(json, type);
    }

    public String droppedWeaponToJson() {
        return new Gson().toJson(droppedWeapon);
    }
    public void setDroppedWeaponFromJson(String json) {
        this.droppedWeapon = new Gson().fromJson(json, UserWeapon.class);
    }

    // --- BORBA ---
    public boolean attack() {
        if (remainingAttacks <= 0 || finished) return false;

        Random random = new Random();
        int chance = random.nextInt(100);
        boolean hit = chance < successRate;

        if (hit) {
            boss.reduceHp(calculateTotalPP());

        }

        remainingAttacks--;

        if (remainingAttacks == 0 || boss.isDefeated()) {
            finished = true;

            finalizeBattle();
        }

        return hit;
    }

    private int calculateTotalPP() {
        if (user == null) return 0; // zaštita

        int totalPP = user.getPowerPoints();

        if(activeItems != null) {
            for(UserItem item : activeItems.values()){
                if(item.getBonusType() == BonusType.PERMANENT_PP || item.getBonusType() == BonusType.TEMPORARY_PP){
                    totalPP += (int)item.getCurrentBonus();
                }
            }
        }

        if(activeWeapon != null) {
            totalPP += activeWeapon.getBonusPP();
        }

        return totalPP;
    }



    // --- NAGRADE ---
    private void finalizeBattle() {
        int baseCoins = 200;
        int bossLevel = boss.getLevel();

        // Ako je bos poražen
        if (boss.isDefeated()) {
            coinsEarned = (int)(baseCoins * Math.pow(1.2, bossLevel - 1));
            droppedItem = dropEquipment();

            // level up bossa prema zadatoj formuli


            //TODO
        } else if ((boss.getMaxHp() - boss.getCurrentHp()) >= boss.getMaxHp() / 2) {
            // ako je umanjeno 50% HP-a
            coinsEarned = (int)((baseCoins * Math.pow(1.2, bossLevel - 1)) / 2);
            droppedItem = dropEquipment() && new Random().nextBoolean(); // 50% šansa
        } else {
            coinsEarned = 0;
            droppedItem = false;
        }

    }

    private boolean dropEquipment() {
        Random rand = new Random();
        int chance = rand.nextInt(100);
        if (chance < 20) { // 20% šansa za drop
            int typeChance = rand.nextInt(100);
            if(typeChance < 95) {
                // odeća
                droppedItems = Map.of("newCloth", new UserItem("New Cloth", BonusType.PERMANENT_PP, 5));
            } else {
                // oružje
                droppedWeapon = new UserWeapon("New Weapon", 10);
            }
            return true;
        }
        return false;
    }

    //  za dobijanje nagrada coina ---
    public int calculateCoins() {
        int baseCoins = 200;
        int bossLevel = boss.getLevel();

        if (boss.isDefeated()) {
            // Bos mrtav -> puna nagrada
            coinsEarned = (int)(baseCoins * Math.pow(1.2, bossLevel - 1));
        } else if (boss.getMaxHp() <= boss.getCurrentHp() / 2) {
            // Bos preživeo ali je skinuto > 50% HP -> pola nagrade
            coinsEarned = (int)((baseCoins * Math.pow(1.2, bossLevel - 1)) / 2);
        } else {
            // Bos živ i nije skinuto 50% -> nema nagrade
            coinsEarned = 0;
        }
        return coinsEarned;
    }



}