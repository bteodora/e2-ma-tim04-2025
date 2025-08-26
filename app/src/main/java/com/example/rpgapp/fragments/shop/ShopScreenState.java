package com.example.rpgapp.fragments.shop;

import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.User;
import java.util.List;

public class ShopScreenState {
    public final User user;
    public final List<Item> shopItems;

    public ShopScreenState(User user, List<Item> shopItems) {
        this.user = user;
        this.shopItems = shopItems;
    }
}