package com.example.rpgapp.fragments.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rpgapp.R;
import com.example.rpgapp.fragments.registration.AuthViewModel;

public class ChangePasswordFragment extends Fragment {

    private EditText editTextOldPassword;
    private EditText editTextNewPassword;
    private EditText editTextNewPasswordConfirm;
    private Button buttonConfirmChange;

    private AuthViewModel authViewModel;

    public ChangePasswordFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        editTextOldPassword = view.findViewById(R.id.editTextOldPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextNewPasswordConfirm = view.findViewById(R.id.editTextNewPasswordConfirm);
        buttonConfirmChange = view.findViewById(R.id.buttonConfirmChange);

        observeViewModel();

        buttonConfirmChange.setOnClickListener(v -> {
            handleChangePassword();
        });
    }

    private void observeViewModel() {
        authViewModel.getPasswordChangedSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                buttonConfirmChange.setEnabled(true);
            }
        });
    }

    private void handleChangePassword() {
        String oldPassword = editTextOldPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String newPasswordConfirm = editTextNewPasswordConfirm.getText().toString().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || newPasswordConfirm.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "New password must be at least 6 characters long!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            Toast.makeText(getContext(), "New passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonConfirmChange.setEnabled(false);

        authViewModel.changePassword(oldPassword, newPassword);
    }
}