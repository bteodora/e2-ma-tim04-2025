package com.example.rpgapp.database;


import android.util.Log;

import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.ItemType;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.tools.GameData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentRepository {

    private UserRepository userRepository;
    public List<Item> getShopStock() {
        Map<String, Item> allItems = GameData.getAllItems();

        return new ArrayList<>(allItems.values());
    }

    public boolean purchaseItem(User currentUser, Item itemToBuy, int price) {

        if (currentUser.getCoins() < price) {
            Log.e("Shop", "Neuspešna kupovina: Nedovoljno novčića!");
            return false;
        }

        currentUser.setCoins(currentUser.getCoins() - price);

        Map<String, UserItem> userItems = currentUser.getUserItems();
        if (userItems == null) {
            userItems = new HashMap<>();
            currentUser.setUserItems(userItems);
        }

        String itemId = itemToBuy.getId();

        UserItem itemState;
        if (userItems.containsKey(itemId)) {
            itemState = userItems.get(itemId);
            itemState.quantity++;
        } else {
            itemState = new UserItem();
            itemState.itemId = itemId;
            itemState.quantity = 1;
            itemState.bonusType = itemToBuy.getBonusType();
            itemState.lifespan = itemToBuy.getLifespan();
            itemState.currentBonus = itemToBuy.getBonusValue();
            itemState.isDuplicated = false;
        }

        userItems.put(itemId, itemState);
        currentUser.setUserItems(userItems);

        Log.d("Shop", "Kupovina uspešna za predmet: " + itemId);
        return true;
    }

}