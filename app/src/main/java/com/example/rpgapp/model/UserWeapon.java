package com.example.rpgapp.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class UserWeapon {
    public String weaponId;
    public int level;
    public double currentBoost;
    public BonusType boostType;
    public UserWeapon() {}

    public String getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(String weaponId) {
        this.weaponId = weaponId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getCurrentBoost() {
        return currentBoost;
    }

    public void setCurrentBoost(double currentBoost) {
        this.currentBoost = currentBoost;
    }

    public BonusType getBoostType() {
        return boostType;
    }

    public void setBoostType(BonusType boostType) {
        this.boostType = boostType;
    }

    public String getBoostAsString() {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.####");
        String boostString = df.format(currentBoost);

        switch (boostType) {
            case PERMANENT_PP:
                sb.append("Permanent Power Points: +").append(boostString).append("%");
                break;
            case TEMPORARY_PP:
                sb.append("Temporary Power Points: +").append(boostString).append("%");
                break;
            case ATTACK_NUM:
                sb.append("Additional attack chance: ").append(boostString).append("%");
                break;
            case SUCCESS_PERCENTAGE:
                sb.append("Success Percentage: +").append(boostString).append("%");
                break;
            case MONEY_BOOST:
                sb.append("Money Boost: +").append(boostString).append("%");
                break;
            default:
                sb.append("Unknown boost type");
                break;
        }

        return sb.toString();
    }

    public void upgrade() {
        BigDecimal currentBoostDecimal = BigDecimal.valueOf(this.currentBoost);
        BigDecimal incrementDecimal = new BigDecimal("0.0001");

        BigDecimal newBoostDecimal = currentBoostDecimal.add(incrementDecimal);

        this.currentBoost = newBoostDecimal.doubleValue();

        this.level++;
    }

    public void duplicateWeapon(){
        BigDecimal currentBoostDecimal = BigDecimal.valueOf(this.currentBoost);
        BigDecimal incrementDecimal = new BigDecimal("0.0002");

        BigDecimal newBoostDecimal = currentBoostDecimal.add(incrementDecimal);

        this.currentBoost = newBoostDecimal.doubleValue();
    }


    public UserWeapon(String weaponId, int level) {
        this.weaponId = weaponId;
        this.level = level;
        this.currentBoost = 0;
        this.boostType = BonusType.PERMANENT_PP;
    }

    public int getBonusPP() {
        if(boostType == BonusType.PERMANENT_PP || boostType == BonusType.TEMPORARY_PP) {
            return (int) currentBoost;
        }
        return 0;
    }

}
