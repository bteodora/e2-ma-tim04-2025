package com.example.rpgapp.fragments.products;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.ProductListAdapter;
import com.example.rpgapp.database.DBContentProvider;
import com.example.rpgapp.database.ProductRepository;
import com.example.rpgapp.database.SQLiteHelper;
import com.example.rpgapp.databinding.FragmentProductsListBinding;
import com.example.rpgapp.model.Product;
import java.util.ArrayList;

public class ProductsListFragment extends ListFragment implements AdapterView.OnItemLongClickListener{
    private static final int PRODUCT_CREATE = 0;
    private static final int PRODUCT_EDIT = 1;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private ProductListAdapter adapter;
    private static final String ARG_PARAM = "param";
    private FragmentProductsListBinding binding;
    MenuProvider menuProvider;

    public static ProductsListFragment newInstance(){
        ProductsListFragment fragment = new ProductsListFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i("ShopApp", "onCreateView Products List Fragment");
        binding = FragmentProductsListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        addMenu();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ShopApp", "onCreate Products List Fragment");
        this.getListView().setDividerHeight(2);
        fillData();
    }

    @Override
    public void onResume() {
        super.onResume();
        fillData();
    }

    private void addMenu()
    {
        menuProvider = new MenuProvider()
        {

            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater)
            {
                menu.clear();
                menuInflater.inflate(R.menu.products_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem)
            {
                NavController navController = Navigation.findNavController(getActivity(), R.id.fragment_nav_content_main);
                // Nakon toga, koristi se NavigationUI.onNavDestinationSelected(item, navController)
                // kako bi se omogućila integracija između MenuItem-a i odredišta unutar aplikacije
                // definisanih unutar navigacionog grafa (NavGraph).
                // Ova funkcija proverava da li je odabrana stavka izbornika povezana s nekim
                // odredištem unutar navigacionog grafa i pokreće tu navigaciju ako postoji
                // odgovarajuće podudaranje.
                return NavigationUI.onNavDestinationSelected(menuItem, navController);
            }
        };

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Handle the click on item at 'position'
    }
    private void fillData() {
        String[] projection = { SQLiteHelper.COLUMN_ID, SQLiteHelper.COLUMN_TITLE, SQLiteHelper.COLUMN_DESCRIPTION };
//       preko content resolver-a
//        Cursor cursor = requireActivity().getContentResolver()
//                .query(DBContentProvider.CONTENT_URI_PRODUCTS, projection, null, null, null);

//        preko repository-ja
        ProductRepository productRepository = new ProductRepository(getContext());
        productRepository.open();
        Cursor cursor = productRepository.getData(null, projection, null, null, null);

        ArrayList<Product> products = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Product row = new Product();
                row.setId(cursor.getLong(0));
                row.setTitle(cursor.getString(1));
                row.setDescription(cursor.getString(2));
                products.add(row);
            }
            adapter = new ProductListAdapter(getActivity(), products);

            setListAdapter(adapter);

        }
//        productRepository.close();
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Uri uri = Uri.parse(DBContentProvider.CONTENT_URI_PRODUCTS + "/"
                + ((Product)adapterView.getItemAtPosition(i)).getId());
        requireContext().getContentResolver().delete(uri, null, null);
        adapter.notifyDataSetChanged();
        return false;
    }

}
