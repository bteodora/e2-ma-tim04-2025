package com.example.rpgapp.fragments.profile;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.ItemType;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.model.UserWeapon;
import com.example.rpgapp.model.Weapon;
import com.example.rpgapp.tools.GameData;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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

        ProfileFragmentArgs args = ProfileFragmentArgs.fromBundle(getArguments());
        String userId = args.getUserId();
        boolean autoSend = args.getAutoSendRequest();

        viewModel.loadUserProfile(userId);

        if (autoSend) {
            viewModel.setAutoSendFlag();
        }
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
                generateAndShowQrCode(user.getUserId());
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
            viewModel.executeAutoSendIfNeeded(status);

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
                    buttonInviteToAlliance.setVisibility(View.GONE);
                    break;

                case PENDING_SENT:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Pending");
                    buttonAddFriend.setEnabled(false);
                    buttonInviteToAlliance.setVisibility(View.GONE);
                    break;

                case PENDING_RECEIVED:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Accept Request");
                    buttonAddFriend.setEnabled(true);
                    buttonInviteToAlliance.setVisibility(View.GONE);
                    buttonAddFriend.setOnClickListener(v -> {
                        showAcceptDeclineDialog();
                    });
                    break;

                case FRIENDS:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setVisibility(View.VISIBLE);
                    buttonAddFriend.setText("Friends");
                    buttonAddFriend.setEnabled(false);
                    buttonInviteToAlliance.setVisibility(View.GONE);
                    break;

                case FRIEND_CAN_BE_INVITED:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setVisibility(View.GONE);
                    buttonInviteToAlliance.setVisibility(View.VISIBLE);
                    buttonInviteToAlliance.setText("Invite to Alliance");
                    buttonInviteToAlliance.setEnabled(true);
                    buttonInviteToAlliance.setOnClickListener(v -> {
                        viewModel.inviteFriendToAlliance();
                    });
                    break;
                case FRIEND_INVITE_PENDING:
                    layout_other_user_actions.setVisibility(View.VISIBLE);
                    buttonAddFriend.setVisibility(View.GONE);
                    buttonInviteToAlliance.setVisibility(View.VISIBLE);
                    buttonInviteToAlliance.setText("Invite Pending");
                    buttonInviteToAlliance.setEnabled(false);
                    break;
            }
        });
    }

    private void generateAndShowQrCode(String userId) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(userId, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            imageViewQrCode.setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
        }
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
        populateBadges(user);

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

    private void equipItem(User user, UserItem itemToEquipFromInventory) {
        if (user.getUserItems() == null) user.setUserItems(new HashMap<>());
        if (user.getEquipped() == null) user.setEquipped(new HashMap<>());

        Item baseItem = GameData.getAllItems().get(itemToEquipFromInventory.getItemId());
        if (baseItem == null) {
            Toast.makeText(getContext(), "Error: Item data not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (baseItem.getType() == ItemType.CLOTHING) {
            final int MAX_EQUIPPED_CLOTHES = 2;
            Map<String, UserItem> equippedItems = user.getEquipped();
            String itemIdToEquip = itemToEquipFromInventory.getItemId();

            if (equippedItems.containsKey(itemIdToEquip)) {
                UserItem equippedVersion = equippedItems.get(itemIdToEquip);

                if (equippedVersion.isDuplicated()) {
                    Toast.makeText(getContext(), "Bonus for this item is already doubled.", Toast.LENGTH_SHORT).show();
                    return;
                }

                equippedVersion.duplicateBonus();
                smanjiKolicinuUInventaru(user, itemIdToEquip);

                viewModel.updateUser(user);
                Toast.makeText(getContext(), baseItem.getName() + " bonus doubled!", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentEquippedClothesCount = 0;
            for (UserItem equippedItem : equippedItems.values()) {
                Item equippedBaseItem = GameData.getAllItems().get(equippedItem.getItemId());
                if (equippedBaseItem != null && equippedBaseItem.getType() == ItemType.CLOTHING) {
                    currentEquippedClothesCount++;
                }
            }

            if (currentEquippedClothesCount >= MAX_EQUIPPED_CLOTHES) {
                Toast.makeText(getContext(), "You can only equip " + MAX_EQUIPPED_CLOTHES + " different clothing items.", Toast.LENGTH_SHORT).show();
                return;
            }

            smanjiKolicinuUInventaru(user, itemIdToEquip);

            UserItem newEquippedItem = new UserItem();
            newEquippedItem.setItemId(itemIdToEquip);
            newEquippedItem.setQuantity(1);
            newEquippedItem.setLifespan(baseItem.getLifespan());
            newEquippedItem.setBonusType(baseItem.getBonusType());
            newEquippedItem.setCurrentBonus(baseItem.getBonusValue());
            newEquippedItem.setDuplicated(false);

            equippedItems.put(itemIdToEquip, newEquippedItem);

            viewModel.updateUser(user);
            Toast.makeText(getContext(), baseItem.getName() + " equipped!", Toast.LENGTH_SHORT).show();

        } else {
            smanjiKolicinuUInventaru(user, itemToEquipFromInventory.getItemId());

            UserItem equippedVersion;
            if (user.getEquipped().containsKey(itemToEquipFromInventory.getItemId())) {
                equippedVersion = user.getEquipped().get(itemToEquipFromInventory.getItemId());
                equippedVersion.setQuantity(equippedVersion.getQuantity() + 1);
            } else {
                equippedVersion = new UserItem();
                equippedVersion.setItemId(itemToEquipFromInventory.getItemId());
                equippedVersion.setQuantity(1);
                equippedVersion.setLifespan(baseItem.getLifespan());
                equippedVersion.setBonusType(baseItem.getBonusType());
                equippedVersion.setCurrentBonus(baseItem.getBonusValue());
                user.getEquipped().put(itemToEquipFromInventory.getItemId(), equippedVersion);
            }

            viewModel.updateUser(user);
            Toast.makeText(getContext(), baseItem.getName() + " activated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void smanjiKolicinuUInventaru(User user, String itemId) {
        UserItem inventoryVersion = user.getUserItems().get(itemId);
        if (inventoryVersion != null) {
            if (inventoryVersion.getQuantity() > 1) {
                inventoryVersion.setQuantity(inventoryVersion.getQuantity() - 1);
            } else {
                user.getUserItems().remove(itemId);
            }
        }
    }


    private void showAcceptDeclineDialog() {
        User otherUser = viewModel.getDisplayedUser().getValue();
        if (otherUser == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Friend Request")
                .setMessage("Respond to the friend request from " + otherUser.getUsername() + "?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    viewModel.acceptFriendRequest();
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    viewModel.declineFriendRequest();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void populateBadges(User user) {
        int badges = 0;

        // Ako je korisnik učestvovao u specijalnoj misiji
        MissionTask mission = viewModel.getCurrentSpecialMission().getValue();
        if (mission != null && mission.getUserTotalProgress().containsKey(user.getUserId())) {
            badges = mission.getUserTotalProgress().get(user.getUserId());
        }

        textViewBadgesCount.setText("Badges: " + badges);
    }


    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}