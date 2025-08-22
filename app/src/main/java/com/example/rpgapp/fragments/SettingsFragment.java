package com.example.rpgapp.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.example.rpgapp.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private BroadcastReceiver receiver;
    private PreferenceManager prefMgr;

    private static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Konfigurisanje PreferenceManager-a
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(getString(R.string.pref_file));
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

        // Učitavanje XML resursa sa podešavanjima
        addPreferencesFromResource(R.xml.preferences);
        //summary atribut nije vise podrzan, pa koristimo setSummaryProvider da setujemo tip elementa
        // Pronađi ListPreference
        ListPreference syncPreference = findPreference(getString(R.string.pref_sync_list));
        if (syncPreference != null) {
            // Postavi SummaryProvider
            syncPreference.setSummaryProvider(preference -> {
                // Prikazuje trenutni izbor kao sažetak
                String currentValue = ((ListPreference) preference).getValue();
                int index = ((ListPreference) preference).findIndexOfValue(currentValue);
                if (index >= 0) {
                    return ((ListPreference) preference).getEntries()[index];
                } else {
                    return "No connection type selected";
                }
            });
        }
        EditTextPreference appNamePref = findPreference("pref_name");
        if (appNamePref != null) {
            // Postavi SummaryProvider za dinamički sažetak
            appNamePref.setSummaryProvider(preference -> {
                String value = ((EditTextPreference) preference).getText();
                return value == null || value.isEmpty()
                        ? "No name set for the application"
                        : "Current name: " + value;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kreiranje i registrovanje BroadcastReceiver-a
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("update-play-tone-action".equals(intent.getAction())) {
                    handlePlayToneUpdate();
                }
            }
        };

        // Registrovanje receiver-a za lokalni broadcast
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(receiver, new IntentFilter("update-play-tone-action"));
    }

    @Override
    public void onPause() {
        super.onPause();

        // Odregistrovanje receiver-a kako bi se izbegli curenja memorije
        if (receiver != null) {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(receiver);
            receiver = null;
        }
    }

    /**
     * Obrada ažuriranja "play_tone" postavke.
     */
    private void handlePlayToneUpdate() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) {
            boolean playTone = sharedPreferences.getBoolean("play_tone", false);
            Log.i("PLAY_TONE_STATUS", "Current play tone status: " + playTone);

            if (playTone) {
                // Ažuriranje vrednosti u SharedPreferences
                sharedPreferences.edit()
                        .putBoolean("play_tone", false)
                        .apply();

                // Ažuriranje UI komponente
                setPlayTonePreference(false);
            }
        } else {
            Log.e("SettingsFragment", "SharedPreferences instance is null!");
        }
    }

    /**
     * Ažurira vrednost SwitchPreference-a za "play_tone".
     *
     * @param value Nova vrednost za preferencu.
     */
    private void setPlayTonePreference(boolean value) {
        SwitchPreference playTonePref = findPreference("play_tone");
        if (playTonePref != null) {
            playTonePref.setChecked(value);
        } else {
            Log.e("SettingsFragment", "SwitchPreference 'play_tone' not found!");
        }
    }
}
