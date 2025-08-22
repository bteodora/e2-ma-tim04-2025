package com.example.rpgapp.fragments.profile;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rpgapp.R;
import com.example.rpgapp.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private boolean isPermissions = true;
    private TextView contactView;
    private String permission = android.Manifest.permission.READ_CONTACTS;
    private FragmentProfileBinding binding;
    private ActivityResultLauncher<String> mPermissionResult;
    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        addMenu();
        return root;
    }



    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactView = (TextView) binding.contactview;
        mPermissionResult = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) {
                        startProcessingContacts();
                    } else {
                        Toast.makeText(getActivity(), "No permission.", Toast.LENGTH_SHORT).show();
                    }
                });
        processContact();

    }

    @Override
    public void onResume() {
        super.onResume();
        // Proveri i obradi kontakte ponovo kada se fragment vrati iz pozadine
        processContact();
    }

    private void processContact() {
        int getContacts = ContextCompat.checkSelfPermission(requireContext(), permission);
        if (getContacts != PackageManager.PERMISSION_GRANTED) {
            mPermissionResult.launch(permission);
        } else {
            startProcessingContacts();
        }
    }

    private void startProcessingContacts() {
        contactView.setText("");
        Cursor cursor = getContacts();

        // Provera da li je cursor null ili prazan
        if (cursor == null || cursor.getCount() == 0) {
            Log.i("ShopApp", "No contacts found.");
            contactView.append("No contacts available.\n");
            return;
        }

        Log.i("ShopApp", "GOT CONTACTS");
        contactView.append("Contacts:\n\n");

        // Iteracija kroz kontakte
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range")
                String displayName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                );

                if (displayName != null) {
                    Log.i("ShopApp", "IN CONTACTS");
                    contactView.append("Contact Name: ");
                    contactView.append(displayName);
                    contactView.append("\n");
                }
            } while (cursor.moveToNext());
        }

        // Zatvori cursor nakon završetka
        cursor.close();
    }

    private Cursor getContacts() {
        String[] projection = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };
        return requireActivity().getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection, null, null, null);
    }

    private void addMenu()
    {
        MenuProvider menuProvider = new MenuProvider()
        {

            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater)
            {
                menu.clear();
                menuInflater.inflate(R.menu.toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem)
            {
                NavController navController = Navigation.findNavController(getActivity(), R.id.fragment_nav_content_main);
                return NavigationUI.onNavDestinationSelected(menuItem, navController);
            }
        };

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

}