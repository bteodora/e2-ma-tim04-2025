package com.example.rpgapp.fragments.registration;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.rpgapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link fragment_register#newInstance} factory method to
 * create an instance of this fragment.
 */
public class fragment_register extends Fragment {

    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private ImageView imageViewAvatar;
    private Button buttonChooseAvatar;
    private Button buttonRegister;

    private EditText editTextPasswordConfirm;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private AuthViewModel authViewModel;
    private String selectedAvatarId = "default_avatar";

    public fragment_register() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static fragment_register newInstance(String param1, String param2) {
        fragment_register fragment = new fragment_register();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        imageViewAvatar = view.findViewById(R.id.imageViewAvatar);
        buttonChooseAvatar = view.findViewById(R.id.buttonChooseAvatar);
        buttonRegister = view.findViewById(R.id.buttonRegister);
        editTextPasswordConfirm = view.findViewById(R.id.editTextPasswordConfirm);

        // Sada postavljamo "osluškivače" za klikove
        buttonRegister.setOnClickListener(v -> {
            // Logika za registraciju će ići ovde KASNIJE
            Log.d("RegisterFragment", "Dugme za registraciju kliknuto!");
            Toast.makeText(getContext(), "Registracija kliknuta!", Toast.LENGTH_SHORT).show();
        });

        observeViewModel();

        buttonRegister.setOnClickListener(v -> {
            // Sada klik na dugme pokreće pravu logiku
            handleRegistration();
        });

        buttonChooseAvatar.setOnClickListener(v -> {
            // Logika za biranje avatara će ići ovde KASNIJE
            Log.d("RegisterFragment", "Dugme za biranje avatara kliknuto!");
            Toast.makeText(getContext(), "Biram avatara!", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        authViewModel.getRegistrationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Registracija uspešna! Proverite email za verifikaciju.", Toast.LENGTH_LONG).show();
                // TODO: Vrati korisnika na ekran za logovanje
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
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
            Toast.makeText(getContext(), "Password must be at least 6 characters long!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(getContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Dodaj bolju validaciju (da li je email validan, da li lozinka ima 6 karaktera, itd.)
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Sva polja su obavezna!", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(email, password, username, selectedAvatarId);
    }
}