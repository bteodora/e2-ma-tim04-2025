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

        // Oduzmi novac
        currentUser.setCoins(currentUser.getCoins() - price);

        Map<String, UserItem> userItems = currentUser.getUserItems();
        if (userItems == null) {
            userItems = new HashMap<>();
        }

        String itemId = itemToBuy.getId();

        if (itemToBuy.getType() == ItemType.POTION) {
            // Logika za napitke...
            UserItem potionState;
            if (userItems.containsKey(itemId)) {
                potionState = userItems.get(itemId);
                potionState.quantity++;
            } else {
                potionState = new UserItem();
                potionState.itemId = itemId;
                potionState.quantity = 1;
                potionState.bonusType = itemToBuy.getBonusType();
                potionState.lifespan = itemToBuy.getLifespan();
                potionState.currentBonus = itemToBuy.getBonusValue();
            }
            userItems.put(itemId, potionState);
        } else {
            if (userItems.containsKey(itemId)) {
                Log.w("Shop", "Pokušaj kupovine duplikata odeće, neuspešno.");
                currentUser.setCoins(currentUser.getCoins() + price);
                return false;
            } else {
                UserItem newClothing = new UserItem();
                newClothing.itemId = itemId;
                newClothing.quantity = 1;
                newClothing.lifespan = itemToBuy.getLifespan();
                newClothing.bonusType = itemToBuy.getBonusType();
                newClothing.currentBonus = itemToBuy.getBonusValue();
                userItems.put(itemId, newClothing);
            }
        }

        currentUser.setUserItems(userItems);

        // OBRISALI SMO userRepository.updateUser(currentUser); ODAVDE!
        // To je sada posao za ViewModel.

        Log.d("Shop", "Lokalni User objekat pripremljen za ažuriranje.");
        return true; // JAVI DA JE SVE PROŠLO KAKO TREBA
    }
}