package com.example.shopapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.shopapp.databinding.FragmentSettingsBinding;
import com.example.shopapp.services.ForegroundService;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SwitchMaterial startServiceButton;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private final BroadcastReceiver serviceStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ForegroundService.ACTION_STOPPED)) {
                // Set switch to off
                startServiceButton.setChecked(false);
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentSettingsBinding.inflate(inflater, container, false);
        }
        View root = binding.getRoot();
        if (binding != null) {
            startServiceButton = binding.switch1;
            startServiceButton.setOnCheckedChangeListener((compoundButton, b) -> {
                Intent intent = new Intent(getActivity(), ForegroundService.class);
                intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
                if (b) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireActivity().startForegroundService(intent);
                    } else {
                        requireActivity().startService(intent);
                    }
                } else {
                    Intent intent1 = new Intent(getActivity(), ForegroundService.class);
                    intent.setAction(ForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
                    requireActivity().stopService(intent1);
                }
            });

            // Register receiver with NOT_EXPORTED flag
            ContextCompat.registerReceiver(
                    requireContext(),
                    serviceStoppedReceiver,
                    new IntentFilter(ForegroundService.ACTION_STOPPED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }


        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}
