package com.example.rpgapp.fragments.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.model.UserWeapon;
import com.example.rpgapp.model.Weapon;
import com.example.rpgapp.tools.GameData;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ProfileViewModel viewModel;

    private ImageView imageViewProfileAvatar;
    private TextView textViewProfileUsername, textViewProfileTitle, textViewProfileLevel, textViewProfileXp, textViewProfileCoins, textViewProfilePP, textViewBadgesCount;
    private LinearLayout layout_coins, layout_power_points, layout_inventory_section, layout_weapons_container;
    private GridLayout grid_equipped_container, grid_inventory_container;
    private Button buttonViewStatistics, buttonChangePassword, buttonAddFriend, buttonInviteToAlliance;
    private LinearLayout layout_own_profile_actions, layout_other_user_actions;
    private ImageView imageViewQrCode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        bindViews(view);
        observeViewModel();

        String userId = ProfileFragmentArgs.fromBundle(getArguments()).getUserId();

        viewModel.loadUserProfile(userId);
    }

    private void bindViews(View view) {
        imageViewProfileAvatar = view.findViewById(R.id.imageViewProfileAvatar);
        textViewProfileUsername = view.findViewById(R.id.textViewProfileUsername);
        textViewProfileTitle = view.findViewById(R.id.textViewProfileTitle);
        textViewProfileLevel = view.findViewById(R.id.textViewProfileLevel);
        textViewProfileXp = view.findViewById(R.id.textViewProfileXp);
        textViewProfileCoins = view.findViewById(R.id.textViewProfileCoins);
        textViewProfilePP = view.findViewById(R.id.textViewProfilePP);
        textViewBadgesCount = view.findViewById(R.id.textViewBadgesCount);
        layout_coins = view.findViewById(R.id.layout_coins);
        layout_power_points = view.findViewById(R.id.layout_power_points);
        layout_inventory_section = view.findViewById(R.id.layout_inventory_section);
        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);

        layout_weapons_container = view.findViewById(R.id.layout_weapons_container);
        grid_equipped_container = view.findViewById(R.id.grid_equipped_container);
        grid_inventory_container = view.findViewById(R.id.grid_inventory_container);
        layout_inventory_section = view.findViewById(R.id.layout_inventory_section);

        buttonViewStatistics = view.findViewById(R.id.buttonViewStatistics);
        buttonChangePassword = view.findViewById(R.id.buttonChangePassword);

        layout_own_profile_actions = view.findViewById(R.id.layout_own_profile_actions);
        layout_other_user_actions = view.findViewById(R.id.layout_other_user_actions);
        buttonAddFriend = view.findViewById(R.id.buttonAddFriend);
        buttonInviteToAlliance = view.findViewById(R.id.buttonInviteToAlliance);

        buttonChangePassword.setOnClickListener(v -> {
            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.action_profileFragment_to_changePasswordFragment);
        });
    }

    private void observeViewModel() {
        viewModel.getDisplayedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                populateUI(user);
            }
        });

        viewModel.getDisplayedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                populateUI(user);
            }
        });

        viewModel.getIsMyProfile().observe(getViewLifecycleOwner(), isMyProfile -> {
            if (isMyProfile != null) {
                int visibility = isMyProfile ? View.VISIBLE : View.GONE;

                buttonViewStatistics.setVisibility(visibility);
                buttonChangePassword.setVisibility(visibility);
                layout_coins.setVisibility(visibility);
                layout_power_points.setVisibility(visibility);
                layout_inventory_section.setVisibility(visibility);
                layout_own_profile_actions.setVisibility(isMyProfile ? View.VISIBLE : View.GONE);
                layout_other_user_actions.setVisibility(isMyProfile ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getFriendshipStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) return;

            layout_other_user_actions.setVisibility(View.GONE);

            switch (status) {
                case MY_PROFILE:
                    break;

                case NOT_FRIENDS:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Add Friend");
                    buttonAddFriend.setEnabled(true);
                    buttonAddFriend.setOnClickListener(v -> {
                        viewModel.sendFriendRequest();
                    });
                    break;

                case PENDING_SENT:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Pending");
                    buttonAddFriend.setEnabled(false);
                    break;

                case PENDING_RECEIVED:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Accept Request");
                    buttonAddFriend.setEnabled(true);
                    // TODO: Kasnije ćemo dodati OnClickListener
                    break;

                case FRIENDS:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Friends");
                    buttonAddFriend.setEnabled(false);
                    break;
            }
        });
    }

    private void populateUI(User user) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        textViewProfileUsername.setText(user.getUsername());
        textViewProfileTitle.setText(user.getTitle());
        textViewProfileLevel.setText(String.valueOf(user.getLevel()));
        textViewProfileXp.setText(numberFormat.format(user.getXp()));
        textViewProfileCoins.setText(numberFormat.format(user.getCoins()));
        textViewProfilePP.setText(String.valueOf(user.getPowerPoints()));

        populateWeapons(user);
        populateEquipped(user);
        populateInventory(user);

        // TODO: QR kod, bedževi
    }

    private void populateWeapons(User user) {
        layout_weapons_container.removeAllViews();
        if (user.getUserWeapons() == null || user.getUserWeapons().isEmpty()) {
            TextView noWeaponsText = new TextView(getContext());
            noWeaponsText.setText("No weapons owned.");
            layout_weapons_container.addView(noWeaponsText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (UserWeapon userWeapon : user.getUserWeapons().values()) {
            View weaponView = inflater.inflate(R.layout.list_item_weapon, layout_weapons_container, false);

            ImageView weaponIcon = weaponView.findViewById(R.id.imageViewWeaponIcon);
            TextView weaponName = weaponView.findViewById(R.id.textViewWeaponName);
            TextView weaponBonus = weaponView.findViewById(R.id.textViewWeaponBonus);
            Button upgradeButton = weaponView.findViewById(R.id.buttonUpgradeWeapon);

            Weapon baseWeapon = GameData.getAllWeapons().get(userWeapon.getWeaponId());
            if (baseWeapon == null) continue;

            weaponName.setText(baseWeapon.getName() + " (Lvl " + userWeapon.getLevel() + ")");
            weaponBonus.setText(userWeapon.getBoostAsString());

            // TODO: Postavi pravu sliku oružja na osnovu baseWeapon.getImageId()
            // weaponIcon.setImageResource(...);

            int upgradePrice = baseWeapon.getUpgradePrice(user.calculatePreviosPrizeFormula());
            upgradeButton.setText("Upgrade\n(" + upgradePrice + " coins)");

            if (user.getCoins() < upgradePrice) {
                upgradeButton.setEnabled(false);
            }

            upgradeButton.setOnClickListener(v -> {
                handleWeaponUpgrade(user, userWeapon, baseWeapon, upgradePrice);
            });

            layout_weapons_container.addView(weaponView);
        }
    }

    private void handleWeaponUpgrade(User user, UserWeapon userWeapon, Weapon baseWeapon, int price) {
        if (user.getCoins() < price) {
            Toast.makeText(getContext(), "Not enough coins to upgrade!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Upgrade " + baseWeapon.getName() + "?")
                .setMessage("This will cost " + price + " coins.")
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    user.setCoins(user.getCoins() - price);

                    userWeapon.upgrade();

                    viewModel.updateUser(user);

                    Toast.makeText(getContext(), baseWeapon.getName() + " upgraded!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void populateEquipped(User user) {
        grid_equipped_container.removeAllViews();
        if (user.getEquipped() == null) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (UserItem equippedItem : user.getEquipped().values()) {
            View itemView = inflater.inflate(R.layout.grid_item_equipment, grid_equipped_container, false);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            itemView.setLayoutParams(params);


            ImageView itemIcon = itemView.findViewById(R.id.imageViewItemIcon);
            TextView itemName = itemView.findViewById(R.id.textViewItemName);
            TextView itemQuantity = itemView.findViewById(R.id.textViewItemQuantity);
            Button itemActionButton = itemView.findViewById(R.id.buttonItemAction);

            Item baseItem = GameData.getAllItems().get(equippedItem.getItemId());
            if (baseItem == null) continue;
            itemName.setText(baseItem.getName());


            if (equippedItem.getQuantity() > 1) {
                itemQuantity.setText(String.valueOf(equippedItem.getQuantity()));
                itemQuantity.setVisibility(View.VISIBLE);
            } else {
                itemQuantity.setVisibility(View.GONE);
            }
            itemActionButton.setVisibility(View.GONE);

            grid_equipped_container.addView(itemView);
        }
    }

    private void populateInventory(User user) {
        grid_inventory_container.removeAllViews();
        if (user.getUserItems() == null || user.getUserItems().isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (UserItem inventoryItem : user.getUserItems().values()) {
            View itemView = inflater.inflate(R.layout.grid_item_equipment, grid_inventory_container, false);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            itemView.setLayoutParams(params);

            ImageView itemIcon = itemView.findViewById(R.id.imageViewItemIcon);
            TextView itemName = itemView.findViewById(R.id.textViewItemName);
            TextView itemQuantity = itemView.findViewById(R.id.textViewItemQuantity);
            Button itemActionButton = itemView.findViewById(R.id.buttonItemAction);

            Item baseItem = GameData.getAllItems().get(inventoryItem.getItemId());
            if (baseItem == null) continue;

            itemName.setText(baseItem.getName());
            // TODO: Postavi pravu sliku item-a

            if (inventoryItem.getQuantity() > 0) {
                itemQuantity.setText(String.valueOf(inventoryItem.getQuantity()));
                itemQuantity.setVisibility(View.VISIBLE);
            } else {
                itemQuantity.setVisibility(View.GONE);
            }

            itemActionButton.setText("Equip");
            itemActionButton.setOnClickListener(v -> {
                showEquipDialog(user, inventoryItem);
            });

            grid_inventory_container.addView(itemView);
        }
    }

    private void showEquipDialog(User user, UserItem itemToEquip) {
        Item baseItem = GameData.getAllItems().get(itemToEquip.getItemId());
        if (baseItem == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Activate " + baseItem.getName() + "?")
                .setMessage(baseItem.getDescription())
                .setPositiveButton("Activate", (dialog, which) -> {
                    equipItem(user, itemToEquip);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void equipItem(User user, UserItem itemToEquip) {
        if (user.getUserItems() == null) user.setUserItems(new HashMap<>());
        if (user.getEquipped() == null) user.setEquipped(new HashMap<>());

        UserItem inventoryVersion = user.getUserItems().get(itemToEquip.getItemId());
        if (inventoryVersion != null) {
            if (inventoryVersion.getQuantity() > 1) {
                inventoryVersion.setQuantity(inventoryVersion.getQuantity() - 1);
            } else {
                user.getUserItems().remove(itemToEquip.getItemId());
            }
        }

        UserItem equippedVersion;
        if (user.getEquipped().containsKey(itemToEquip.getItemId())) {
            equippedVersion = user.getEquipped().get(itemToEquip.getItemId());
            equippedVersion.setQuantity(equippedVersion.getQuantity() + 1);
        } else {
            equippedVersion = new UserItem();
            Item baseItem = GameData.getAllItems().get(itemToEquip.getItemId());
            if (baseItem == null) return;

            equippedVersion.setItemId(itemToEquip.getItemId());
            equippedVersion.setQuantity(1);
            equippedVersion.setLifespan(baseItem.getLifespan());
            equippedVersion.setBonusType(baseItem.getBonusType());
            equippedVersion.setCurrentBonus(baseItem.getBonusValue());

            user.getEquipped().put(itemToEquip.getItemId(), equippedVersion);
        }

        viewModel.updateUser(user);

        Toast.makeText(getContext(), itemToEquip.getItemId() + " activated!", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}