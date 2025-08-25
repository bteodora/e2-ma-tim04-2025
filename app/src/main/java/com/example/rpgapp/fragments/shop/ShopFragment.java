package com.example.rpgapp.fragments.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.ShopAdapter;

public class ShopFragment extends Fragment {

    private ShopViewModel viewModel;
    private RecyclerView recyclerView;
    private ShopAdapter adapter;

    public ShopFragment() {}

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
        // Posmatramo listu predmeta iz prodavnice
        viewModel.getShopItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });

        // Posmatramo trenutnog korisnika da bismo znali njegov inventar
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                adapter.setCurrentUser(user);
            }
        });

        // Posmatramo status kupovine (poruke o uspehu/grešci)
        viewModel.getPurchaseStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
            }
        });
    }
}