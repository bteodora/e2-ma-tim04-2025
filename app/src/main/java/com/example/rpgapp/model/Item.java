package com.example.rpgapp.model;

import java.util.Map;
import java.util.Random;

public class Item {
    private String id;
    private String name;
    private String description;

    private ItemType type;
    private double price_percentage; //npr 20% -> cena je 20% boss money
    private int lifespan; // za brojanje koliko dugo se koristi nesto
                          // posle borbe sa bosom, svima se smanji za -1, a brisu se oni s nulom iz onih koji su kupljeni
    private BonusType bonusType;
    private double bonusValue;
    private double bonusValueBase; // osnovna vrednost
    private  String image;

    public int calculatePrice(int bossMoney){
        return (int) price_percentage*bossMoney;
    }

    public void duplicateChlothes(){
        bonusValue = bonusValue + bonusValueBase;
    }

    public boolean isBonusTriggered() { // za cizme - da li je dobijen bonus napad
        double roll = new Random().nextDouble();
        return roll < this.bonusValue;
    }

    public Item() {    }

    public Item(String id, String name, String description, ItemType type, double price_percentage, int lifespan, BonusType bonusType, double bonusValue, String image) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.price_percentage = price_percentage;
        this.lifespan = lifespan;
        this.bonusType = bonusType;
        this.bonusValue = bonusValue;
        this.image = image;
    }

    public double getBonusValueBase() {
        return bonusValueBase;
    }

    public void setBonusValueBase(double bonusValueBase) {
        this.bonusValueBase = bonusValueBase;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public double getPrice_percentage() {
        return price_percentage;
    }

    public void setPrice_percentage(double price_percentage) {
        this.price_percentage = price_percentage;
    }

    public int getLifespan() {
        return lifespan;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public BonusType getBonusType() {
        return bonusType;
    }

    public void setBonusType(BonusType bonusType) {
        this.bonusType = bonusType;
    }

    public double getBonusValue() {
        return bonusValue;
    }

    public void setBonusValue(double bonusValue) {
        this.bonusValue = bonusValue;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
