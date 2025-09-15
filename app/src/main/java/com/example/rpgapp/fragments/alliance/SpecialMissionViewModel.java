package com.example.rpgapp.fragments.alliance;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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



    // Za obične korisničke Task-ove
    public int calculateHpReductionFromTask(Task task) {
        if (task == null || task.getTitle() == null) return 0;

        String title = task.getTitle().trim().toLowerCase();

        if (title.equals("veoma lak") || title.equals("važni")) {
            return 1;
        }
        if (title.equals("laki") || title.equals("normalni")) {
            return 2;
        }

        return 0; // ako nije nijedan od poznatih
    }
    // Za MissionTask-ove
    public int calculateHpReductionFromMissionTask(MissionTask missionTask) {
        if (missionTask == null || missionTask.getName() == null) return 0;

        String name = missionTask.getName().trim().toLowerCase();

        switch (name) {
            case "kupovina u prodavnici":
            case "udarac u regularnoj borbi":
                return 2;
            case "ostali zadaci":
            case "poruka u savezu":
                return 4;
            default:
                return 10;
        }
    }

    // U SpecialMissionViewModel
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

//    public boolean areAllTasksCompleted() {
//        SpecialMission mission = currentMission.getValue();
//        if (mission == null) return false;
//        return mission.getTasks().stream()
//                .allMatch(t -> t.isCompleted(currentUserId)); // currentUserId možeš čuvati u VM
//    }

    public void reduceHP(int amount, String userId, MissionTask task) {
        SpecialMission mission = currentMission.getValue();
        if (mission == null) return;

        // Smanjenje HP bossa
        mission.reduceBossHP(amount);
        mission.increaseUserProgress(userId, amount);
        mission.increaseAllianceProgress(amount);

        // Update task progres
        task.incrementProgress(userId);

        // Snimi u Firestore
        firestore.collection("specialMissions")
                .document(mission.getMissionId())
                .set(mission)
                .addOnSuccessListener(aVoid -> {
                    _taskCompletedLiveData.setValue(task);
                    currentMission.postValue(mission);
                });
    }

    public void checkMissionStatus(SpecialMission mission) {
        if (mission == null) return;

        // Ako HP = 0 -> boss poražen
        if (mission.getBossHP() <= 0) {
            mission.endMission();
            // ovde možeš da dodeliš nagrade
        }

        // Ako je isteklo 14 dana -> vreme je prošlo
        if (System.currentTimeMillis() - mission.getStartTime() > mission.getDurationMillis()) {
            mission.endMission();
            // nagrade ili neuspeh, zavisi od logike
        }
    }
    public void abortMission() {
        SpecialMission mission = currentMission.getValue();
        if (mission == null) return;

        mission.setActive(false);
        repository.updateMission(mission, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() { currentMission.postValue(null); }
            @Override
            public void onFailure(Exception e) { e.printStackTrace(); }
        });
    }

    public boolean isUserEligible(String userId) {
        SpecialMission mission = currentMission.getValue();
        return mission != null && mission.getUserTaskProgress().containsKey(userId);
    }
    public void refreshMission(String allianceId) {
        repository.getMission(allianceId).observeForever(mission -> {
            if (mission != null) {
                currentMission.postValue(mission);
            }
        });
    }



    private int calculateNextBossRewardCoins(int previousBossLevel) {
        // prvi boss = 200, svaki naredni +20%
        double base = 200;
        return (int) (base * Math.pow(1.2, previousBossLevel));
    }

    public void claimRewards() {
        SpecialMission mission = currentMission.getValue();
        if (mission == null || mission.isActive()) return;

        UserRepository userRepo = UserRepository.getInstance(getApplication());
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // ⬅ ovde definišeš db

        // 1️⃣ Izračunaj nagradu za sledećeg bossa
        int previousBossLevel = mission.getCompletedBossCount(); // broj već pobedjenih bossova
        int nextBossCoins = calculateNextBossRewardCoins(previousBossLevel);
        int coinsReward = nextBossCoins / 2; // 50% od nagrade za sledećeg bossa

        int potionsReward = 1; // 1 napitak
        int clothesReward = 1; // 1 komad odeće

        // 2️⃣ Prođi kroz sve članove saveza
        for (String userId : mission.getUserTaskProgress().keySet()) {

            // 2a️⃣ Dobij bedž za korisnika
            int newBossCount = previousBossLevel + 1; // novi boss koji je pobedjen
            String badgeImage = getBadgeForBossCount(newBossCount);

            Map<String, Object> reward = new HashMap<>();
            reward.put("coins", coinsReward);
            reward.put("potions", potionsReward);
            reward.put("clothes", clothesReward);

            // 2b️⃣ Dodaj bedž u Firestore
            db.collection("users").document(userId)
                    .update("badges", FieldValue.arrayUnion(badgeImage));

            // 2c️⃣ Sačuvaj ostale nagrade preko repozitorijuma
            userRepo.updateUserReward(userId, reward);
        }

        // 3️⃣ Obeleži misiju kao završenu
        mission.endMission();
        mission.incrementCompletedBossCount(); // povećaj broj pobedjenih bossova

        db.collection("alliances")
                .document(mission.getAllianceId())
                .update("missionStarted", false);

        // 4️⃣ Sačuvaj promene u repozitorijumu
        repository.updateMission(mission, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                Log.d("SpecialMissionVM", "Rewards claimed for all members");
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
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


    public LiveData<Boolean> hasActiveMission(String allianceId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        repository.getMission(allianceId).observeForever(mission -> {
            result.postValue(mission != null && mission.isActive());
        });
        return result;
    }


    public void startMission(Alliance alliance) {
        if (alliance == null) return;

        SpecialMission newMission = new SpecialMission(alliance.getMemberIds().size());
        newMission.setAllianceId(alliance.getAllianceId());

        repository.saveMission(newMission, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() {
                currentMission.postValue(newMission);
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }
    public LiveData<SpecialMission> refreshCurrentMission(String allianceId) {
        MutableLiveData<SpecialMission> liveData = new MutableLiveData<>();
        firestore.collection("specialMissions")
                .whereEqualTo("allianceId", allianceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        SpecialMission mission = querySnapshot.getDocuments().get(0).toObject(SpecialMission.class);
                        liveData.setValue(mission);
                    } else {
                        liveData.setValue(null);
                    }
                });
        return liveData;
    }


    public void forceEndMission(String allianceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("specialMissions")
                .document(allianceId)
                .update("isActive", false);

        db.collection("alliances")
                .document(allianceId)
                .update("missionStarted", false);
    }


}