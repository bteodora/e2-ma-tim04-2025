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

    private MutableLiveData<ShopScreenState> screenState = new MutableLiveData<>();
    private User currentUser; // Čuvaćemo ga ovde
    private List<Item> currentShopItems;
    private MutableLiveData<String> purchaseStatus = new MutableLiveData<>();

    public ShopViewModel(@NonNull Application application) {
        super(application);
        // Pretpostavka da EquipmentRepository nema zavisnosti
        equipmentRepository = new EquipmentRepository();
        userRepository = UserRepository.getInstance(application);
        authRepository = new AuthRepository(application);

        loadInitialData();
    }

    public LiveData<ShopScreenState> getScreenState() { return screenState; }
    public LiveData<String> getPurchaseStatus() { return purchaseStatus; }

    // Getteri ostaju isti
    public LiveData<User> getCurrentUser() { return currentUserLiveData; }
    public LiveData<List<Item>> getShopItems() { return shopItemsLiveData; }

    private void loadInitialData() {
        currentShopItems = equipmentRepository.getShopStock();
        FirebaseUser fbUser = authRepository.getCurrentUser();
        if (fbUser != null) {
            userRepository.getUserById(fbUser.getUid(), new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    currentUser = user;
                    updateScreenState();
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

    private void updateScreenState() {
        if (currentUser != null && currentShopItems != null) {
            screenState.postValue(new ShopScreenState(currentUser, currentShopItems));
        }
    }

    public void purchaseItem(Item itemToBuy) {
        if (currentUser == null) {
            purchaseStatus.postValue("Error: User data not available!");
            return;
        }
        int price = itemToBuy.calculatePrice(currentUser.calculatePreviosPrizeFormula());

        boolean isSuccess = equipmentRepository.purchaseItem(currentUser, itemToBuy, price);

        if (isSuccess) {
            userRepository.updateUser(currentUser);

            updateScreenState();

            purchaseStatus.postValue("Purchase successful!");
        } else {
            purchaseStatus.postValue("Purchase failed! Check coins or if you already own this item.");
        }
    }
}