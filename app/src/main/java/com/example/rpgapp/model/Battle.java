package com.example.rpgapp.model;

import java.util.Random;

public class Battle {
    private User user;
    private Boss boss;
    private int remainingAttacks;
    private boolean finished;

    public Battle(User user, int bossLevel) {
        this.user = user;
        this.boss = new Boss(bossLevel);
        this.remainingAttacks = 5;
        this.finished = false;
    }

    public User getUser() { return user; }
    public Boss getBoss() { return boss; }
    public int getRemainingAttacks() { return remainingAttacks; }
    public boolean isFinished() { return finished; }

    public boolean attack(int successRate) {
        if (remainingAttacks <= 0 || finished) return false;

        Random random = new Random();
        int chance = random.nextInt(100);

        boolean hit = chance < successRate;

        if (hit) {
            boss.reduceHp(calculateTotalPP());
        }

        remainingAttacks--;

        if (remainingAttacks == 0 || boss.isDefeated()) finished = true;

        return hit;
    }


    private int calculateTotalPP() {
        int totalPP = user.getPowerPoints();
        if(user.getEquipped() != null) {
            for(UserItem item : user.getEquipped().values()){
                if(item.getBonusType() == BonusType.PERMANENT_PP || item.getBonusType() == BonusType.TEMPORARY_PP){
                    totalPP += (int)item.getCurrentBonus();
                }
            }
        }
        return totalPP;
    }
    public int calculateCoins() {
        int base = 200;
        int level = boss.getLevel();
        return (int) (base * Math.pow(1.2, level - 1));
    }


    //TODO: RAZDVOJ ODECU I ORUZJE 95%-5%
    public boolean dropEquipment() {
        Random rand = new Random();
        if(rand.nextInt(100) < 20){ // 20% šansa za item
            return true;
        }
        return false;
    }

}
