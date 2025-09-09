package com.example.rpgapp.fragments.battle;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.example.rpgapp.R;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Battle;
import com.example.rpgapp.model.BonusType;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.model.UserWeapon;
import com.example.rpgapp.services.TaskService;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BossBattleFragment extends Fragment implements SensorEventListener {

    private static final String SP_NAME = "game_prefs";
    private static final String SP_BOSS_LEVEL = "boss_level";
    private static final String SP_BOSS_HP = "boss_hp";

    private Battle battle;
    private User user;

    private ImageView bossImageView, treasureChestImage;
    private ProgressBar bossHpBar, userPpBar;
    private TextView successRateText, remainingAttacksText, coinsEarnedText;
    private Button attackButton;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastUpdate = 0;

    private int successRate = 67; // default
    private boolean isAttackInProgress = false;

    private TaskService taskService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boss_battle, container, false);

        // UI bind
        bossImageView = view.findViewById(R.id.bossImageView);
        treasureChestImage = view.findViewById(R.id.treasureChestImage);
        bossHpBar = view.findViewById(R.id.bossHpBar);
        userPpBar = view.findViewById(R.id.userPpBar);
        successRateText = view.findViewById(R.id.successRateText);
        remainingAttacksText = view.findViewById(R.id.remainingAttacksText);
        coinsEarnedText = view.findViewById(R.id.coinsEarnedText);
        attackButton = view.findViewById(R.id.attackButton);

        // Load user
        user = UserRepository.getInstance(requireContext()).getLoggedInUser();
        if (user == null) user = new User("Player1", "avatar1");

        // Restore battle state
        int bossLevel = loadBossLevelOrDefault(1);
        battle = new Battle(user, bossLevel);
        restoreBossHpIfExists();

        int totalPP = calculateTotalPP(user);
        userPpBar.setMax(Math.max(totalPP, 1));
        userPpBar.setProgress(totalPP);

        bossHpBar.setMax(battle.getBoss().getMaxHp());
        bossHpBar.setProgress(battle.getBoss().getCurrentHp());

        remainingAttacksText.setText("Remaining attacks: " + battle.getRemainingAttacks() + "/5");
        coinsEarnedText.setVisibility(View.GONE);
        treasureChestImage.setVisibility(View.GONE);
        treasureChestImage.setImageResource(R.drawable.sanduk);

        attackButton.setOnClickListener(v -> handleAttack());

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        taskService = TaskService.getInstance(requireContext());
        taskService.getSuccessRate().observe(getViewLifecycleOwner(), rate -> {
            successRate = clamp((int) Math.round(rate), 1, 100);
            successRateText.setText("Chance to hit: " + successRate + "%");
        });

        return view;
    }

    // === BORBA ===

    private void handleAttack() {
        if (isAttackInProgress || battle.isFinished()) return;
        isAttackInProgress = true;

        boolean hit = battle.attack(successRate);
        if (hit) {
            bossImageView.setImageResource(R.drawable.boss_borba);
            bossImageView.postDelayed(() -> bossImageView.setImageResource(R.drawable.boss), 300);
            Toast.makeText(getContext(), "Hit!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Miss!", Toast.LENGTH_SHORT).show();
        }

        bossHpBar.setProgress(battle.getBoss().getCurrentHp());
        remainingAttacksText.setText("Remaining attacks: " + battle.getRemainingAttacks() + "/5");

        if (battle.isFinished()) showResults();
        isAttackInProgress = false;
    }

    private void showResults() {
        attackButton.setEnabled(false);
        treasureChestImage.setVisibility(View.VISIBLE);
        coinsEarnedText.setVisibility(View.VISIBLE);

        consumeTemporaryEquipment(user);
        giveReward();
    }

    // === NAGRADE ===

    private void giveReward() {
        if (user == null) return;

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
        } else if (didAtLeastHalfDamage) {
            coinsReward = baseCoins / 2;
            equipmentChance = 10;
        }

        coinsReward = (int) Math.round(coinsReward * (1.0 + getMoneyBoostPercent(user)));

        String equipmentMsg = "";
        if (equipmentChance > 0 && new Random().nextInt(100) < equipmentChance) {
            if (new Random().nextInt(100) < 95) {
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

    // === HELPERI ===

    private int calculateTotalPP(User u) {
        int total = u.getPowerPoints();
        if (u.getEquipped() != null) {
            for (UserItem it : u.getEquipped().values()) {
                if (it.getBonusType() == BonusType.PERMANENT_PP || it.getBonusType() == BonusType.TEMPORARY_PP)
                    total += (int) it.getCurrentBonus();
            }
        }
        return Math.max(total, 0);
    }

    private void consumeTemporaryEquipment(User u) {
        if (u.getEquipped() == null) return;
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
        return requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getInt(SP_BOSS_LEVEL, def);
    }

    private void restoreBossHpIfExists() {
        SharedPreferences sp = requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        int savedHp = sp.getInt(SP_BOSS_HP, -1);
        if (savedHp >= 0) {
            int max = battle.getBoss().getMaxHp();
            int hp = Math.min(savedHp, max);
            battle.getBoss().reduceHp(max - hp);
        }
    }

    // === SENSOR ===

    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        long curTime = System.currentTimeMillis();
        if ((curTime - lastUpdate) > 100) {
            long diffTime = curTime - lastUpdate;
            lastUpdate = curTime;

            float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;
            if (speed > 800) handleAttack();

            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
