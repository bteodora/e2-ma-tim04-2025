package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.rpgapp.model.Alliance;
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
    private User currentUser;
    private TextView textViewBossHP, textViewUserProgress, textViewAllianceProgress, textViewTimeLeft;
    private ProgressBar progressBarBossHP;
    private RecyclerView recyclerViewTasks;
    private MissionTaskAdapter taskAdapter;

    private RecyclerView recyclerViewMembersProgress;
    private MemberProgressAdapter memberProgressAdapter;
    private TextView textViewMembersTitle;


    private Handler missionHandler = new Handler();
    private Runnable missionRunnable;
    private Button btnStartMission;


    private static final long MISSION_CHECK_INTERVAL = 1000 * 60; // proverava svake minute

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_special_missions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRepositoriesAndViewModels();
        setupAdapters();
        observeUserAndAlliance();
    }

    /** Inicijalizacija svih UI elemenata */
    private void bindViews(View view) {
        textViewBossHP = view.findViewById(R.id.textViewBossHP);
        textViewUserProgress = view.findViewById(R.id.textViewUserProgress);
        textViewAllianceProgress = view.findViewById(R.id.textViewAllianceProgress);
        progressBarBossHP = view.findViewById(R.id.progressBarBossHP);
        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks);
        textViewTimeLeft = view.findViewById(R.id.textViewTimeLeft);
        recyclerViewMembersProgress = view.findViewById(R.id.recyclerViewMembersProgress);
        btnStartMission = view.findViewById(R.id.btnStartMission);
        textViewMembersTitle = view.findViewById(R.id.textViewMembersTitle);

    }

    /** Inicijalizacija repozitorijuma i ViewModel-a */
    private void setupRepositoriesAndViewModels() {
        userRepository = UserRepository.getInstance(requireContext());
        currentUserId = userRepository.getLoggedInUser() != null
                ? userRepository.getLoggedInUser().getUserId()
                : null;

        viewModel = new ViewModelProvider(requireActivity()).get(SpecialMissionViewModel.class);
    }

    /** Postavljanje adaptera za zadatke i članove saveza */
    private void setupAdapters() {
        memberProgressAdapter = new MemberProgressAdapter();
        recyclerViewMembersProgress.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMembersProgress.setAdapter(memberProgressAdapter);

        taskAdapter = new MissionTaskAdapter(position -> {
            SpecialMission mission = viewModel.getCurrentMission().getValue();
            if (mission != null && currentUserId != null && mission.getTasks() != null
                    && position < mission.getTasks().size()) {
                MissionTask task = mission.getTasks().get(position);
                if (task.incrementProgress(currentUserId)) {
                    int hpReduction = calculateHpReduction(task);
                    viewModel.completeTask(position, hpReduction, 1, currentUserId);
                    memberProgressAdapter.updateProgress(mission.getUserTaskProgress());
                }
            }
        }, currentUserId);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    /** Observeri za korisnika, savez i misije */
    private void observeUserAndAlliance() {
        AllianceViewModel allianceViewModel = new ViewModelProvider(requireActivity()).get(AllianceViewModel.class);

        allianceViewModel.getLoggedInUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;

            allianceViewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
                if (currentUser == null || alliance == null) return;

                boolean isLeader = currentUser.getUserId().equals(alliance.getLeaderId());

                setupMissionObserver(btnStartMission, isLeader);
                setupStartMissionButton(btnStartMission, alliance, isLeader);
            });

            allianceViewModel.members.observe(getViewLifecycleOwner(), members -> {
                SpecialMission mission = viewModel.getCurrentMission().getValue();
                Map<String, Integer> progressMap = mission != null ? mission.getUserTaskProgress() : new HashMap<>();
                memberProgressAdapter.setMembersAndProgress(members, progressMap);
            });

            allianceViewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
                if (alliance != null) viewModel.loadMission(alliance.getAllianceId());
            });
        });
    }

    /** Observer za trenutnu misiju */
    private void setupMissionObserver(Button btnStartMission, boolean isLeader) {
        viewModel.getCurrentMission().observe(getViewLifecycleOwner(), mission -> {
            if (mission != null && mission.isActive()) {
                btnStartMission.setVisibility(View.GONE);
                showMissionUI(mission);
                startMissionTimer();
            } else if (isLeader) {
                btnStartMission.setVisibility(View.VISIBLE);
                hideMissionUI();
            } else {
                btnStartMission.setVisibility(View.GONE);
                hideMissionUI();
                textViewBossHP.setText("Nema aktivne misije");
            }
        });
    }

    /** Klik dugme StartMission */
    private void setupStartMissionButton(Button btnStartMission, Alliance alliance, boolean isLeader) {
        btnStartMission.setOnClickListener(v -> {
            if (!isLeader) return;

            viewModel.hasActiveMission(alliance.getAllianceId())
                    .observe(getViewLifecycleOwner(), hasActive -> {
                        if (Boolean.FALSE.equals(hasActive)) {
                            viewModel.startMission(alliance);
                            btnStartMission.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Počela je specijalna misija!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Savez već ima aktivnu misiju!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void showMissionUI(SpecialMission mission) {
        // Pokaži sve UI elemente misije
        textViewBossHP.setVisibility(View.VISIBLE);
        textViewUserProgress.setVisibility(View.VISIBLE);
        textViewAllianceProgress.setVisibility(View.VISIBLE);
        progressBarBossHP.setVisibility(View.VISIBLE);
        textViewTimeLeft.setVisibility(View.VISIBLE);
        recyclerViewTasks.setVisibility(View.VISIBLE);
        recyclerViewMembersProgress.setVisibility(View.VISIBLE);
        textViewMembersTitle.setVisibility(View.VISIBLE);

        updateUI(mission);
    }

    private void hideMissionUI() {
        // Sakrij sve UI elemente misije
        textViewBossHP.setVisibility(View.GONE);
        textViewUserProgress.setVisibility(View.GONE);
        textViewAllianceProgress.setVisibility(View.GONE);
        progressBarBossHP.setVisibility(View.GONE);
        textViewTimeLeft.setVisibility(View.GONE);
        recyclerViewTasks.setVisibility(View.GONE);
        recyclerViewMembersProgress.setVisibility(View.GONE);
        textViewMembersTitle.setVisibility(View.GONE);
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