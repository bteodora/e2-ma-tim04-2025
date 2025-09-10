package com.example.rpgapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rpgapp.R;
import com.example.rpgapp.fragments.shop.ShopScreenState;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.google.android.material.card.MaterialCardView;

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

    public void updateData(ShopScreenState state) {
        if (state == null || state.shopItems == null || state.user == null) return;
        this.items.clear();
        this.items.addAll(state.shopItems);
        this.currentUser = state.user;
        notifyDataSetChanged();
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemIcon;
        private TextView itemName, itemDescription, itemStatus, itemPrice;
        LinearLayout  cardViewItem;
        LinearLayout expandableLayout;
        private Button buyButton;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.imageViewItemIcon);
            itemName = itemView.findViewById(R.id.textViewItemName);
            cardViewItem = itemView.findViewById(R.id.cardViewItem);
            expandableLayout = itemView.findViewById(R.id.layout_expandable_description);
            itemDescription = expandableLayout.findViewById(R.id.textViewItemDescription);
            itemStatus = itemView.findViewById(R.id.textViewItemStatus);
            itemPrice = itemView.findViewById(R.id.textViewItemPrice);
            buyButton = itemView.findViewById(R.id.buttonBuyItem);
        }

        public void bind(final Item item, User user, final OnBuyClickListener listener) {
            itemName.setText(item.getName());
            itemDescription.setText(item.getDescription());

            String imageId = item.getImage();
            if (imageId == null || imageId.isEmpty()) imageId = "default_item";

            Context context = itemView.getContext();
            int resourceId = context.getResources().getIdentifier(imageId, "drawable", context.getPackageName());
            if (resourceId == 0) resourceId = R.drawable.default_avatar;

            Glide.with(context)
                    .load(resourceId)
                    .into(itemIcon);

            cardViewItem.setOnClickListener(v -> {
                boolean isVisible = expandableLayout.getVisibility() == View.VISIBLE;
                expandableLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            });

            buyButton.setOnClickListener(v -> listener.onBuyClick(item));

            if (user == null) {
                itemPrice.setText("...");
                itemStatus.setText("");
                buyButton.setEnabled(false);
                return;
            }

            int price = item.calculatePrice(user.calculatePreviosPrizeFormula());
            itemPrice.setText(price + " coins");
            itemPrice.setVisibility(View.VISIBLE);

            Map<String, UserItem> userItems = user.getUserItems();
            boolean ownsItem = userItems != null && userItems.containsKey(item.getId());
            int quantity = ownsItem ? userItems.get(item.getId()).quantity : 0;

            itemStatus.setText("Owned: " + quantity);
            itemStatus.setVisibility(View.VISIBLE);
            buyButton.setVisibility(View.VISIBLE);
            buyButton.setEnabled(user.getCoins() >= price);
        }
    }
}
