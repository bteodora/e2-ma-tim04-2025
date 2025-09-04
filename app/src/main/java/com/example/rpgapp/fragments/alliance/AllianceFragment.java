package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.AlliancePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AllianceFragment extends Fragment {
    private AllianceViewModel viewModel;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AlliancePagerAdapter pagerAdapter;
    private TextView notInAllianceText;
    private LinearLayout allianceContentLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicijalizuj ViewModel u glavnom fragmentu
        viewModel = new ViewModelProvider(this).get(AllianceViewModel.class);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        notInAllianceText = view.findViewById(R.id.textViewNotInAlliance);
        allianceContentLayout = view.findViewById(R.id.layout_alliance_content);

        pagerAdapter = new AlliancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Overview");
                    break;
                case 1:
                    tab.setText("Chat");
                    break;
                    // TODO dodati i za misiju
            }
        }).attach();

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                // Korisnik je u savezu, prikaži tabove
                allianceContentLayout.setVisibility(View.VISIBLE);
                notInAllianceText.setVisibility(View.GONE);
            } else {
                // Korisnik nije u savezu, prikaži poruku
                allianceContentLayout.setVisibility(View.GONE);
                notInAllianceText.setVisibility(View.VISIBLE);
            }
        });
    }
}