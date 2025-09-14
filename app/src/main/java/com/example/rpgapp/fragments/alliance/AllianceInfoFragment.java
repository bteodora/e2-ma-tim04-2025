package com.example.rpgapp.fragments.alliance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.UserAdapter;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.User;

public class AllianceInfoFragment extends Fragment {

    private AllianceViewModel viewModel;
    private TextView allianceName, leaderName;
    private RecyclerView recyclerViewMembers;
    private UserAdapter membersAdapter;
    private Button buttonDisband, buttonLeave;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allianceName = view.findViewById(R.id.textViewAllianceName);
        leaderName = view.findViewById(R.id.textViewLeaderName);
        recyclerViewMembers = view.findViewById(R.id.recyclerViewMembers);
        buttonDisband = view.findViewById(R.id.buttonDisbandAlliance);
        buttonLeave = view.findViewById(R.id.buttonLeaveAlliance);

        // ✅ Deljeni ViewModel sa parent fragmentom ili Activity
        viewModel = new ViewModelProvider(requireActivity()).get(AllianceViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        membersAdapter = new UserAdapter(userId -> {});
        recyclerViewMembers.setAdapter(membersAdapter);
    }

    private void setupClickListeners() {
        buttonDisband.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Disband Alliance")
                .setMessage("Are you sure you want to disband this alliance? This action cannot be undone.")
                .setPositiveButton("Disband", (dialog, which) -> viewModel.disbandAlliance())
                .setNegativeButton("Cancel", null)
                .show()
        );

        buttonLeave.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Leave Alliance")
                .setMessage("Are you sure you want to leave this alliance?")
                .setPositiveButton("Leave", (dialog, which) -> viewModel.leaveAlliance())
                .setNegativeButton("Cancel", null)
                .show()
        );
    }

    private void observeViewModel() {
        viewModel.getLoggedInUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            updateButtonVisibility();
        });

        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                leaderName.setText(alliance.getLeaderUsername());
                updateButtonVisibility();
            }
        });

        viewModel.members.observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                membersAdapter.setUsers(members, null);
            }
        });

        viewModel.getActionStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Toast.makeText(getContext(), success ? "Action successful." : "Action failed.", Toast.LENGTH_SHORT).show();
                viewModel.resetActionStatus();
            }
        });
    }

    private void updateButtonVisibility() {
        Alliance alliance = viewModel.getCurrentAlliance().getValue();

        if (currentUser == null || alliance == null) {
            buttonDisband.setVisibility(View.GONE);
            buttonLeave.setVisibility(View.GONE);
            return;
        }

        boolean isLeader = currentUser.getUserId().equals(alliance.getLeaderId());
        boolean isMissionStarted = alliance.isMissionStarted();

        buttonDisband.setVisibility(isLeader && !isMissionStarted ? View.VISIBLE : View.GONE);
        buttonLeave.setVisibility(!isLeader && !isMissionStarted ? View.VISIBLE : View.GONE);
    }
}
