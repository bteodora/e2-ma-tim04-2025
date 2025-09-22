package com.example.rpgapp.fragments.battle;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
//import com.example.rpgapp.adapters.WeaponListAdapter;
import com.example.rpgapp.adapters.EquipmentListAdapter;
import com.example.rpgapp.adapters.WeaponListAdapter;
import com.example.rpgapp.database.BattleRepository;
import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.fragments.profile.ProfileFragment;
import com.example.rpgapp.model.Battle;
import com.example.rpgapp.model.BonusType;
import com.example.rpgapp.model.Boss;
import com.example.rpgapp.model.EquipmentDisplay;
import com.example.rpgapp.model.EquippedItem;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.model.UserWeapon;
import com.example.rpgapp.model.Weapon;
import com.example.rpgapp.services.TaskService;
import com.example.rpgapp.tools.GameData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import com.bumptech.glide.Glide;

public class BossBattleFragment extends Fragment implements SensorEventListener {

    private static final String SP_NAME = "game_prefs";
    private static final String SP_BOSS_LEVEL = "boss_level";
    private static final String SP_BOSS_HP = "boss_hp";

    private BattleRepository battleRepository;

    private Battle battle;
    private User user;
    private String currentUserId;

    private ImageView bossImageView, treasureChestImage, activeWeaponIcon;
    private ProgressBar bossHpBar, userPpBar;
    private TextView successRateText, remainingAttacksText, coinsEarnedText, bossLevelText, bossHpText, userPpText;
    private Button attackButton, selectWeaponButton, changeEquipmentButton;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastUpdate = 0;

    private int successRate = 67; // default
    private boolean isAttackInProgress = false;

    private TaskService taskService;
    private SpecialMissionViewModel specialMissionViewModel;
    private SpecialMission activeMission;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = UserRepository.getInstance(requireContext()).getLoggedInUser();

        if (user == null) {
            Toast.makeText(requireContext(), "Niste ulogovani.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user != null) {
            currentUserId = user.getUserId();
        }

        battleRepository = new BattleRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        specialMissionViewModel = new ViewModelProvider(requireActivity())
                .get(SpecialMissionViewModel.class);

        specialMissionViewModel.getCurrentMission().observe(getViewLifecycleOwner(), mission -> {
            activeMission = mission;
        });

        View view = inflater.inflate(R.layout.fragment_boss_battle, container, false);

        bossImageView = view.findViewById(R.id.bossImageView);
        treasureChestImage = view.findViewById(R.id.treasureChestImage);
        bossHpBar = view.findViewById(R.id.bossHpBar);
        userPpBar = view.findViewById(R.id.userPpBar);
        successRateText = view.findViewById(R.id.successRateText);
        remainingAttacksText = view.findViewById(R.id.remainingAttacksText);
        coinsEarnedText = view.findViewById(R.id.coinsEarnedText);
        attackButton = view.findViewById(R.id.attackButton);
        selectWeaponButton = view.findViewById(R.id.selectWeaponButton);
        activeWeaponIcon = view.findViewById(R.id.activeWeaponIcon);
        bossLevelText = view.findViewById(R.id.bossLevelText);
        bossHpText = view.findViewById(R.id.bossHpText);
        userPpText = view.findViewById(R.id.userPpText);
        changeEquipmentButton = view.findViewById(R.id.changeEquipmentButton);

        // --- Hide neki elementi na startu ---
        coinsEarnedText.setVisibility(View.GONE);
        treasureChestImage.setVisibility(View.GONE);
        activeWeaponIcon.setVisibility(View.GONE);

        // --- Provera da li je user ulogovan ---
        if (user == null) {
            Toast.makeText(requireContext(), "Niste ulogovani.", Toast.LENGTH_SHORT).show();
            return view;
        }

        // --- Učitavanje User + Battle (izdvojeno u posebnu metodu) ---
        loadUserAndBattle();

        // --- Senzor za pokrete ---
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        return view;
    }



