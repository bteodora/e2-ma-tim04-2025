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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SpecialMissionViewModel extends AndroidViewModel {

    private final SpecialMissionRepository repository;
    private final MutableLiveData<SpecialMission> currentMission = new MutableLiveData<>();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public SpecialMissionViewModel(@NonNull Application application) {
        super(application);
        repository = SpecialMissionRepository.getInstance(application);
    }

    public LiveData<SpecialMission> getCurrentMission() {
        return currentMission;
    }

//    public void loadMission(String allianceId) {
//        SpecialMission mission = repository.getMission(allianceId).getValue();
//        currentMission.setValue(mission);
//    }
public void loadMission(String allianceId) {
    firestore.collection("specialMissions")
            .whereEqualTo("allianceId", allianceId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e("SpecialMissionVM", "Firestore error", e);
                    Toast.makeText(getApplication(), "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshot == null || snapshot.isEmpty()) {
                    Log.d("SpecialMissionVM", "No mission found for allianceId: " + allianceId);
                    currentMission.postValue(null); // UI zna da nema aktivne misije
                    return;
                }

                SpecialMission mission = snapshot.getDocuments().get(0).toObject(SpecialMission.class);
                Log.d("SpecialMissionVM", "Mission loaded: " + mission);
                currentMission.postValue(mission);
            });
}



    public void startSpecialMission(Alliance alliance) {
        if (alliance == null || alliance.getMemberIds() == null) return;

        // Kreiraj novu misiju prema broju članova saveza
        SpecialMission mission = new SpecialMission(alliance.getMemberIds().size());
        mission.setAllianceId(alliance.getAllianceId());
        mission.setActive(true);

        firestore.collection("specialMissions")
                .add(mission)
                .addOnSuccessListener(docRef -> {
                    Log.d("SpecialMissionVM", "Special mission started with ID: " + docRef.getId());
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



    public void completeTask(int taskIndex, int userProgressIncrement, int allianceProgressIncrement, String userId) {
        SpecialMission mission = currentMission.getValue();
        if (mission == null || userId == null) return;

        // Automatski izračunaj HP reduction
        int hpReduction = calculateHpReduction(mission.getTasks().get(taskIndex));

        // Update mission progress
        MissionTask task = mission.getTasks().get(taskIndex);
        boolean valid = task.incrementProgress(userId); // proveri da li je task uspešno urađen
        if (!valid) return;

        mission.reduceBossHP(hpReduction);
        mission.increaseUserProgress(userId, userProgressIncrement);
        mission.increaseAllianceProgress(hpReduction);

        currentMission.setValue(mission);

        // Sačuvaj promene u repo
        repository.updateMission(mission, new UserRepository.RequestCallback() {
            @Override
            public void onSuccess() { }

            @Override
            public void onFailure(Exception e) { e.printStackTrace(); }
        });

        // Ako je boss pobijen
        if (mission.getBossHP() <= 0) {
            mission.endMission();
            mission.setActive(false);
            repository.updateMission(mission, new UserRepository.RequestCallback() {
                @Override
                public void onSuccess() { }

                @Override
                public void onFailure(Exception e) { e.printStackTrace(); }
            });
        }
    }

    public int calculateHpReduction(MissionTask task) {
        String name = task.getName();

        // Veoma lak, Laki, Normalni ili Važni zadatak
        if (name.equals("Veoma lak") || name.equals("Laki") || name.equals("Normalni") || name.equals("Važni")) {
            if (name.equals("Laki") || name.equals("Normalni")) return 2; // udvostručeno
            return 1; // Veoma lak ili Važni
        }

        // Ostali zadaci
        switch (name) {
            case "Kupovina u prodavnici":
            case "Udarac u regularnoj borbi":
                return 2;
            case "Ostali zadaci":
                return 4;
            case "Bez nerešenih zadataka":
                return 10;
            case "Poruka u savezu":
                return 4;
            default:
                return 1;
        }
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

        // 1️⃣ Izračunaj nagradu za sledećeg bossa
        int previousBossLevel = mission.getCompletedBossCount(); // broj već pobedjenih bossova
        int nextBossCoins = calculateNextBossRewardCoins(previousBossLevel);
        int coinsReward = nextBossCoins / 2; // 50% od nagrade za sledećeg bossa

        int potionsReward = 1; // 1 napitak
        int clothesReward = 1; // 1 komad odeće

        // 2️⃣ Prođi kroz sve članove saveza
        for (String userId : mission.getUserTaskProgress().keySet()) {
            Map<String, Object> reward = new HashMap<>();
            reward.put("coins", coinsReward);
            reward.put("potions", potionsReward);
            reward.put("clothes", clothesReward);
            reward.put("badge", mission.getUserTaskProgress().get(userId)); // broj uspešno urađenih specijalnih zadataka

            // 3️⃣ Sačuvaj nagrade za korisnika
            userRepo.updateUserReward(userId, reward);
        }

        // 4️⃣ Obeleži misiju kao završenu
        mission.endMission();
        mission.incrementCompletedBossCount(); // povećaj broj pobedjenih bossova

        FirebaseFirestore.getInstance()
                .collection("alliances")
                .document(mission.getAllianceId())
                .update("missionStarted", false);


        // 5️⃣ Sačuvaj promene u repozitorijumu
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

    /** Izračunava koliko HP bossu treba da se oduzme za dati zadatak */


//    public void forceEndMission() {
//        SpecialMission mission = currentMission.getValue();
//        if (mission == null) return;
//
//        mission.setActive(false);  // zatvori misiju
//        mission.endMission();
//
//        repository.updateMission(mission, new UserRepository.RequestCallback() {
//            @Override
//            public void onSuccess() {
//                currentMission.postValue(mission); // odmah osveži UI
//                Log.d("SpecialMissionVM", "Misija uspešno prekinuta i sačuvana u bazi.");
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//                Log.e("SpecialMissionVM", "Greška prilikom prekidanja misije", e);
//            }
//        });
//    }


}