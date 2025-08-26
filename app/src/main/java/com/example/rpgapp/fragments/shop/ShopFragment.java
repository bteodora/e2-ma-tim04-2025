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

public class ShopFragment extends Fragment {

    private ShopViewModel viewModel;
    private RecyclerView recyclerView;
    private ShopAdapter adapter;
    private TextView textViewUserCoins;


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
        textViewUserCoins = view.findViewById(R.id.textViewUserCoins);

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
        });
    }
}