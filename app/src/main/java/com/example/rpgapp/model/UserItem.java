package com.example.rpgapp.model;

public class UserItem {
    public String itemId;
    public int lifespan;
    public double currentBonus;
    public BonusType bonusType;
    public int quantity;

    public UserItem() {}

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public UserItem(String itemId, BonusType bonusType, double currentBonus) {
        this.itemId = itemId;
        this.bonusType = bonusType;
        this.currentBonus = currentBonus;
        this.lifespan = 1;
        this.quantity = 1;
    }

}