    private void loadUserAndBattle() {
        UserRepository.getInstance(requireContext()).getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User loadedUser) {
                if (loadedUser == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "User not found!", Toast.LENGTH_SHORT).show());
                    return;
                }

                user = loadedUser;
                // int bossLevel = loadBossLevelOrDefault(1);
                int bossLevel;


                SharedPreferences sp = requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

                if (sp.contains(SP_BOSS_LEVEL)) {
                    // Učitavamo prethodni level
                    bossLevel = sp.getInt(SP_BOSS_LEVEL, 1);
                } else {
                    // Prvi put startuje na level 1
                    bossLevel = 1;
                    saveBossLevel(bossLevel); // sačuvaj prvi put
                }

                battleRepository.getBattlesForCurrentUser(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        battle = task.getResult().get(0);
                        battle.setUser(user);
                        restoreBossHpIfExists();
                    } else {
                        battle = new Battle(user, bossLevel, successRate);
                        battleRepository.addOrUpdateBattle(battle, t -> {});
                    }

                    // --- POSTAVI UI tek kada su user i battle učitani ---
                    requireActivity().runOnUiThread(() -> setupUi());

                    // --- Učitaj specijalnu misiju za ulogovanog korisnika ---
                    SpecialMissionRepository.getInstance(requireContext())
                            .getActiveMissionForUser(currentUserId, new SpecialMissionRepository.MissionCallback() {
                                @Override
                                public void onMissionLoaded(SpecialMission mission) {
                                    specialMissionViewModel.setCurrentMission(mission);
                                    activeMission = mission;
                                    requireActivity().runOnUiThread(() -> {
                                        if (mission != null) {
                                            Toast.makeText(requireContext(),
                                                    "Svakim udarcem resavate zadatke iz specijalne misije!",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Error loading mission: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                                }
                            });


                });
            }

            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error loading user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }



    private void setupUi() {


    if (user == null || battle == null) return;


        int totalPP = calculateTotalPP(user);
        //user.setPowerPoints(totalPP);

        //UserRepository.getInstance(requireContext()).updateUser(user);
        battleRepository.updateBattle(battle, t -> {});

        userPpBar.setMax(Math.max(totalPP, 1));
        userPpBar.setProgress(totalPP);

        // Ovde samo prikazuješ PP
        userPpText.setText("PP: " + totalPP);


        bossHpBar.setMax(battle.getBoss().getMaxHp());
        bossHpBar.setProgress(battle.getBoss().getCurrentHp());

        bossLevelText.setText("Level: " + battle.getBoss().getLevel());
        bossHpText.setText("HP: " + battle.getBoss().getCurrentHp() + "/" + battle.getBoss().getMaxHp());


        remainingAttacksText.setText("Remaining attacks: " + battle.getRemainingAttacks() + "/5");

        attackButton.setOnClickListener(v -> handleAttack());
        selectWeaponButton.setOnClickListener(v -> showEquipmentSelectionDialog());

        taskService = TaskService.getInstance(requireContext());
        taskService = TaskService.getInstance(requireContext());
        taskService.getSuccessRate().observe(getViewLifecycleOwner(), rate -> {
            int baseSuccessRate = (int) Math.round(rate);
            successRate = calculateSuccessRate(user, baseSuccessRate);

            successRateText.setText("Chance to hit: " + successRate + "%");
            if (battle != null) battle.setSuccessRate(successRate);
        });

        // --- Onemogući attack dok se misija ne učita --- **************************************************
        specialMissionViewModel.getCurrentMission().observe(getViewLifecycleOwner(), mission -> {
            activeMission = mission;
            attackButton.setEnabled(activeMission != null);
        });

        changeEquipmentButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("userId", currentUserId); // ovde stavi ID korisnika koji treba
            NavController navController = NavHostFragment.findNavController(BossBattleFragment.this);
            navController.navigate(R.id.action_battleFragment_to_myProfileFragment, bundle);
        });



    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void handleAttack() {
        if (isAttackInProgress || battle == null || user == null || battle.getRemainingAttacks() <= 0) {
            if (battle != null && battle.isFinished()) {
                showResults();
                battle.setActiveWeapon(null);
                activeWeaponIcon.setVisibility(View.GONE);
            }
            return;
        }
        isAttackInProgress = true;


        boolean hit = battle.attack();
        if (hit) {
            bossImageView.setImageResource(R.drawable.boss_borba);
            bossImageView.postDelayed(() -> bossImageView.setImageResource(R.drawable.boss), 300);
            Toast.makeText(getContext(), "Hit!", Toast.LENGTH_SHORT).show();

            // --- Active mission update ---
            if (activeMission != null) {
                for (int i = 0; i < activeMission.getTasks().size(); i++) {
                    MissionTask task = activeMission.getTasks().get(i);
                    if ("Udarac u regularnoj borbi".equals(task.getName())) {
                        specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                        break;
                    }
                }
            }

        } else {
            Toast.makeText(getContext(), "Miss!", Toast.LENGTH_SHORT).show();
        }

        bossHpBar.setProgress(battle.getBoss().getCurrentHp());
        bossHpText.setText("HP: " + battle.getBoss().getCurrentHp() + "/" + battle.getBoss().getMaxHp());

        if (battle.getRemainingAttacks() == 0 && !battle.getBoss().isDefeated()) {
            if (user.getEquipped() != null) {
                for (EquippedItem item : user.getEquipped().values()) {
                    if (item.getBonusType() == BonusType.ATTACK_NUM) {
                        if (item.isBonusTriggered()) {
                            Toast.makeText(getContext(), "Bonus: Dobili ste dodatni napad!", Toast.LENGTH_LONG).show();
                            battle.setRemainingAttacks(1);
                            break;
                        }
                    }
                }
            }
        }

        // Ažuriraj tekst o broju preostalih napada
        remainingAttacksText.setText("Remaining attacks: " + battle.getRemainingAttacks() + "/5");

        // Ažuriraj stanje borbe u bazi
        battleRepository.updateBattle(battle, t -> {});

        // Proveri da li je borba SADA završena (tek nakon provere za bonus)
        if (battle.isFinished()) {

            if (getView() != null) {
                getView().postDelayed(this::showResults, 1200);
            } else {
                showResults(); // U slučaju da je view uništen
                battle.setActiveWeapon(null);
                activeWeaponIcon.setVisibility(View.GONE);
            }
        }

        isAttackInProgress = false;

    }

    private void showResults() {
        attackButton.setEnabled(false);
        treasureChestImage.setVisibility(View.VISIBLE);
        coinsEarnedText.setVisibility(View.VISIBLE);

        if (battle == null || user == null) return;

        boolean userWon = battle.getBoss().isDefeated();

        String message = userWon ? "Pobedio si!" : "Izgubio si!";
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        user.reduceLifespan();

        giveReward();

        // --- Sačuvaj borbu, ali je ne briši ---
         battle.setFinished(false); // opcionalno, možeš ga setovati da znaš da je završena
         battle.setRemainingAttacks(5);
        battleRepository.updateBattle(battle, t -> {});

        battle.setActiveWeapon(null);
        activeWeaponIcon.setVisibility(View.GONE);


        if (userWon) {
            int nextLevel = battle.getBoss().getLevel() + 1;
            saveBossLevel(nextLevel);
        } else {
            persistBossState(battle.getBoss().getLevel(), battle.getBoss().getCurrentHp());
        }

        UserRepository.getInstance(requireContext()).updateUser(user);
    }


    private void giveReward() {
        if (user == null || battle == null) return;

        treasureChestImage.setImageResource(R.drawable.sanduk_otvoren);

        int bossMaxHp = battle.getBoss().getMaxHp();
        int bossCurrentHp = battle.getBoss().getCurrentHp();
        boolean defeated = battle.getBoss().isDefeated();
        boolean didAtLeastHalfDamage = (bossMaxHp - bossCurrentHp) * 2 >= bossMaxHp;

        int baseCoins = battle.calculateCoins();
        int coinsReward = 0;
        int equipmentChance = 0;

        if (defeated) {
            coinsReward = baseCoins;
            equipmentChance = 20;


            clearBossState();
            battle.getBoss().levelUp(); // podigni level i resetuj HP
            saveBossLevel(battle.getBoss().getLevel());
            battle.setRemainingAttacks(5);
            battleRepository.updateBattle(battle, t -> {});



        } else if (didAtLeastHalfDamage) {
            coinsReward = baseCoins / 2;
            equipmentChance = 10;
        }

        coinsReward = (int) Math.round(coinsReward * (1.0 + getMoneyBoostPercent(user)));

        String equipmentMsg = "";
        Random random = new Random();

        if (equipmentChance > 0 && random.nextInt(100) < equipmentChance) {
            if (random.nextInt(100) < 95) {
                // Dodaj ITEM (odeća / napitak)
                Map<String, Item> allItems = GameData.getAllItems();
                List<String> ids = new ArrayList<>(allItems.keySet());

                // Izaberi nasumičan Item ID
                String randomItemId = ids.get(random.nextInt(ids.size()));
                Item baseItem = allItems.get(randomItemId);

                UserItem clothing = new UserItem();
                clothing.setItemId(baseItem.getId());                // <-- koristi pravi ID iz GameData
                clothing.setQuantity(1);
                clothing.setBonusType(baseItem.getBonusType());      // koristi bonus iz GameData
                clothing.setCurrentBonus(baseItem.getBonusValue());
                clothing.setLifespan(baseItem.getLifespan());

                if (user.getUserItems() == null) {
                    user.setUserItems(new HashMap<>());
                }
                user.getUserItems().put(clothing.getItemId(), clothing);

                equipmentMsg = " +1 Item (" + baseItem.getName() + ")";
            } else {
                // Dodaj WEAPON
                Map<String, Weapon> allWeapons = GameData.getAllWeapons();
                List<String> weaponIds = new ArrayList<>(allWeapons.keySet());

                // Izaberi nasumično oružje
                String randomWeaponId = weaponIds.get(random.nextInt(weaponIds.size()));
                Weapon baseWeapon = allWeapons.get(randomWeaponId);

                UserWeapon weapon = new UserWeapon();
                weapon.setWeaponId(baseWeapon.getId());              // <-- koristi pravi ID iz GameData
                weapon.setLevel(baseWeapon.getLevel());
                weapon.setCurrentBoost(baseWeapon.getBoost());
                weapon.setBoostType(baseWeapon.getBoost_type());

                if (user.getUserWeapons() == null) {
                    user.setUserWeapons(new HashMap<>());
                }
                user.getUserWeapons().put(weapon.getWeaponId(), weapon);

                equipmentMsg = " +1 Weapon (" + baseWeapon.getName() + ")";
            }
        }


        user.setCoins(user.getCoins() + coinsReward);
        String text = "Coins earned: " + coinsReward + " (Total: " + user.getCoins() + ")";
        if (!equipmentMsg.isEmpty()) text += equipmentMsg;
        coinsEarnedText.setText(text);

        if (defeated) {
            clearBossState();
            saveBossLevel(battle.getBoss().getLevel() + 1);
        } else {
            persistBossState(battle.getBoss().getLevel(), battle.getBoss().getCurrentHp());
        }

        UserRepository.getInstance(requireContext()).updateUser(user);
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit().putLong("user_coins", user.getCoins()).apply();
    }

    private int calculateTotalPP(User u) {
        double basePP = u.getPowerPoints();

        double permanentMultiplier = 1.0;

        if (u.getEquipped() != null) {
            for (EquippedItem item : u.getEquipped().values()) {
                if (item.getBonusType() == BonusType.PERMANENT_PP) {
                    permanentMultiplier *= (1.0 + item.getCurrentBonus());
                }
            }
        }

        if (u.getUserWeapons() != null) {
            for (UserWeapon weapon : u.getUserWeapons().values()) {
                if (weapon.getBoostType() == BonusType.PERMANENT_PP) {
                    permanentMultiplier *= (1.0 + weapon.getCurrentBoost());
                }
            }
        }

        double ppAfterPermanentBonuses = Math.round(basePP * permanentMultiplier);

        double totalTemporaryBonusValue = 0.0;

        if (u.getEquipped() != null) {
            for (EquippedItem item : u.getEquipped().values()) {
                if (item.getBonusType() == BonusType.TEMPORARY_PP) {
                    totalTemporaryBonusValue += (ppAfterPermanentBonuses * item.getCurrentBonus());
                }
            }
        }

        double finalPP = ppAfterPermanentBonuses + totalTemporaryBonusValue;

        return (int) Math.round(finalPP);
    }


    private int calculateSuccessRate(User u, int baseRate) {
        double finalRate = baseRate;
        if (u.getEquipped() != null) {
            for (EquippedItem item : u.getEquipped().values()) {
                if (item.getBonusType() == BonusType.SUCCESS_PERCENTAGE) {
                    finalRate += item.getCurrentBonus();
                }
            }
        }
        return clamp((int) Math.round(finalRate), 1, 100);
    }


    private double getMoneyBoostPercent(User u) {
        double sum = 0.0;
        if (u.getEquipped() != null)
            for (EquippedItem it : u.getEquipped().values())
                if (it.getBonusType() == BonusType.MONEY_BOOST) sum += it.getCurrentBonus();
        if (u.getUserWeapons() != null)
            for (UserWeapon w : u.getUserWeapons().values())
                if (w.getBoostType() == BonusType.MONEY_BOOST) sum += w.getCurrentBoost();
        return Math.max(0.0, sum);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void persistBossState(int level, int currentHp) {
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit().putInt(SP_BOSS_LEVEL, level).putInt(SP_BOSS_HP, currentHp).apply();
    }

    private void clearBossState() {
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit().remove(SP_BOSS_HP).apply();
    }

    private void saveBossLevel(int nextLevel) {
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit().putInt(SP_BOSS_LEVEL, nextLevel).apply();
    }

    private int loadBossLevelOrDefault(int def) {
        SharedPreferences sp = requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (!sp.contains(SP_BOSS_LEVEL)) return def;
        return sp.getInt(SP_BOSS_LEVEL, def);
    }

    private void restoreBossHpIfExists() {
        if (battle == null) return;
        SharedPreferences sp = requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        int savedHp = sp.getInt(SP_BOSS_HP, -1);
        if (savedHp >= 0) {
            battle.getBoss().setCurrentHp(savedHp); // direktno postavljanje
        }
    }


    private void showEquipmentSelectionDialog() {
        List<EquipmentDisplay> equipmentList = new ArrayList<>();

        // Weapons
        Map<String, Weapon> allWeapons = GameData.getAllWeapons();
        if (user != null && user.getUserWeapons() != null) {
            for (UserWeapon uw : user.getUserWeapons().values()) {
                Weapon weaponData = allWeapons.get(uw.getWeaponId());
                if (weaponData != null) {
                    int resId = getResourceIdByName(weaponData.getImage());
                    equipmentList.add(new EquipmentDisplay(
                            weaponData.getId(),
                            weaponData.getName(),
                            weaponData.getDescription(),
                            weaponData.getBoost(),
                            resId
                    ));
                }
            }
        }

        Map<String, Item> allItems = GameData.getAllItems();
        if (user != null && user.getUserItems() != null) {
//            Toast.makeText(requireContext(),
//                    "User ima " + user.getUserItems().size() + " item(a)",
//                    Toast.LENGTH_SHORT).show();

            for (UserItem ui : user.getUserItems().values()) {
//                Toast.makeText(requireContext(),
//                        "Obrađujem itemId: " + ui.getItemId(),
//                        Toast.LENGTH_SHORT).show();

                Item itemData = allItems.get(ui.getItemId());
                if (itemData != null) {
//                    Toast.makeText(requireContext(),
//                            "Nađen item: " + itemData.getName(),
//                            Toast.LENGTH_SHORT).show();

                    int resId = getResourceIdByName(itemData.getImage());
                    equipmentList.add(new EquipmentDisplay(
                            itemData.getId(),
                            itemData.getName(),
                            itemData.getDescription(),
                            itemData.getBonusValue(),
                            resId
                    ));
                } else {
                    Toast.makeText(requireContext(),
                            "Nema podataka u GameData za id: " + ui.getItemId(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(requireContext(), "User nema iteme.", Toast.LENGTH_SHORT).show();
        }


        if (equipmentList.isEmpty()) {
            Toast.makeText(requireContext(), "Nemate dostupnu opremu.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_weapon_list, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerWeapons);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        EquipmentListAdapter adapter = new EquipmentListAdapter(equipmentList);
        recyclerView.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Vaša oprema")
                .setView(dialogView)
                .setNegativeButton("Zatvori", null)
                .create()
                .show();
    }



    /**
     * Helper metoda koja pronalazi drawable resource ID po imenu
     */
    private int getResourceIdByName(String resourceName) {
        if (resourceName == null) return R.drawable.ic_face; // default
        int resId = requireContext().getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());
        return resId != 0 ? resId : R.drawable.ic_face; // fallback
    }



    // === Sensor ===
    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        long curTime = System.currentTimeMillis();
        if (curTime - lastUpdate > 100) {
            float delta = Math.abs(x + y + z - lastX - lastY - lastZ);
            if (delta > 15 && !isAttackInProgress) handleAttack();
            lastX = x; lastY = y; lastZ = z; lastUpdate = curTime;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}