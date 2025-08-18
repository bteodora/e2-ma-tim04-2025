package com.example.shopapp.fragments.products;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProductsPageViewModel extends ViewModel {
    private final MutableLiveData<String> searchText;
    public ProductsPageViewModel(){
        searchText = new MutableLiveData<>();
        searchText.setValue("This is search help!");
    }
    public LiveData<String> getText(){
        return searchText;
    }
}
