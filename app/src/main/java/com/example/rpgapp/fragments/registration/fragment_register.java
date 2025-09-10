package com.example.rpgapp.fragments.registration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.rpgapp.R;

import java.util.Arrays;
import java.util.List;

public class fragment_register extends Fragment {

    private EditText editTextUsername, editTextEmail, editTextPassword, editTextPasswordConfirm;
    private ImageView imageViewAvatar;
    private ImageButton buttonAvatarLeft, buttonAvatarRight;
    private Button buttonRegister;
    private TextView goToLogin;

    private AuthViewModel authViewModel;
    private String selectedAvatarId = "default_avatar";
    private int currentAvatarIndex = 0;

    private final List<String> avatarIds = Arrays.asList(
            "avatar1",
            "avatar2",
            "avatar3",
            "avatar4",
            "avatar5"
    );

    public fragment_register() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        bindViews(view);
        setupListeners();
        observeViewModel();
        updateAvatarDisplay();
    }

    private void bindViews(View view) {
        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextPasswordConfirm = view.findViewById(R.id.editTextPasswordConfirm);
        imageViewAvatar = view.findViewById(R.id.imageViewAvatar);
        buttonAvatarLeft = view.findViewById(R.id.buttonAvatarLeft);
        buttonAvatarRight = view.findViewById(R.id.buttonAvatarRight);
        buttonRegister = view.findViewById(R.id.buttonRegister);
        goToLogin = view.findViewById(R.id.textViewGoToLogin);
    }

    private void setupListeners() {
        buttonRegister.setOnClickListener(v -> handleRegistration());
        goToLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(fragment_register.this)
                        .navigate(R.id.action_registerFragment_to_loginFragment)
        );

        buttonAvatarRight.setOnClickListener(v -> {
            currentAvatarIndex = (currentAvatarIndex + 1) % avatarIds.size();
            updateAvatarDisplay();
        });

        buttonAvatarLeft.setOnClickListener(v -> {
            currentAvatarIndex--;
            if (currentAvatarIndex < 0) {
                currentAvatarIndex = avatarIds.size() - 1;
            }
            updateAvatarDisplay();
        });
    }

    private void updateAvatarDisplay() {
        selectedAvatarId = avatarIds.get(currentAvatarIndex);
        int resourceId = getResources().getIdentifier(selectedAvatarId, "drawable", requireContext().getPackageName());
        Glide.with(this)
                .asGif()
                .load(resourceId)
                .into(imageViewAvatar);
    }

    private void observeViewModel() {
        authViewModel.getRegistrationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Registration successful! Please check your email for verification.", Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(fragment_register.this)
                        .navigate(R.id.action_registerFragment_to_loginFragment);
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRegistration() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordConfirm = editTextPasswordConfirm.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters long!");
            return;
        }

        if (!password.equals(passwordConfirm)) {
            editTextPasswordConfirm.setError("Passwords do not match!");
            return;
        }

        authViewModel.register(email, password, username, selectedAvatarId);
    }
}
