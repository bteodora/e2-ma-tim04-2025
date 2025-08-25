package com.example.rpgapp.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.ItemType;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private List<Item> items = new ArrayList<>();
    private User currentUser;
    private final OnBuyClickListener buyClickListener;

    public interface OnBuyClickListener {
        void onBuyClick(Item item);
    }

    public ShopAdapter(OnBuyClickListener listener) {
        this.buyClickListener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        Item currentItem = items.get(position);
        holder.bind(currentItem, currentUser, buyClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        notifyDataSetChanged();
    }


    static class ShopViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemIcon;
        private TextView itemName, itemDescription, ownedQuantity, itemPrice;
        private Button buyButton;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.imageViewItemIcon);
            itemName = itemView.findViewById(R.id.textViewItemName);
            itemDescription = itemView.findViewById(R.id.textViewItemDescription);
            ownedQuantity = itemView.findViewById(R.id.textViewOwnedQuantity);
            itemPrice = itemView.findViewById(R.id.textViewItemPrice);
            buyButton = itemView.findViewById(R.id.buttonBuyItem);
        }

        public void bind(final Item item, User user, final OnBuyClickListener listener) {
            itemName.setText(item.getName());
            itemDescription.setText(item.getDescription());

            // TODO: Učitaj pravu sliku na osnovu item.getImageId()

            if (user != null) {
                int price = item.calculatePrice(user.calculatePreviosPrizeFormula());
                itemPrice.setText(price + " coins");

                Map<String, UserItem> userItems = user.getUserItems();
                int quantity = 0;
                if (userItems != null && userItems.containsKey(item.getId())) {
                    quantity = userItems.get(item.getId()).quantity;
                }

                if (item.getType() == ItemType.POTION) {
                    ownedQuantity.setText("Owned: " + quantity);
                    ownedQuantity.setVisibility(View.VISIBLE);
                    buyButton.setEnabled(true);
                } else {
                    ownedQuantity.setVisibility(View.GONE);
                    buyButton.setEnabled(quantity == 0);
                }

                if (user.getCoins() < price) {
                    buyButton.setEnabled(false);
                }

            } else {
                itemPrice.setText("...");
                buyButton.setEnabled(false);
            }

            buyButton.setOnClickListener(v -> listener.onBuyClick(item));
        }
    }
}