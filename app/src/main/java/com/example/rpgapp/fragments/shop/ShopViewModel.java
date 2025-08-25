package com.example.rpgapp.fragments.shop;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.rpgapp.database.AuthRepository;
import com.example.rpgapp.database.EquipmentRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Item;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ShopViewModel extends AndroidViewModel {

    private EquipmentRepository equipmentRepository;
    private UserRepository userRepository;
    private AuthRepository authRepository;

    private MutableLiveData<User> currentUserLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Item>> shopItemsLiveData = new MutableLiveData<>();
    private MutableLiveData<String> purchaseStatusLiveData = new MutableLiveData<>();

    public ShopViewModel(@NonNull Application application) {
        super(application);
        // Pretpostavka da EquipmentRepository nema zavisnosti
        equipmentRepository = new EquipmentRepository();
        userRepository = new UserRepository(application);
        authRepository = new AuthRepository(application);

        loadInitialData();
    }

    // Getteri ostaju isti
    public LiveData<User> getCurrentUser() { return currentUserLiveData; }
    public LiveData<List<Item>> getShopItems() { return shopItemsLiveData; }
    public LiveData<String> getPurchaseStatus() { return purchaseStatusLiveData; }

    private void loadInitialData() {
        shopItemsLiveData.postValue(equipmentRepository.getShopStock());

        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser != null) {
            userRepository.getUserById(firebaseUser.getUid(), new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    currentUserLiveData.postValue(user);
                }

                @Override
                public void onError(Exception e) {
                    purchaseStatusLiveData.postValue("Failed to load user data.");
                }
            });
        } else {
            purchaseStatusLiveData.postValue("Error: User not logged in!");
        }
    }

    public void purchaseItem(Item itemToBuy) {
        User user = currentUserLiveData.getValue();
        if (user == null) {
            purchaseStatusLiveData.postValue("Error: User data not available!");
            return;
        }

        int price = itemToBuy.calculatePrice(user.calculatePreviosPrizeFormula());

        boolean isSuccess = equipmentRepository.purchaseItem(user, itemToBuy, price);

        if (isSuccess) {
            userRepository.updateUser(user);

            purchaseStatusLiveData.postValue("Purchase successful!");
        } else {
            purchaseStatusLiveData.postValue("Purchase failed! Check if you have enough coins or already own this item.");
        }
    }
}