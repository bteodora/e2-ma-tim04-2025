package com.example.rpgapp.fragments.friends;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.UserAdapter;
import java.util.List; // Import List
import androidx.navigation.fragment.NavHostFragment;

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
        Log.d(TAG, "FriendsFragment: onViewCreated START");

        searchViewUsers = view.findViewById(R.id.searchViewUsers);
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        Log.d(TAG, "FriendsFragment: Views initialized");

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);
        Log.d(TAG, "FriendsFragment: ViewModel initialized");

        viewModel.getCurrentUserLiveDataForDebug().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Log.d(TAG, "!!! TEST OBSERVER: Korisnik je konačno stigao u Fragment: " + user.getUsername());
            } else {
                Log.d(TAG, "!!! TEST OBSERVER: Korisnik je NULL u Fragmentu.");
            }
        });

        setupRecyclerView();
        setupSearchView();
        observeViewModel();

        Log.d(TAG, "FriendsFragment: onViewCreated END");
    }

    private void setupRecyclerView() {
        // Kreiramo adapter i prosleđujemo mu implementaciju listener-a
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
            userAdapter.setUsers(users);
        });

        // ... observer za greške ostaje isti ...
        Log.d(TAG, "FriendsFragment: Observers are set up.");
    }
}