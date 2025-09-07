package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.MemberProgressAdapter;
import com.example.rpgapp.adapters.MissionTaskAdapter;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SpecialMissionFragment extends Fragment {

    private SpecialMissionViewModel viewModel;
    private UserRepository userRepository;
    private String currentUserId;

    private TextView textViewBossHP, textViewUserProgress, textViewAllianceProgress, textViewTimeLeft;
    private ProgressBar progressBarBossHP;
    private RecyclerView recyclerViewTasks;
    private MissionTaskAdapter taskAdapter;

    private RecyclerView recyclerViewMembersProgress;
    private MemberProgressAdapter memberProgressAdapter;

    private Handler missionHandler = new Handler();
    private Runnable missionRunnable;
    private static final long MISSION_CHECK_INTERVAL = 1000 * 60; // proverava svake minute

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_special_mission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewBossHP = view.findViewById(R.id.textViewBossHP);
        textViewUserProgress = view.findViewById(R.id.textViewUserProgress);
        textViewAllianceProgress = view.findViewById(R.id.textViewAllianceProgress);
        progressBarBossHP = view.findViewById(R.id.progressBarBossHP);
        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks);
        textViewTimeLeft = view.findViewById(R.id.textViewTimeLeft);
        recyclerViewMembersProgress = view.findViewById(R.id.recyclerViewMembersProgress);

        userRepository = UserRepository.getInstance(requireContext());
        currentUserId = userRepository.getLoggedInUser() != null ? userRepository.getLoggedInUser().getUserId() : null;

        viewModel = new ViewModelProvider(requireActivity()).get(SpecialMissionViewModel.class);
        AllianceViewModel allianceViewModel = new ViewModelProvider(requireActivity()).get(AllianceViewModel.class);

        memberProgressAdapter = new MemberProgressAdapter();
        recyclerViewMembersProgress.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMembersProgress.setAdapter(memberProgressAdapter);

        allianceViewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                Toast.makeText(getContext(), "Učitavam misiju za savez: " + alliance.getAllianceId(), Toast.LENGTH_SHORT).show();
                Log.d("SpecialMissionFragment", "Učitavam misiju za savez: " + alliance.getAllianceId());
                viewModel.loadMission(alliance.getAllianceId());
            }
        });

        allianceViewModel.members.observe(getViewLifecycleOwner(), members -> {
            SpecialMission mission = viewModel.getCurrentMission().getValue();
            Map<String, Integer> progressMap = mission != null ? mission.getUserTaskProgress() : new HashMap<>();
            memberProgressAdapter.setMembersAndProgress(members, progressMap);
        });

        taskAdapter = new MissionTaskAdapter(position -> {
            if (currentUserId != null) {
                SpecialMission mission = viewModel.getCurrentMission().getValue();
                if (mission != null && mission.getTasks() != null && position < mission.getTasks().size()) {
                    MissionTask task = mission.getTasks().get(position);
                    boolean valid = task.incrementProgress(currentUserId);
                    if (valid) {
                        int hpReduction = calculateHpReduction(task);
                        viewModel.completeTask(position, hpReduction, 1, currentUserId);
                        memberProgressAdapter.updateProgress(mission.getUserTaskProgress());
                    }
                }
            }
        }, currentUserId);

        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTasks.setAdapter(taskAdapter);

        viewModel.getCurrentMission().observe(getViewLifecycleOwner(), mission -> {
            if (mission != null) {
                updateUI(mission);
                startMissionTimer();
            } else {
                stopMissionTimer();
            }
        });
    }

    private int calculateHpReduction(MissionTask task) {
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


    private void updateUI(SpecialMission mission) {
        if (mission == null) return;

        int bossHP = Math.max(0, mission.getBossHP());
        textViewBossHP.setText("Boss HP: " + bossHP);
        progressBarBossHP.setMax(Math.max(1, mission.getMaxBossHP()));
        progressBarBossHP.setProgress(bossHP);

        int userProgress = 0;
        if (currentUserId != null && mission.getUserTaskProgress() != null) {
            Integer progress = mission.getUserTaskProgress().get(currentUserId);
            if (progress != null) userProgress = progress;
        }
        textViewUserProgress.setText("Your Progress: " + userProgress);
        textViewAllianceProgress.setText("Alliance Progress: " + mission.getAllianceProgress());

        long timeLeft = mission.getDurationMillis() - (System.currentTimeMillis() - mission.getStartTime());
        textViewTimeLeft.setText(timeLeft > 0 ? "Preostalo dana: " + TimeUnit.MILLISECONDS.toDays(timeLeft) : "Misija je završena");

        memberProgressAdapter.updateProgress(mission.getUserTaskProgress());

        if (mission.getTasks() != null && !mission.getTasks().isEmpty()) {
            taskAdapter.setTasks(mission.getTasks());
        }
    }

    private void startMissionTimer() {
        stopMissionTimer();

        missionRunnable = new Runnable() {
            @Override
            public void run() {
                SpecialMission mission = viewModel.getCurrentMission().getValue();
                if (mission != null) {
                    long timeLeft = mission.getDurationMillis() - (System.currentTimeMillis() - mission.getStartTime());
                    textViewTimeLeft.setText(timeLeft > 0 ?
                            "Preostalo dana: " + TimeUnit.MILLISECONDS.toDays(timeLeft) :
                            "Misija je završena");

                    if (!mission.isActive() || timeLeft <= 0) {
                        viewModel.claimRewards();
                        stopMissionTimer();
                        Toast.makeText(getContext(), "Misija je završena!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    missionHandler.postDelayed(this, MISSION_CHECK_INTERVAL);
                }
            }
        };

        missionHandler.post(missionRunnable);
    }

    private void stopMissionTimer() {
        if (missionRunnable != null) {
            missionHandler.removeCallbacks(missionRunnable);
            missionRunnable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopMissionTimer();
    }
}
