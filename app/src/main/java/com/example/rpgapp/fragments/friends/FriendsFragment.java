package com.example.rpgapp.fragments.friends;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.UserAdapter;
import java.util.List; // Import List
import androidx.navigation.fragment.NavHostFragment;
import androidx.activity.result.ActivityResultLauncher;

import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.SendRequestStatus;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import android.content.pm.ActivityInfo;

public class FriendsFragment extends Fragment {
    // Promenjen TAG da bi se lakše filtriralo
    private static final String TAG = "RPGApp_Debug";
    private FriendsViewModel viewModel;
    private SearchView searchViewUsers;
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;

    public FriendsFragment() {
        Log.d(TAG, "FriendsFragment: Constructor called");
    }

    // U FriendsFragment.java

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_LONG).show();
                    return;
                }

                String scannedUserId = result.getContents();
                Log.d("FriendsFragment", "Skeniran je userId: " + scannedUserId);

                viewModel.sendFriendRequest(scannedUserId, (status, e) -> {
                    String message = "";
                    boolean shouldNavigate = false;

                    switch (status) {
                        case SUCCESS:
                            message = "Friend request sent!";
                            shouldNavigate = true;
                            break;
                        case ALREADY_FRIENDS:
                            message = "You are already friends!";
                            shouldNavigate = true;
                            break;
                        case REQUEST_ALREADY_SENT:
                            message = "Request was already sent.";
                            shouldNavigate = true;
                            break;
                        case CANNOT_ADD_SELF:
                            message = "You cannot add yourself as a friend.";
                            break;
                        case FAILURE:
                            message = "Error: " + (e != null ? e.getMessage() : "Unknown error");
                            break;
                    }

                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                    if (shouldNavigate) {
                        NavController navController = NavHostFragment.findNavController(FriendsFragment.this);

                        if (status == SendRequestStatus.SUCCESS) {
                            navController.getCurrentBackStackEntry().getSavedStateHandle().set("needs_refresh", true);
                        }

                        FriendsFragmentDirections.ActionFriendsFragmentToFriendProfileFragment action =
                                FriendsFragmentDirections.actionFriendsFragmentToFriendProfileFragment();
                        action.setUserId(scannedUserId);
                        navController.navigate(action);
                    }
                });
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "FriendsFragment: onCreate called");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "FriendsFragment: onCreateView called");
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchViewUsers = view.findViewById(R.id.searchViewUsers);
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        view.findViewById(R.id.buttonScanQr).setOnClickListener(v -> {
            startScanner();
        });

        viewModel.getCurrentUserLiveDataForDebug().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Log.d(TAG, "Korisnik je stigao u Fragment: " + user.getUsername());
            } else {
                Log.d(TAG, "Korisnik je NULL u Fragmentu.");
            }
        });

        setupRecyclerView();
        setupSearchView();
        observeViewModel();

        Log.d(TAG, "FriendsFragment: onViewCreated END");
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a friend's QR code");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        // options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(userId -> {
            Log.d("RPGApp_Debug", "Kliknuto na korisnika sa ID: " + userId);

            FriendsFragmentDirections.ActionFriendsFragmentToFriendProfileFragment action =
                    FriendsFragmentDirections.actionFriendsFragmentToFriendProfileFragment();
            action.setUserId(userId);

            NavHostFragment.findNavController(this).navigate(action);
        });

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);
    }


    private void setupSearchView() {
        Log.d(TAG, "FriendsFragment: SearchView setup complete");
        searchViewUsers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchUsers(query); // Ovu logiku ćemo doraditi
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {

                }
                return true;
            }
        });
    }

    private void observeViewModel() {
        Log.d(TAG, "FriendsFragment: Setting up observers for ViewModel LiveData");
        viewModel.getDisplayedUsers().observe(getViewLifecycleOwner(), users -> {
            Log.d(TAG, "FriendsFragment_Observer: displayedUsers LiveData triggered!");
            if (users == null) {
                Log.d(TAG, "FriendsFragment_Observer: Primljena lista je NULL.");
                return;
            }
            Log.d(TAG, "FriendsFragment_Observer: Primljeno " + users.size() + " korisnika. Prosleđujem adapteru.");
            userAdapter.setUsers(users, null);
        });

        Log.d(TAG, "FriendsFragment: Observers are set up.");
    }
}