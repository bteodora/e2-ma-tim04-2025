package com.example.rpgapp.model;

public class EquipmentDisplay {
    private String id;
    private String name;
    private String description;
    private double boost;
    private int imageResId;

    public EquipmentDisplay(String id, String name, String description, double boost, int imageResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.boost = boost;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getBoost() { return boost; }
    public int getImageResId() { return imageResId; }
}

