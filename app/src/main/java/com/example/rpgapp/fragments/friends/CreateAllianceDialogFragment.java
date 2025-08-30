package com.example.rpgapp.fragments.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.fragment.NavHostFragment;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.UserAdapter;
import com.example.rpgapp.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class CreateAllianceDialogFragment extends DialogFragment{
    private FriendsViewModel viewModel;
    private EditText allianceNameEditText;
    private RecyclerView friendsRecyclerView;
    private UserAdapter friendsAdapter;
    private Button createButton, cancelButton;
    private Map<String, Boolean> selectedFriends = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_alliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(FriendsViewModel.class);

        allianceNameEditText = view.findViewById(R.id.editTextAllianceName);
        friendsRecyclerView = view.findViewById(R.id.recyclerViewInviteFriends);
        createButton = view.findViewById(R.id.buttonCreate);
        cancelButton = view.findViewById(R.id.buttonCancel);

        setupRecyclerView();
        observeViewModel();

        cancelButton.setOnClickListener(v -> dismiss()); // dismiss() zatvara dijalog
        createButton.setOnClickListener(v -> createAlliance());
    }

    private void setupRecyclerView() {
        friendsAdapter = new UserAdapter(userId -> {
            boolean isSelected = selectedFriends.containsKey(userId) && selectedFriends.get(userId);
            selectedFriends.put(userId, !isSelected);
            int position = findPositionByUserId(userId);
            if (position != -1) {
                friendsAdapter.notifyItemChanged(position);
            }
        });

        friendsAdapter.setSelectionMap(selectedFriends);
        friendsRecyclerView.setAdapter(friendsAdapter);
    }

    private void observeViewModel() {
        viewModel.displayedUsers.observe(getViewLifecycleOwner(), friends -> {
            if (friends != null) {
                selectedFriends.clear();
                for (User friend : friends) {
                    selectedFriends.put(friend.getUserId(), false);
                }
                friendsAdapter.setUsers(friends, null);
            }
        });

        viewModel.getAllianceCreationStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(getContext(), "Alliance created successfully!", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to create alliance. You might already be in one.", Toast.LENGTH_LONG).show();
                }
                viewModel.resetAllianceCreationStatus();
            }
        });
    }

    private int findPositionByUserId(String userId) {
        for (int i = 0; i < friendsAdapter.getItemCount(); i++) {
            if (friendsAdapter.getUserAt(i).getUserId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }

    private void createAlliance() {
        String allianceName = allianceNameEditText.getText().toString().trim();
        if (allianceName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter an alliance name", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> invitedIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedFriends.entrySet()) {
            if (entry.getValue()) {
                invitedIds.add(entry.getKey());
            }
        }

        if (invitedIds.isEmpty()) {
            Toast.makeText(getContext(), "Please invite at least one friend", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pozovi metodu iz ViewModel-a
        viewModel.createAlliance(allianceName, invitedIds);

        Toast.makeText(getContext(), "Creating alliance: " + allianceName, Toast.LENGTH_SHORT).show();
        // dismiss(); // Zatvori dijalog nakon uspešnog poziva
    }
}
