package com.example.rpgapp.fragments.profile;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.rpgapp.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpgapp.R;
import com.example.rpgapp.model.User;

import java.text.NumberFormat;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;

    private ImageView imageViewProfileAvatar;
    private TextView textViewProfileUsername, textViewProfileTitle, textViewProfileLevel, textViewProfileXp, textViewProfileCoins, textViewProfilePP, textViewBadgesCount;
    private LinearLayout layout_coins, layout_power_points, layout_inventory_section;
    private Button buttonViewStatistics;
    private ImageView imageViewQrCode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        bindViews(view);

        String userId = null;

        viewModel.loadUserProfile(userId);

        observeViewModel();
    }

    private void bindViews(View view) {
        imageViewProfileAvatar = view.findViewById(R.id.imageViewProfileAvatar);
        textViewProfileUsername = view.findViewById(R.id.textViewProfileUsername);
        textViewProfileTitle = view.findViewById(R.id.textViewProfileTitle);
        textViewProfileLevel = view.findViewById(R.id.textViewProfileLevel);
        textViewProfileXp = view.findViewById(R.id.textViewProfileXp);
        textViewProfileCoins = view.findViewById(R.id.textViewProfileCoins);
        textViewProfilePP = view.findViewById(R.id.textViewProfilePP);
        textViewBadgesCount = view.findViewById(R.id.textViewBadgesCount);
        layout_coins = view.findViewById(R.id.layout_coins);
        layout_power_points = view.findViewById(R.id.layout_power_points);
        layout_inventory_section = view.findViewById(R.id.layout_inventory_section);
        buttonViewStatistics = view.findViewById(R.id.buttonViewStatistics);
        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);
    }

    private void observeViewModel() {
        viewModel.getDisplayedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                populateUI(user);
            }
        });

        viewModel.getIsMyProfile().observe(getViewLifecycleOwner(), isMyProfile -> {
            if (isMyProfile != null) {
                int visibility = isMyProfile ? View.VISIBLE : View.GONE;

                buttonViewStatistics.setVisibility(visibility);
                layout_coins.setVisibility(visibility);
                layout_power_points.setVisibility(visibility);
                layout_inventory_section.setVisibility(visibility);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(User user) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        textViewProfileUsername.setText(user.getUsername());
        textViewProfileTitle.setText(user.getTitle());
        textViewProfileLevel.setText(String.valueOf(user.getLevel()));
        textViewProfileXp.setText(numberFormat.format(user.getXp()));
        textViewProfileCoins.setText(numberFormat.format(user.getCoins()));
        textViewProfilePP.setText(String.valueOf(user.getPowerPoints()));

        int badgeCount = (user.getBadges() != null) ? user.getBadges().size() : 0;
        textViewBadgesCount.setText("(" + badgeCount + ")");

        // TODO: Učitati pravu sliku avatara na osnovu user.getAvatarId()
        // TODO: Dinamički popuniti layout_badges_container sa sličicama bedževa
        // TODO: Dinamički popuniti layout_equipment_container i grid_inventory_container
        // TODO: Generisati i prikazati QR kod na osnovu user.getUserId()
    }
}