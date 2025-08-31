package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.rpgapp.R;

public class AllianceFragment extends Fragment {

    private AllianceViewModel viewModel;
    private TextView textViewAllianceName;
    private TextView textViewLeaderName;
    // ... ostali UI elementi ...

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AllianceViewModel.class);

        textViewAllianceName = view.findViewById(R.id.textViewAllianceName);
        textViewLeaderName = view.findViewById(R.id.textViewLeaderName);
        // ... poveži ostale view-ove ...

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                // Imamo podatke, popuni UI
                textViewAllianceName.setText(alliance.getName());
                textViewLeaderName.setText(alliance.getLeaderUsername());
                // TODO: Popuni RecyclerView sa članovima
            } else {
                // Korisnik nije u savezu, prikaži poruku
                textViewAllianceName.setText("You are not in an alliance.");
                // TODO: Sakrij ostatak UI-ja
            }
        });
    }
}