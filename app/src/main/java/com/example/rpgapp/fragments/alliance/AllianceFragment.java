package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.AlliancePagerAdapter;

public class AllianceFragment extends Fragment {

    private AllianceViewModel viewModel;
    private ViewPager2 viewPager;
    private AlliancePagerAdapter pagerAdapter;

    // Elementi za "Niste u alijansi" stanje
    private LinearLayout noAllianceLayout;
    private Button buttonCreateAlliance, buttonViewInvites;

    // Elementi za "U alijansi ste" stanje
    private LinearLayout allianceContentLayout;
    private TextView textViewAllianceName, textViewAllianceMembers;
    private Button buttonTabMembers, buttonTabChat, buttonTabQuests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AllianceViewModel.class);

        bindViews(view);
        setupViewPager();
        setupTabButtons();
        observeViewModel();
    }

    private void bindViews(View view) {
        // Povezivanje elemenata za oba stanja
        noAllianceLayout = view.findViewById(R.id.layout_no_alliance);
        buttonCreateAlliance = view.findViewById(R.id.buttonCreateAlliance);
        buttonViewInvites = view.findViewById(R.id.buttonViewInvites);

        allianceContentLayout = view.findViewById(R.id.layout_alliance_content);
        viewPager = view.findViewById(R.id.viewPager);
        textViewAllianceName = view.findViewById(R.id.textViewAllianceName);
        textViewAllianceMembers = view.findViewById(R.id.textViewAllianceMembers);
        buttonTabMembers = view.findViewById(R.id.buttonTabMembers);
        buttonTabChat = view.findViewById(R.id.buttonTabChat);
        buttonTabQuests = view.findViewById(R.id.buttonTabQuests);
    }

    private void setupViewPager() {
        pagerAdapter = new AlliancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Listener koji prati promenu stranice na ViewPager-u
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabStyles(position); // Ažuriraj stil dugmića kada se stranica promeni
            }
        });
    }

    private void setupTabButtons() {
        buttonTabMembers.setOnClickListener(v -> viewPager.setCurrentItem(0));
        buttonTabChat.setOnClickListener(v -> viewPager.setCurrentItem(1));
        buttonTabQuests.setOnClickListener(v -> viewPager.setCurrentItem(2));
    }

    private void observeViewModel() {
        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                // Korisnik je u alijansi
                allianceContentLayout.setVisibility(View.VISIBLE);
                noAllianceLayout.setVisibility(View.GONE);

                // Popuni podatke o alijansi
                textViewAllianceName.setText(alliance.getName());
                textViewAllianceMembers.setText(alliance.getMemberCount() + " members");

                // Postavi inicijalno stanje tabova
                updateTabStyles(viewPager.getCurrentItem());

            } else {
                // Korisnik nije u alijansi
                allianceContentLayout.setVisibility(View.GONE);
                noAllianceLayout.setVisibility(View.VISIBLE);

                // TODO: Postavi listenere za dugmad "Create Alliance" i "View Invites"
                // buttonCreateAlliance.setOnClickListener(...)
                // buttonViewInvites.setOnClickListener(...)
            }
        });
    }

    /**
     * Pomoćna metoda koja menja stilove dugmića-tabova na osnovu
     * trenutno selektovane stranice.
     * @param selectedPosition Pozicija trenutne stranice (0, 1, ili 2)
     */
    private void updateTabStyles(int selectedPosition) {
        // Prvo, postavi SVA dugmad na "outline" (neselektovani) stil
        buttonTabMembers.setBackgroundResource(R.drawable.panel_border028);
        buttonTabChat.setBackgroundResource(R.drawable.panel_border028);
        buttonTabQuests.setBackgroundResource(R.drawable.panel_border028);

        // Zatim, postavi "filled" (selektovani) stil samo na izabrano dugme
        switch (selectedPosition) {
            case 0: // Members
                buttonTabMembers.setBackgroundResource(R.drawable.panel_transparent_border028);
                break;
            case 1: // Chat
                buttonTabChat.setBackgroundResource(R.drawable.panel_transparent_border028);
                break;
            case 2: // Quests
                buttonTabQuests.setBackgroundResource(R.drawable.panel_transparent_border028);
                break;
        }
    }
}