package com.example.rpgapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.rpgapp.fragments.alliance.AllianceChatFragment;
import com.example.rpgapp.fragments.alliance.AllianceInfoFragment;

public class AlliancePagerAdapter extends FragmentStateAdapter {
    public AlliancePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AllianceInfoFragment(); // Prvi tab je sada Info/Members
            case 1:
                return new AllianceChatFragment(); // Drugi tab je Chat
            default:
                return new AllianceInfoFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Sada imamo samo 2 taba
    }
}