package com.example.rpgapp.model;

public class Weapon {
    private String id;
    private String name;
    private String description;
    private double boost; // krece od 0,05
    private BonusType boost_type;
    private int level;
    private  String image;

    public int getUpgradePrice(int bossMoney){
        return (int) (0.6*bossMoney);
    }

    public String getBoostAsString() {
        StringBuilder sb = new StringBuilder();

        switch (boost_type) {
            case PERMANENT_PP:
                sb.append("Permanent Power Points: +").append(boost);
                break;
            case TEMPORARY_PP:
                sb.append("Temporary Power Points: +").append(boost);
                break;
            case ATTACK_NUM:
                sb.append("Additional attack chance: ").append(boost).append("%");
                break;
            case SUCCESS_PERCENTAGE:
                sb.append("Success Percentage: +").append(boost).append("%");
                break;
            case MONEY_BOOST:
                sb.append("Money Boost: +").append(boost).append("%");
                break;
            default:
                sb.append("Unknown boost type");
                break;
        }

        return sb.toString();
    }

    public void upgrade(){
        boost = boost +0.0001;
        level++;
    }

    public void duplicateWeapon(){
        boost = boost + 0.0002;
    }

    public Weapon() {
    }

    public Weapon(String id, String name, double boost, BonusType boost_type, int level, String image, String description) {
        this.id = id;
        this.name = name;
        this.boost = boost;
        this.boost_type = boost_type;
        this.level = level;
        this.image = image;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public double getBoost() {
        return boost;
    }

    public void setBoost(double boost) {
        this.boost = boost;
    }

    public BonusType getBoost_type() {
        return boost_type;
    }

    public void setBoost_type(BonusType boost_type) {
        this.boost_type = boost_type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
