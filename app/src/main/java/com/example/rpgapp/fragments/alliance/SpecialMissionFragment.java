package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.os.Handler;
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
import com.example.rpgapp.fragments.alliance.AllianceViewModel;
import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SpecialMissionFragment extends Fragment {

    private SpecialMissionViewModel viewModel;
    private UserRepository userRepository;
    private String currentUserId;
    private User currentUser;

    private TextView textViewBossHP, textViewUserProgress, textViewAllianceProgress, textViewTimeLeft, textViewMembersTitle;
    private ProgressBar progressBarBossHP;
    private RecyclerView recyclerViewTasks, recyclerViewMembersProgress;
    private MissionTaskAdapter taskAdapter;
    private MemberProgressAdapter memberProgressAdapter;
    private Button btnStartMission;

    private Handler missionHandler = new Handler();
    private Runnable missionRunnable;
    private AllianceViewModel allianceViewModel;

    private static final long MISSION_CHECK_INTERVAL = 1000 * 60;

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
        setupInitialUI();
        observeUserAndAlliance();
    }

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

    private void setupInitialUI() {
        textViewBossHP.setText("Učitavanje misije...");
        textViewBossHP.setVisibility(View.VISIBLE);
        btnStartMission.setVisibility(View.GONE);
        hideMissionUI();
    }

    private void setupRepositoriesAndViewModels() {
        userRepository = UserRepository.getInstance(requireContext());
        currentUserId = userRepository.getLoggedInUser() != null
                ? userRepository.getLoggedInUser().getUserId()
                : null;

        viewModel = new ViewModelProvider(requireActivity()).get(SpecialMissionViewModel.class);
        allianceViewModel = new ViewModelProvider(requireActivity()).get(AllianceViewModel.class);
    }

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
                    int hpReduction = viewModel.calculateHpReduction(task);
                    viewModel.completeTask(position, hpReduction, 1, currentUserId);
                    memberProgressAdapter.updateProgress(mission.getUserTaskProgress());
                }
            }
        }, currentUserId);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    private void observeUserAndAlliance() {
        allianceViewModel.getLoggedInUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            allianceViewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
                if (currentUser == null || alliance == null) return;

                FirebaseFirestore.getInstance()
                        .collection("alliances")
                        .document(alliance.getAllianceId())
                        .addSnapshotListener((snapshot, e) -> {
                            if (e != null || snapshot == null) return;
                            Boolean missionStarted = snapshot.getBoolean("missionStarted");
                            allianceViewModel.setMissionStarted(missionStarted != null && missionStarted);
                        });

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

    private void setupMissionObserver(Button btnStartMission, boolean isLeader) {
        viewModel.getCurrentMission().observe(getViewLifecycleOwner(), mission -> {
            if (mission == null) {
                // Nema aktivne misije
                hideMissionUI();
                textViewBossHP.setVisibility(View.VISIBLE);
                textViewBossHP.setText("Nema aktivne misije");

                // Prikazi dugme samo ako je lider
                btnStartMission.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            } else if (mission.isActive()) {
                btnStartMission.setVisibility(View.GONE);
                showMissionUI(mission);
                startMissionTimer();
            } else {
                hideMissionUI();
                textViewBossHP.setVisibility(View.VISIBLE);
                textViewBossHP.setText("Nema aktivne misije");
                btnStartMission.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            }
        });
    }


    private void setupStartMissionButton(Button btnStartMission, Alliance alliance, boolean isLeader) {
        btnStartMission.setOnClickListener(v -> {
            if (!isLeader) return;

            SpecialMission mission = viewModel.getCurrentMission().getValue();
            if (mission == null || !mission.isActive()) {
                // Kreiraj novu misiju
                viewModel.startSpecialMission(alliance);
                allianceViewModel.setMissionStarted(true); // odmah obeleži da je misija startovana
            } else {
                Toast.makeText(getContext(), "Savez već ima aktivnu misiju!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMissionUI(SpecialMission mission) {
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
        textViewBossHP.setVisibility(View.GONE);
        textViewUserProgress.setVisibility(View.GONE);
        textViewAllianceProgress.setVisibility(View.GONE);
        progressBarBossHP.setVisibility(View.GONE);
        textViewTimeLeft.setVisibility(View.GONE);
        recyclerViewTasks.setVisibility(View.GONE);
        recyclerViewMembersProgress.setVisibility(View.GONE);
        textViewMembersTitle.setVisibility(View.GONE);
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
                    textViewTimeLeft.setText(timeLeft > 0 ? "Preostalo dana: " + TimeUnit.MILLISECONDS.toDays(timeLeft) : "Misija je završena");

                    if (!mission.isActive() || timeLeft <= 0) {
                        viewModel.claimRewards();
                        allianceViewModel.setMissionStarted(false);
                        viewModel.forceEndMission(mission.getAllianceId());
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
