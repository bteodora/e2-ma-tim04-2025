package com.example.rpgapp.fragments.new_product;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rpgapp.database.ProductRepository;
import com.example.rpgapp.databinding.FragmentNewProductBinding;
import com.example.rpgapp.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NewProductFragment extends Fragment {

    private NewProductViewModel mViewModel;
    private FragmentNewProductBinding binding;
    private EditText titleText;
    private EditText descrText;
    private Button confirmButton;
    public static NewProductFragment newInstance() {
        return new NewProductFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(NewProductViewModel.class);

        binding = FragmentNewProductBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.productsTitle;
//        productsViewModel.getTitle().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(NewProductViewModel.class);
        titleText = binding.newTitle;
        descrText = binding.newDescr;
        confirmButton = binding.addNewPr;
        // TODO: Use the ViewModel
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewProduct();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void addNewProduct() {
        String title = titleText.getText().toString();
        String description = descrText.getText().toString();

        if (title.length() == 0 && description.length() == 0) {
            return;
        }

        // Pozivamo Repozitorijum na stari, proveren način
        ProductRepository productRepository = new ProductRepository(getContext());
        productRepository.open();
        productRepository.insertData(title, description, "image"); // Vratili smo stari poziv
        productRepository.close();

        // Obaveštavamo korisnika i vraćamo se nazad
        Toast.makeText(getContext(), "Proizvod je sačuvan!", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

}