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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
//import com.example.rpgapp.adapters.WeaponListAdapter;
import com.example.rpgapp.adapters.WeaponListAdapter;
import com.example.rpgapp.database.BattleRepository;
import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.model.Battle;
import com.example.rpgapp.model.BonusType;
import com.example.rpgapp.model.Boss;
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
    private Button attackButton, selectWeaponButton;

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



        // Dobavljanje ulogovanog korisnika iz UserRepository, isto kao u TaskPageFragment
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
            activeMission = mission; // čuvamo lokalno
        });

        View view = inflater.inflate(R.layout.fragment_boss_battle, container, false);

        // --- Bind UI ---
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
        selectWeaponButton.setOnClickListener(v -> showWeaponSelectionDialog());

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

//        updateUserPP();

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
                for (UserItem item : user.getEquipped().values()) {
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
//        user.setPowerPoints(user.getBasePowerPoints());
//        updateUserPP();
        resetPPToBase(user);


       // battleRepository.updateBattle(battle, t -> {});


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
                UserItem clothing = new UserItem();
                clothing.setItemId("clothing_" + System.currentTimeMillis());
                clothing.setQuantity(1);
                clothing.setBonusType(BonusType.TEMPORARY_PP);
                clothing.setCurrentBonus(5.0);
                clothing.setLifespan(5);
                if (user.getUserItems() == null) user.setUserItems(new HashMap<>());
                user.getUserItems().put(clothing.getItemId(), clothing);
                equipmentMsg = " +1 Clothing";
            } else {
                UserWeapon weapon = new UserWeapon();
                weapon.setWeaponId("weapon_" + System.currentTimeMillis());
                weapon.setLevel(1);
                weapon.setCurrentBoost(0.05);
                weapon.setBoostType(BonusType.PERMANENT_PP);
                if (user.getUserWeapons() == null) user.setUserWeapons(new HashMap<>());
                user.getUserWeapons().put(weapon.getWeaponId(), weapon);
                equipmentMsg = " +1 Weapon";
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
        //double basePP = u.getBasePowerPoints();   // originalna vrednost pre borbe
        double currentPP = u.getPowerPoints();    // vrednost koja može da se menja u toku igre

        double finalPP = currentPP;


        double permanentMultiplier = 1.0;

        if (u.getEquipped() != null) {
            for (UserItem item : u.getEquipped().values()) {
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

        if (battle != null && battle.getActiveWeapon() != null && battle.getActiveWeapon().getBoostType() == BonusType.PERMANENT_PP) {

            permanentMultiplier *= (1.0 + battle.getActiveWeapon().getCurrentBoost());
        }


        finalPP = currentPP * permanentMultiplier;

        double temporaryBonusValue = 0.0;

        if (u.getEquipped() != null) {
            for (UserItem item : u.getEquipped().values()) {
                if (item.getBonusType() == BonusType.TEMPORARY_PP) {
                    temporaryBonusValue += (currentPP * item.getCurrentBonus());
                }
            }
        }

        if (battle != null && battle.getActiveWeapon() != null && battle.getActiveWeapon().getBoostType() == BonusType.TEMPORARY_PP) {
            temporaryBonusValue += (currentPP * battle.getActiveWeapon().getCurrentBoost());
        }

        finalPP += temporaryBonusValue;
//        u.setPowerPoints((int)Math.round(finalPP));
//        u.setBasePowerPoints((int)Math.round(finalPP));

        return (int) Math.round(finalPP);
    }

    private int calculateSuccessRate(User u, int baseRate) {
        double finalRate = baseRate;
        if (u.getEquipped() != null) {
            for (UserItem item : u.getEquipped().values()) {
                if (item.getBonusType() == BonusType.SUCCESS_PERCENTAGE) {
                    finalRate += item.getCurrentBonus();
                }
            }
        }
        return clamp((int) Math.round(finalRate), 1, 100);
    }


    private void consumeTemporaryEquipment(User u) {
        if (u == null || u.getEquipped() == null) return;
        Map<String, UserItem> eq = u.getEquipped();
        Map<String, String> toRemove = new HashMap<>();
        for (Map.Entry<String, UserItem> e : eq.entrySet()) {
            UserItem it = e.getValue();
            if (it.getBonusType() == BonusType.TEMPORARY_PP) {
                int ls = Math.max(0, it.getLifespan() - 1);
                it.setLifespan(ls);
                if (ls == 0) toRemove.put(e.getKey(), e.getKey());
            }
        }
        for (String key : toRemove.keySet()) eq.remove(key);
        int totalPP = calculateTotalPP(u);
        userPpBar.setMax(Math.max(totalPP, 1));
        userPpBar.setProgress(totalPP);
    }

    private double getMoneyBoostPercent(User u) {
        double sum = 0.0;
        if (u.getEquipped() != null)
            for (UserItem it : u.getEquipped().values())
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

    private void showWeaponSelectionDialog() {
        if (user == null || user.getUserWeapons() == null || user.getUserWeapons().isEmpty()) {
            Toast.makeText(requireContext(), "Nemate oružje.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Weapon> allWeapons = GameData.getAllWeapons();

        List<UserWeapon> weaponsList = new ArrayList<>();
        for (UserWeapon uw : user.getUserWeapons().values()) {
            Weapon weaponData = allWeapons.get(uw.getWeaponId());
            if (weaponData != null) {
                uw.setName(weaponData.getName());
                uw.setCurrentBoost(weaponData.getBoost());
                uw.setImageResourceId(getResourceIdByName(weaponData.getImage()));
                uw.setBoostType(weaponData.getBoost_type());
                weaponsList.add(uw);
            }
        }

        if (weaponsList.isEmpty()) {
            Toast.makeText(requireContext(), "Nemate dostupna oružja.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_weapon_list, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerWeapons);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Napravi AlertDialog i sačuvaj u promenljivu
        AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Izaberite oružje")
                .setView(dialogView)
                .setNegativeButton("Otkaži", null)
                .create();

        // Adapter sa klik listener-om
        WeaponListAdapter adapter = new WeaponListAdapter(weaponsList, weapon -> {
            if (battle != null) {
                battle.setActiveWeapon(weapon);
                updateUserPP();

            }

            activeWeaponIcon.setVisibility(View.VISIBLE);
            Glide.with(activeWeaponIcon.getContext())
                    .asGif()
                    .load(weapon.getImageResourceId())
                    .placeholder(R.drawable.ic_face)
                    .into(activeWeaponIcon);



            Toast.makeText(requireContext(), "Aktivirano: " + weapon.getName(), Toast.LENGTH_SHORT).show();

            // Zatvori dijalog
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        // Prikaži dialog
        dialog.show();
    }


    /**
     * Helper metoda koja pronalazi drawable resource ID po imenu
     */
    private int getResourceIdByName(String resourceName) {
        if (resourceName == null) return R.drawable.ic_face; // default
        int resId = requireContext().getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());
        return resId != 0 ? resId : R.drawable.ic_face; // fallback
    }

    private void updateUserPP() {
        if (user == null) return;

        // Izračunaj ukupne PP sa svim bonusima
        int totalPP = calculateTotalPP(user);

        // Ažuriraj UI
        userPpBar.setMax(Math.max(totalPP, 1));
        userPpBar.setProgress(totalPP);
        userPpText.setText("PP: " + totalPP);

        // Sačuvaj trenutni PP u user objekt (opciono, za kasniju upotrebu)
        user.setPowerPoints(totalPP);
    }

    private void resetPPToBase(User u) {
        if (u == null) return;

        double basePP = u.getBasePowerPoints();
        if (basePP <= 0) basePP = 10; // default vrednost ako base nije inicijalizovan

        double permanentMultiplier = 1.0;
        if (u.getEquipped() != null) {
            for (UserItem item : u.getEquipped().values()) {
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

        int totalPP = (int) Math.round(basePP * permanentMultiplier);

        // Update UI bez prebrisavanja u calculateTotalPP()
        userPpBar.setMax(totalPP);
        userPpBar.setProgress(totalPP);
        userPpText.setText("PP: " + totalPP);

        // Sačuvaj samo ako želiš trajnu vrednost
        u.setPowerPoints(totalPP);
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