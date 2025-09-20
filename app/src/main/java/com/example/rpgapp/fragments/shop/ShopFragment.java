package com.example.rpgapp.fragments.shop;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.ShopAdapter;
import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.User;

public class ShopFragment extends Fragment {

    private ShopViewModel viewModel;
    private RecyclerView recyclerView;
    private ShopAdapter adapter;
    private TextView textViewUserCoins;
    private SpecialMissionViewModel specialMissionViewModel;
    private SpecialMission activeMission;
    private String currentUserId;
    private boolean shopTaskCompleted = false; // flag da ne rešava dvaput




    public ShopFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User user = UserRepository.getInstance(requireContext()).getLoggedInUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Niste ulogovani.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUserId = user.getUserId();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewShop);
        viewModel = new ViewModelProvider(this).get(ShopViewModel.class);
        textViewUserCoins = view.findViewById(R.id.textViewUserCoins);

        specialMissionViewModel = new ViewModelProvider(requireActivity())
                .get(SpecialMissionViewModel.class);

        specialMissionViewModel.getCurrentMission().observe(getViewLifecycleOwner(), mission -> {
            activeMission = mission;
        });

        SpecialMissionRepository.getInstance(requireContext())
                .getActiveMissionForUser(currentUserId, new SpecialMissionRepository.MissionCallback() {
                    @Override
                    public void onMissionLoaded(SpecialMission mission) {
                        if (isAdded()) {
                            specialMissionViewModel.setCurrentMission(mission);
                            activeMission = mission;
                            requireActivity().runOnUiThread(() -> {
                                if (mission != null) {
                                    Toast.makeText(requireContext(),
                                            "Kupovinom u shopu rešavate zadatak iz specijalne misije!",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Greška prilikom učitavanja misije: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                });




        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new ShopAdapter(item -> {
            viewModel.purchaseItem(item);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getScreenState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                textViewUserCoins.setText(String.valueOf(state.user.getCoins()));
                adapter.updateData(state);

                Log.d("ShopFragment", "Stanje ekrana je osveženo. Korisnik: " + state.user.getUsername());
            }



        });

        viewModel.getPurchaseStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
            }



            if ("Purchase successful!".equals(status)) {
                shopTaskCompleted = true;
                if (specialMissionViewModel.getCurrentMission().getValue() != null) {
                    SpecialMission activeMission = specialMissionViewModel.getCurrentMission().getValue();
                    for (int i = 0; i < activeMission.getTasks().size(); i++) {
                        MissionTask task = activeMission.getTasks().get(i);
                        if ("Kupovina u prodavnici".equals(task.getName())) {
                            specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                            break;
                        }
                    }
                }
            }
        });
    }
}