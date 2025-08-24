// U LoginFragment.java
package com.example.rpgapp.fragments.login; // Proveri da li je putanja tačna

import android.content.Intent;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rpgapp.R;
import com.example.rpgapp.activities.HomeActivity;
import com.example.rpgapp.fragments.registration.AuthViewModel;

public class LoginFragment extends Fragment {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;

    private AuthViewModel authViewModel;

    public LoginFragment() {    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        editTextEmail = view.findViewById(R.id.editTextLoginEmail);
        editTextPassword = view.findViewById(R.id.editTextLoginPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);

        observeViewModel();

        buttonLogin.setOnClickListener(v -> {
            handleLogin();
        });
    }

    // U LoginFragment.java

    private void observeViewModel() {
        authViewModel.getLoggedInUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Toast.makeText(getContext(), "Welcome back!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), HomeActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            buttonLogin.setEnabled(true);

            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getUserDeletedAndShouldRegisterAgain().observe(getViewLifecycleOwner(), shouldRegister -> {
            buttonLogin.setEnabled(true);

            if (shouldRegister != null && shouldRegister) {
                NavController navController = NavHostFragment.findNavController(LoginFragment.this);
                navController.navigate(R.id.action_loginFragment_to_registerFragment);
            }
        });
    }

    private void handleLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Samo onemogući dugme i pozovi ViewModel.
        // Ne treba nam nikakav observer ovde.
        buttonLogin.setEnabled(false);
        authViewModel.login(email, password);
    }


}