package com.example.rpgapp.fragments.requests;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment; // <-- VAŽAN IMPORT
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // <-- VAŽAN IMPORT

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.UserAdapter;
import com.example.rpgapp.model.User;

public class RequestsFragment extends Fragment {

    private RequestsViewModel viewModel;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewRequests);
        viewModel = new ViewModelProvider(this).get(RequestsViewModel.class);

        setupRecyclerView();
        observeViewModel();

        viewModel.loadRequests();
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(userId -> {
            Log.d("RequestsFragment", "Navigating to profile of user: " + userId);

            RequestsFragmentDirections.ActionRequestsFragmentToFriendProfileFragment action =
                    RequestsFragmentDirections.actionRequestsFragmentToFriendProfileFragment();

            action.setUserId(userId);

            NavHostFragment.findNavController(this).navigate(action);
        });

        userAdapter.setOnActionButtonClickListener(user -> {
            showRespondDialog(user);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(userAdapter);
    }

    private void observeViewModel() {
        viewModel.getRequestingUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                userAdapter.setUsers(users, "Respond");
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRespondDialog(User user) {
        new AlertDialog.Builder(getContext())
                .setTitle("Friend Request")
                .setMessage("Accept friend request from " + user.getUsername() + "?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    viewModel.acceptRequest(user.getUserId());
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    viewModel.declineRequest(user.getUserId());
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}