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
import android.widget.Button;
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
    private static final String TAG = "RPGApp_Debug";
    private FriendsViewModel viewModel;
    private SearchView searchViewUsers;
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;

    public FriendsFragment() {
        Log.d(TAG, "FriendsFragment: Constructor called");
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_LONG).show();
                    return;
                }

                String scannedUserId = result.getContents();
                Log.d("FriendsFragment", "Skeniran je userId: " + scannedUserId);

                FriendsFragmentDirections.ActionFriendsFragmentToFriendProfileFragment action =
                        FriendsFragmentDirections.actionFriendsFragmentToFriendProfileFragment();
                action.setUserId(scannedUserId);

                action.setAutoSendRequest(true);

                NavHostFragment.findNavController(FriendsFragment.this).navigate(action);
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
        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        searchViewUsers = view.findViewById(R.id.searchViewUsers);
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        view.findViewById(R.id.buttonScanQr).setOnClickListener(v -> {
            startScanner();
        });

        Button buttonViewRequests = view.findViewById(R.id.buttonViewRequests);
        buttonViewRequests.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_view_requests);
        });

        Button createAllianceButton = view.findViewById(R.id.buttonCreateAlliance);
        createAllianceButton.setOnClickListener(v -> {
            CreateAllianceDialogFragment dialog = new CreateAllianceDialogFragment();
            dialog.show(getParentFragmentManager(), "CreateAllianceDialog");
        });

        setupRecyclerView();
        setupSearchView();
        observeViewModel();
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a friend's QR code");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
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
                viewModel.searchUsers(query);
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