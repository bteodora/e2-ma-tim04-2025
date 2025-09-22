package com.example.rpgapp.model;

import java.util.Random;
import java.util.UUID;

public class EquippedItem {
    private String instanceId;
    private String itemId;
    private int lifespan;
    private double currentBonus;
    private BonusType bonusType;
    private boolean isDuplicated;
    public EquippedItem() {
        if (this.instanceId == null) {
            this.instanceId = UUID.randomUUID().toString();
        }
    }

    public EquippedItem(String itemId, int lifespan, double currentBonus, BonusType bonusType) {
        this();
        this.itemId = itemId;
        this.lifespan = lifespan;
        this.currentBonus = currentBonus;
        this.bonusType = bonusType;
        this.isDuplicated = false;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getLifespan() {
        return lifespan;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public double getCurrentBonus() {
        return currentBonus;
    }

    public void setCurrentBonus(double currentBonus) {
        this.currentBonus = currentBonus;
    }

    public BonusType getBonusType() {
        return bonusType;
    }

    public void setBonusType(BonusType bonusType) {
        this.bonusType = bonusType;
    }

    public boolean isDuplicated() {
        return isDuplicated;
    }

    public void setDuplicated(boolean duplicated) {
        isDuplicated = duplicated;
    }

    public void duplicateBonus() {
        if (!isDuplicated) {
            this.currentBonus = this.currentBonus * 2;
            this.isDuplicated = true;
        }
    }
    public boolean isBonusTriggered() { // za cizme - da li je dobijen bonus napad
        double roll = new Random().nextDouble();
        return roll < this.currentBonus;
    }
}