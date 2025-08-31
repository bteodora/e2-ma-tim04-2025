package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.UserAdapter;

public class AllianceInfoFragment extends Fragment {
    private AllianceViewModel viewModel;
    private TextView allianceName, leaderName;
    private RecyclerView recyclerViewMembers;
    private UserAdapter membersAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(AllianceViewModel.class);

        allianceName = view.findViewById(R.id.textViewAllianceName);
        leaderName = view.findViewById(R.id.textViewLeaderName);
        recyclerViewMembers = view.findViewById(R.id.recyclerViewMembers);

        setupRecyclerView();

        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                allianceName.setText(alliance.getName());
                leaderName.setText("Leader: " + alliance.getLeaderUsername());
                // TODO: Logika za prikaz dugmića
            }
        });

        // TODO: Trebaće nam novi LiveData u ViewModel-u koji vraća listu profila članova
        // viewModel.getMembers().observe(getViewLifecycleOwner(), members -> {
        //     membersAdapter.setUsers(members, null);
        // });
        observeViewModel();
    }

    private void setupRecyclerView() {
        membersAdapter = new UserAdapter(userId -> {
        });
        recyclerViewMembers.setAdapter(membersAdapter);
    }
    private void observeViewModel() {
        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                allianceName.setText(alliance.getName());
                leaderName.setText("Leader: " + alliance.getLeaderUsername());
            }
        });

        viewModel.members.observe(getViewLifecycleOwner(), memberList -> {
            if (memberList != null) {
                membersAdapter.setUsers(memberList, null);
            }
        });
    }
}