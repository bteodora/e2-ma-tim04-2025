package com.example.rpgapp.fragments.alliance;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.ItemType;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.Task;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserItem;
import com.example.rpgapp.tools.GameData;
import java.util.stream.Collectors;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpecialMissionViewModel extends AndroidViewModel {

    private final SpecialMissionRepository repository;
    private final MutableLiveData<SpecialMission> currentMission = new MutableLiveData<>();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    // LiveData da obavesti fragment kada je task kompletiran
    private final MutableLiveData<MissionTask> _taskCompletedLiveData = new MutableLiveData<>();
    public LiveData<MissionTask> taskCompletedLiveData = _taskCompletedLiveData;

    public LiveData<SpecialMission> getCurrentMission() {
        return currentMission;
    }
    public void setCurrentMission(SpecialMission mission) {
        currentMission.postValue(mission);
    }

    private boolean rewardsAlreadyClaimed = false;

    public boolean isRewardsAlreadyClaimed() {
        return rewardsAlreadyClaimed;
    }

    public void setRewardsAlreadyClaimed(boolean claimed) {
        rewardsAlreadyClaimed = claimed;
    }

    public SpecialMissionViewModel(@NonNull Application application) {
        super(application);
        repository = SpecialMissionRepository.getInstance(application);
    }




    public void loadMission(String allianceId, long delayMillis) {
        new android.os.Handler().postDelayed(() -> {
            firestore.collection("specialMissions")
                    .whereEqualTo("allianceId", allianceId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            SpecialMission mission = querySnapshot.getDocuments().get(0).toObject(SpecialMission.class);
                            currentMission.postValue(mission);
                            Log.d("SpecialMissionVM", "Mission loaded after delay: " + mission);
                        } else {
                            currentMission.postValue(null);
                            Log.d("SpecialMissionVM", "No mission found after delay for allianceId: " + allianceId);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("SpecialMissionVM", "Failed to load mission after delay", e));
        }, delayMillis);
    }


    public void startSpecialMission(Alliance alliance) {
        if (alliance == null || alliance.getMemberIds() == null) return;

        // Kreiraj novu misiju prema broju članova saveza
        SpecialMission mission = new SpecialMission(alliance.getMemberIds().size());
        String missionId = firestore.collection("specialMissions").document().getId(); // kreiraj ID unapred
        mission.setMissionId(missionId);
        mission.setAllianceId(alliance.getAllianceId());
        mission.setActive(true);

        firestore.collection("specialMissions")
                .document(missionId)
                .set(mission)
                .addOnSuccessListener(docRef -> {
                    //Log.d("SpecialMissionVM", "Special mission started with ID: " + docRef.getId());
                    currentMission.postValue(mission);

                    // Obavesti saveza da je misija pokrenuta
                    firestore.collection("alliances")
                            .document(alliance.getAllianceId())
                            .update("missionStarted", true);
                })
                .addOnFailureListener(e -> {
                    Log.e("SpecialMissionVM", "Failed to start special mission", e);
                    Toast.makeText(getApplication(), "Failed to start special mission", Toast.LENGTH_SHORT).show();
                });
    }

    public void completeTask(int taskIndex, String missionId, String userId) {
        SpecialMission mission = currentMission.getValue();
        if (mission == null || userId == null) return;

        // 1. Uzmemo task
        List<MissionTask> tasks = mission.getTasks();
        if (tasks == null || taskIndex >= tasks.size()) return;

        MissionTask task = tasks.get(taskIndex);
        if (task == null) return;

        // 2. Inkrement progres
        boolean valid = task.incrementProgress(userId);
        if (!valid) {
            Log.d("SpecialMissionVM", "Task max completions reached for user: " + userId);
            return;
        }

        // 3. Izračunaj koliko HP-a boss gubi
        int hpReduction = task.getHpReductionPerCompletion();

        // 4. Update misije
        mission.reduceBossHP(hpReduction);
        mission.increaseUserProgress(userId, hpReduction); //bilo 1
        mission.increaseAllianceProgress(hpReduction);

        // 5. Snimi nazad izmenjeni task u listu
        tasks.set(taskIndex, task);
        mission.setTasks(tasks);

        // 6. Proveri da li je boss mrtav
        if (mission.getBossHP() <= 0) {
            mission.endMission();
            mission.setActive(false);
        }

        // 7. Snimi sve izmene u Firestore
        FirebaseFirestore.getInstance()
                .collection("specialMissions")
                .document(missionId)
                .set(mission)  // snima celu misiju sa svim poljima
                .addOnSuccessListener(aVoid -> {
                    _taskCompletedLiveData.setValue(task);
                    currentMission.postValue(mission);
                    Log.d("SpecialMissionVM", "Task + mission progress updated for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("SpecialMissionVM", "Error updating mission", e);
                });
    }


    public static class TaskResult {
        public int hpReduction;
        public String missionTaskName; // naziv MissionTask-a koji će biti update-ovan

        public TaskResult(int hpReduction, String missionTaskName) {
            this.hpReduction = hpReduction;
            this.missionTaskName = missionTaskName;
        }
    }

    /**
     * Mapira običan Task na odgovarajući MissionTask i vraća koliko HP-a boss gubi.
     */
    private TaskResult mapTaskToMissionTask(Task task) {
        if (task == null || task.getTitle() == null) return new TaskResult(0, null);

        String title = task.getTitle().trim().toLowerCase();
        int hpReduction;
        String missionTaskName;

        switch (title) {
            case "kupovina u prodavnici":
                hpReduction = 2;
                missionTaskName = "Kupovina u prodavnici";
                break;
            case "udarac u regularnoj borbi":
                hpReduction = 2;
                missionTaskName = "Udarac u regularnoj borbi";
                break;
            case "laki":
            case "normalni":
            case "važni":
                hpReduction = 1;
                missionTaskName = "Laki/Normalni/Važni zadaci";
                break;
            case "ostali zadaci":
                hpReduction = 4;
                missionTaskName = "Ostali zadaci";
                break;
            case "bez nerešenih zadataka":
                hpReduction = 10;
                missionTaskName = "Bez nerešenih zadataka";
                break;
            case "poruka u savezu":
                hpReduction = 4;
                missionTaskName = "Poruka u savezu";
                break;
            default:
                hpReduction = 0;
                missionTaskName = null;
        }

        return new TaskResult(hpReduction, missionTaskName);
    }


    public void handleNormalTaskCompletion(Task task, String userId) {
        SpecialMission mission = currentMission.getValue();
        if (mission == null || !mission.isActive()) return;

        TaskResult result = mapTaskToMissionTask(task);
        if (result.hpReduction == 0 || result.missionTaskName == null) return;

        // Pronađi odgovarajući MissionTask
        MissionTask missionTask = mission.getTasks().stream()
                .filter(t -> t.getName().equals(result.missionTaskName))
                .findFirst()
                .orElse(null);

        if (missionTask == null) return;

        // Inkrement progres
        if (!missionTask.incrementProgress(userId)) return;

        // Update misije
        mission.reduceBossHP(result.hpReduction);
        mission.increaseUserProgress(userId, result.hpReduction);
        mission.increaseAllianceProgress(result.hpReduction);

        FirebaseFirestore.getInstance()
                .collection("specialMissions")
                .document(mission.getMissionId())
                .set(mission)
                .addOnSuccessListener(aVoid -> {
                    _taskCompletedLiveData.setValue(missionTask);
                    currentMission.postValue(mission);
                    Log.d("SpecialMissionVM", "Task + mission progress updated for user: " + userId);
                })
                .addOnFailureListener(e -> Log.e("SpecialMissionVM", "Error updating mission from task completion", e));
    }

    private int calculateNextBossRewardCoins(int previousBossLevel) {
        // prvi boss = 200, svaki naredni +20%
        double base = 200;
        return (int) (base * Math.pow(1.2, previousBossLevel));
    }


    public void claimRewards(RewardMessagesCallback callback) {
        SpecialMission mission = currentMission.getValue();
        if (mission == null || mission.isActive()) {
            callback.onComplete("Misija je aktivna", null);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Random random = new Random();
        int previousBossLevel = mission.getCompletedBossCount();
        int nextBossCoins = calculateNextBossRewardCoins(previousBossLevel);
        int coinsReward = nextBossCoins / 2;

        Map<String, Item> allItems = GameData.getAllItems();
        List<Item> potions = allItems.values().stream()
                .filter(item -> item.getType() == ItemType.POTION)
                .collect(Collectors.toList());

        List<Item> clothes = allItems.values().stream()
                .filter(item -> item.getType() == ItemType.CLOTHING)
                .collect(Collectors.toList());

        Map<String, String> rewardMessages = new HashMap<>();
        final int[] tasksCompleted = {0};
        int totalUsers = mission.getUserTaskProgress().size();

        for (String userId : mission.getUserTaskProgress().keySet()) {
            UserRepository.getInstance(getApplication()).getUserById(userId, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    if (user != null) {
                        Item potionReward = potions.get(random.nextInt(potions.size()));
                        Item clothingReward = clothes.get(random.nextInt(clothes.size()));
                        int newBossCount = previousBossLevel + 1;
                        String badgeImage = getBadgeForBossCount(newBossCount);

                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                .update(
                                        "coins", FieldValue.increment(coinsReward),
                                        "badges", FieldValue.arrayUnion(badgeImage)
                                );

                        if (user.getUserItems() == null) user.setUserItems(new HashMap<>());

                        UserItem newPotion = new UserItem();
                        newPotion.setItemId(potionReward.getId());
                        newPotion.setQuantity(1);
                        newPotion.setBonusType(potionReward.getBonusType());
                        newPotion.setCurrentBonus(potionReward.getBonusValue());
                        newPotion.setLifespan(potionReward.getLifespan());
                        newPotion.setDuplicated(false);
                        user.getUserItems().merge(potionReward.getId(), newPotion, (existing, incoming) -> {
                            existing.setQuantity(existing.getQuantity() + 1);
                            return existing;
                        });

                        UserItem newClothing = new UserItem();
                        newClothing.setItemId(clothingReward.getId());
                        newClothing.setQuantity(1);
                        newClothing.setBonusType(clothingReward.getBonusType());
                        newClothing.setCurrentBonus(clothingReward.getBonusValue());
                        newClothing.setLifespan(clothingReward.getLifespan());
                        newClothing.setDuplicated(false);
                        user.getUserItems().merge(clothingReward.getId(), newClothing, (existing, incoming) -> {
                            existing.setQuantity(existing.getQuantity() + 1);
                            return existing;
                        });

                        if (user.getBadges() == null) user.setBadges(new ArrayList<>());
                        if (!user.getBadges().contains(badgeImage)) user.getBadges().add(badgeImage);

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        DocumentReference userRef = db.collection("users").document(userId);

// Potion
                        Map<String, Object> potionUpdate = new HashMap<>();
                        potionUpdate.put("userItems." + potionReward.getId() + ".itemId", potionReward.getId());
                        potionUpdate.put("userItems." + potionReward.getId() + ".bonusType", potionReward.getBonusType());
                        potionUpdate.put("userItems." + potionReward.getId() + ".currentBonus", potionReward.getBonusValue());
                        potionUpdate.put("userItems." + potionReward.getId() + ".lifespan", potionReward.getLifespan());
                        potionUpdate.put("userItems." + potionReward.getId() + ".duplicated", false);
                        potionUpdate.put("userItems." + potionReward.getId() + ".quantity", FieldValue.increment(1));

                        userRef.update(potionUpdate);

// Clothing
                        Map<String, Object> clothingUpdate = new HashMap<>();
                        clothingUpdate.put("userItems." + clothingReward.getId() + ".itemId", clothingReward.getId());
                        clothingUpdate.put("userItems." + clothingReward.getId() + ".bonusType", clothingReward.getBonusType());
                        clothingUpdate.put("userItems." + clothingReward.getId() + ".currentBonus", clothingReward.getBonusValue());
                        clothingUpdate.put("userItems." + clothingReward.getId() + ".lifespan", clothingReward.getLifespan());
                        clothingUpdate.put("userItems." + clothingReward.getId() + ".duplicated", false);
                        clothingUpdate.put("userItems." + clothingReward.getId() + ".quantity", FieldValue.increment(1));

                        userRef.update(clothingUpdate);


                        rewardMessages.put(userId, "💰 " + coinsReward + " coins, "
                                + potionReward.getName() + ", "
                                + clothingReward.getName() + ", "
                                + "badge: " + badgeImage);
                    }

                    tasksCompleted[0]++;
                    if (tasksCompleted[0] == totalUsers) {
                        // Svi korisnici završeni, pozovi callback
                        String finalMessage = String.join("\n", rewardMessages.values());
                        callback.onComplete(finalMessage, null);
                    }
                }

                @Override
                public void onError(Exception e) {
                    tasksCompleted[0]++;
                    if (tasksCompleted[0] == totalUsers) {
                        String finalMessage = String.join("\n", rewardMessages.values());
                        callback.onComplete(finalMessage, e);
                    }
                }
            });
        }

        // Završavanje misije
        mission.endMission();
        mission.incrementCompletedBossCount();
        db.collection("alliances").document(mission.getAllianceId())
                .update("missionStarted", false);

        repository.updateMission(mission, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                Log.d("SpecialMissionVM", "All rewards claimed successfully");
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Callback interfejs
    public interface RewardMessagesCallback {
        void onComplete(String rewardMessages, @Nullable Exception e);
    }


    private String getBadgeForBossCount(int count) {
        switch (count) {
            case 1: return "badge_bronze.png";
            case 2: return "badge_silver.png";
            case 3: return "badge_gold.png";
            case 4: return "badge_platinum.png";
            default: return "badge_legendary.png";
        }
    }

    public void forceEndMission(String allianceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        db.collection("specialMissions")
//                .document(allianceId)
//                .update("isActive", false);
        FirebaseFirestore.getInstance()
                .collection("missions")
                .document(allianceId)
                .delete();


        db.collection("alliances")
                .document(allianceId)
                .update("missionStarted", false);
    }


}