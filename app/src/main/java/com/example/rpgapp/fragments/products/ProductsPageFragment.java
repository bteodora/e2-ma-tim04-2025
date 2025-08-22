package com.example.rpgapp.fragments.products;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.rpgapp.R;
import com.example.rpgapp.databinding.FragmentProductsPageBinding;
import com.example.rpgapp.fragments.FragmentTransition;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ProductsPageFragment extends Fragment {
    private Spinner spinner;

    int lastSpinnerSelection;
    int currentSelectedIndex;
    private ProductsPageViewModel productsViewModel;
    private FragmentProductsPageBinding binding;

    public static ProductsPageFragment newInstance() {
        return new ProductsPageFragment();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        productsViewModel = new ViewModelProvider(this).get(ProductsPageViewModel.class);

        binding = FragmentProductsPageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SearchView searchView = binding.searchText;
        productsViewModel.getText().observe(getViewLifecycleOwner(), searchView::setQueryHint);

        Button btnFilters = binding.btnFilters;
        btnFilters.setOnClickListener(v -> {
            Log.i("ShopApp", "Bottom Sheet Dialog");
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.FullScreenBottomSheetDialog);
            View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        });

        spinner = binding.btnSort;
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.sort_array));
        // Specify the layout to use when the list of choices appears
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(arrayAdapter);


        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.i("REZ", String.valueOf(currentSelectedIndex));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedIndex = spinner.getSelectedItemPosition();

                if (currentSelectedIndex != lastSpinnerSelection) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(requireActivity());
                    dialog.setMessage("Change the sort option?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Log.v("ShopApp", (String) parent.getItemAtPosition(position));
                                    ((TextView) parent.getChildAt(0)).setTextColor(Color.MAGENTA);
                                    lastSpinnerSelection = currentSelectedIndex;
                                    currentSelectedIndex = position;
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    spinner.setSelection(lastSpinnerSelection, true);
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = dialog.create();
                    alert.show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        FragmentTransition.to(ProductsListFragment.newInstance(), requireActivity(), false, R.id.scroll_products_list);

    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}