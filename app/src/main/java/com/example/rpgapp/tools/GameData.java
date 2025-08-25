package com.example.rpgapp.tools;

import com.example.rpgapp.model.BonusType;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.ItemType;
import com.example.rpgapp.model.Weapon;

import java.util.HashMap;
import java.util.Map;

public class GameData {

    public static Map<String, Item> getAllItems() {
        Map<String, Item> items = new HashMap<>();


        Item gloves = new Item();
        gloves.setId("gloves_strength");
        gloves.setName("Gloves of Strength");
        gloves.setBonusType(BonusType.TEMPORARY_PP);
        gloves.setBonusValue(0.10);
        gloves.setLifespan(2);
        gloves.setPrice_percentage(60);
        gloves.setType(ItemType.CLOTHING);
        items.put(gloves.getId(), gloves);


        // 5. Štit za povećanje šanse uspešnog napada (+10%)
        Item shield = new Item();
        shield.setId("shield_luck");
        shield.setName("Shield of Luck");
        shield.setDescription("Increases your successful attack chance by 10%. Lasts for 2 boss fights.");
        shield.setImage("icon_shield");
        shield.setType(ItemType.CLOTHING);
        shield.setPrice_percentage(60);
        shield.setLifespan(2);
        shield.setBonusType(BonusType.SUCCESS_PERCENTAGE);
        shield.setBonusValue(0.10);
        items.put(shield.getId(), shield);

        // 6. Čizme za šansu povećanja broja napada (+1 dodatni napad)
        Item boots = new Item();
        boots.setId("boots_swiftness");
        boots.setName("Boots of Swiftness");
        boots.setDescription("Grants a 40% chance to perform an extra attack. Lasts for 2 boss fights.");
        boots.setImage("icon_boots");
        boots.setType(ItemType.CLOTHING);
        boots.setPrice_percentage(80);
        boots.setLifespan(2);
        boots.setBonusType(BonusType.ATTACK_NUM);
        boots.setBonusValue(0.40);
        items.put(boots.getId(), boots);


        // Napitak za jednokratnu snagu (+20% PP, traje 1 borbu)
        Item tempPotion20 = new Item();
        tempPotion20.setId("potion_temp_pp_20");
        tempPotion20.setName("Minor Draught of Power");
        tempPotion20.setDescription("Temporarily increases your Power Points by 20% for the next boss fight.");
        tempPotion20.setImage("icon_potion_pp_temp_1");
        tempPotion20.setType(ItemType.POTION);
        tempPotion20.setPrice_percentage(50);
        tempPotion20.setLifespan(1);
        tempPotion20.setBonusType(BonusType.TEMPORARY_PP);
        tempPotion20.setBonusValue(0.20);
        items.put(tempPotion20.getId(), tempPotion20);

        // Napitak za jednokratnu snagu (+40% PP, traje 1 borbu)
        Item tempPotion40 = new Item();
        tempPotion40.setId("potion_temp_pp_40");
        tempPotion40.setName("Greater Draught of Power");
        tempPotion40.setDescription("Temporarily increases your Power Points by 40% for the next boss fight.");
        tempPotion40.setImage("icon_potion_pp_temp_2");
        tempPotion40.setType(ItemType.POTION);
        tempPotion40.setPrice_percentage(70);
        tempPotion40.setLifespan(1);
        tempPotion40.setBonusType(BonusType.TEMPORARY_PP);
        tempPotion40.setBonusValue(0.40);
        items.put(tempPotion40.getId(), tempPotion40);

        // Napitak za trajno povećanje snage (+5% PP)
        Item permPotion5 = new Item();
        permPotion5.setId("potion_perm_pp_5");
        permPotion5.setName("Elixir of Enduring Strength");
        permPotion5.setDescription("Permanently increases your base Power Points by 5%.");
        permPotion5.setImage("icon_potion_pp_perm_1");
        permPotion5.setType(ItemType.POTION);
        permPotion5.setPrice_percentage(200);
        permPotion5.setLifespan(0);
        permPotion5.setBonusType(BonusType.PERMANENT_PP);
        permPotion5.setBonusValue(0.05);
        items.put(permPotion5.getId(), permPotion5);

        // Napitak za trajno povećanje snage (+10% PP)
        Item permPotion10 = new Item();
        permPotion10.setId("potion_perm_pp_10");
        permPotion10.setName("Greater Elixir of Enduring Strength");
        permPotion10.setDescription("Permanently increases your base Power Points by 10%.");
        permPotion10.setImage("icon_potion_pp_perm_2");
        permPotion10.setType(ItemType.POTION);
        permPotion10.setPrice_percentage(1000);
        permPotion10.setLifespan(0);
        permPotion10.setBonusType(BonusType.PERMANENT_PP);
        permPotion10.setBonusValue(0.10);
        items.put(permPotion10.getId(), permPotion10);


        return items;
    }

    public static Map<String, Weapon> getAllWeapons() {
        Map<String, Weapon> weapons = new HashMap<>();

        // Bonus: Trajno povećanje snage (PP) za 5%
        Weapon sword = new Weapon();
        sword.setId("sword_basic");
        sword.setName("Warrior's Sword");
        sword.setDescription("A sturdy blade that permanently increases your Power Points by 5%.");
        sword.setImage("icon_sword");
        sword.setBoost_type(BonusType.PERMANENT_PP);
        sword.setBoost(0.05);
        sword.setLevel(1);
        weapons.put(sword.getId(), sword);

        // Bonus: Stalno povećanje dobijenog novca za 5%
        Weapon bow = new Weapon();
        bow.setId("bow_basic");
        bow.setName("Ranger's Bow");
        bow.setDescription("A reliable bow that permanently increases the amount of coins you find by 5%.");
        bow.setImage("icon_bow");
        bow.setBoost_type(BonusType.MONEY_BOOST);
        bow.setBoost(0.05);
        bow.setLevel(1);
        weapons.put(bow.getId(), bow);

        return weapons;
    }
}
