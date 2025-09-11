package com.example.rpgapp.model;

import java.math.BigDecimal;

public class UserItem {
    public String itemId;
    public int lifespan;

    public boolean isDuplicated;
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

    public void duplicateBonus(){
        if(!isDuplicated){
            this.currentBonus = this.currentBonus*2;
            this.isDuplicated = true;
        }
    }

    public boolean isDuplicated() {
        return isDuplicated;
    }

    public void setDuplicated(boolean duplicated) {
        isDuplicated = duplicated;
    }
    
    public UserItem(String itemId, BonusType bonusType, double currentBonus) {
        this.itemId = itemId;
        this.bonusType = bonusType;
        this.currentBonus = currentBonus;
        this.lifespan = 1;
        this.quantity = 1;
    }

}
