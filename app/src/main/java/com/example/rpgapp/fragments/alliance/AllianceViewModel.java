package com.example.rpgapp.fragments.alliance;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.User;

public class AllianceViewModel extends AndroidViewModel {

    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private LiveData<User> loggedInUserLiveData;

    // LiveData za prikazivanje detalja o savezu
    private MutableLiveData<Alliance> currentAlliance = new MutableLiveData<>();

    public AllianceViewModel(@NonNull Application application) {
        super(application);
        allianceRepository = AllianceRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        loggedInUserLiveData = userRepository.getLoggedInUserLiveData();

        loggedInUserLiveData.observeForever(user -> {
            if (user != null && user.getAllianceId() != null) {
                loadAllianceDetails(user.getAllianceId());
            } else {
                currentAlliance.postValue(null);
            }
        });
    }

    public LiveData<Alliance> getCurrentAlliance() {
        return currentAlliance;
    }

    private void loadAllianceDetails(String allianceId) {
        // TODO: U AllianceRepository dodati metodu getAllianceById
        // allianceRepository.getAllianceById(allianceId, alliance -> {
        //     currentAlliance.postValue(alliance);
        // });
    }

    // TODO: Ovde ćemo dodati metode za ukidanje saveza, napuštanje, slanje poruka...
}