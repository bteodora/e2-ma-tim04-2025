package com.example.rpgapp.model;

public class Category {

    private int id;
    private String name;
    private String color; // npr. "#FF5722"

    public Category() {}

    public Category(int id,String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
