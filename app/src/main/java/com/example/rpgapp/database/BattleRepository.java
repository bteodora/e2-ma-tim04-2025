package com.example.rpgapp.database;

import androidx.annotation.NonNull;

import com.example.rpgapp.model.Battle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BattleRepository {

    private final FirebaseFirestore db;
    private final CollectionReference battlesRef;
    private final FirebaseUser currentUser;

    public BattleRepository() {
        db = FirebaseFirestore.getInstance();
        battlesRef = db.collection("battles");
        currentUser = FirebaseAuth.getInstance().getCurrentUser(); // direktno iz FirebaseAuth
    }

    private String getCurrentUserId() {
        if (currentUser != null) return currentUser.getUid();
        return null;
    }

    // --- Dodavanje borbe ---
//    public void addBattle(Battle battle, OnCompleteListener<Void> listener) {
//        String userId = getCurrentUserId();
//        if (userId == null) return;
//
//        battle.setUserId(userId); // postavljamo userId
//        battlesRef.document(battle.getBattleId())
//                .set(battle)
//                .addOnCompleteListener(listener);
//    }
    public void addBattle(Battle battle, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        battle.setUserId(userId);
        battlesRef.add(battle)
                .addOnCompleteListener(task -> {
                    if (listener != null) listener.onComplete(task.isSuccessful() ?
                            Tasks.forResult(null) : Tasks.forException(task.getException()));
                });
    }


    // --- Dohvatanje borbi trenutno ulogovanog korisnika ---
    public void getBattlesForCurrentUser(OnCompleteListener<List<Battle>> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onComplete(Tasks.forResult(new ArrayList<>()));
            return;
        }

        battlesRef.whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    List<Battle> battles = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            battles.add(doc.toObject(Battle.class));
                        }
                    }
                    listener.onComplete(Tasks.forResult(battles));
                });
    }

    // --- Dohvatanje borbe po ID-u za trenutno ulogovanog korisnika ---
    public void getBattleById(String battleId, OnCompleteListener<Battle> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onComplete(Tasks.forResult(null));
            return;
        }

        battlesRef.document(battleId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Battle battle = task.getResult().toObject(Battle.class);
                        if (battle != null && userId.equals(battle.getUserId())) {
                            listener.onComplete(Tasks.forResult(battle));
                        } else {
                            listener.onComplete(Tasks.forResult(null));
                        }
                    } else {
                        listener.onComplete(Tasks.forResult(null));
                    }
                });
    }

    // --- Update borbe trenutno ulogovanog korisnika ---
    public void updateBattle(Battle battle, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null || !userId.equals(battle.getUserId())) return;

        battlesRef.document(battle.getBattleId())
                .set(battle)
                .addOnCompleteListener(listener);
    }

    // --- Brisanje borbe trenutno ulogovanog korisnika ---
    public void deleteBattle(String battleId, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        battlesRef.document(battleId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Battle battle = task.getResult().toObject(Battle.class);
                        if (battle != null && userId.equals(battle.getUserId())) {
                            battlesRef.document(battleId)
                                    .delete()
                                    .addOnCompleteListener(listener);
                        }
                    }
                });
    }
}
