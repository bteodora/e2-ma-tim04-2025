package com.example.rpgapp.model;

public class Boss {
    private int level;
    private int maxHp;
    private int currentHp;

    public Boss(){}

    public Boss(int level) {
        this.level = level;
        this.maxHp = calculateBossHp(level);
        this.currentHp = maxHp;
    }

    public void levelUp() {
        this.level++;
        this.maxHp = this.maxHp * 2 + this.maxHp / 2; // formula ×2.5
        this.currentHp = this.maxHp; // reset HP na full
    }


    private int calculateBossHp(int level) {
        int hp = 200;
        for (int i = 2; i <= level; i++) {
            hp = hp * 2 + hp / 2;
        }
        return hp;
    }
    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(hp, maxHp));
    }


    public int getLevel() { return level; }
    public int getMaxHp() { return maxHp; }
    public int getCurrentHp() { return currentHp; }

    public void reduceHp(int amount) {
        currentHp -= amount;
        if (currentHp < 0) currentHp = 0;
    }

    public boolean isDefeated() {
        return currentHp == 0;
    }
}