package com.example.rpgapp.fragments.alliance;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.User;

import java.util.ArrayList;
import java.util.List;

public class AllianceViewModel extends AndroidViewModel {

    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private LiveData<User> loggedInUserLiveData;
    private MutableLiveData<Alliance> currentAlliance = new MutableLiveData<>();
    public final LiveData<List<User>> members;
    private MutableLiveData<Boolean> actionStatus = new MutableLiveData<>();
    public LiveData<Boolean> getActionStatus() { return actionStatus; }
    public void resetActionStatus() { actionStatus.setValue(null); }

    public AllianceViewModel(@NonNull Application application) {
        super(application);
        allianceRepository = AllianceRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        loggedInUserLiveData = userRepository.getLoggedInUserLiveData();

        // Slušaj promene na ulogovanom korisniku
        loggedInUserLiveData.observeForever(user -> {
            if (user != null && user.getAllianceId() != null && !user.getAllianceId().isEmpty()) {
                // Ako je korisnik u savezu, počni da slušaš promene na tom savezu
                allianceRepository.listenToAlliance(user.getAllianceId(), alliance -> {
                    currentAlliance.postValue(alliance);
                });
            } else {
                currentAlliance.postValue(null); // Korisnik nije u savezu
            }
        });

        members = Transformations.switchMap(currentAlliance, alliance -> {
            MutableLiveData<List<User>> memberProfiles = new MutableLiveData<>();
            if (alliance != null && alliance.getMemberIds() != null) {
                allianceRepository.getMemberProfiles(alliance.getMemberIds(), new UserRepository.FriendsCallback() {
                    @Override
                    public void onFriendsLoaded(List<User> users) {
                        memberProfiles.postValue(users);
                    }
                    @Override
                    public void onError(Exception e) {
                        memberProfiles.postValue(new ArrayList<>());
                    }
                });
            } else {
                memberProfiles.postValue(new ArrayList<>());
            }
            return memberProfiles;
        });
    }

    public LiveData<Alliance> getCurrentAlliance() {
        return currentAlliance;
    }

    public LiveData<User> getLoggedInUser() {
        return loggedInUserLiveData;
    }


    public void disbandAlliance() {
        Alliance alliance = currentAlliance.getValue();
        if (alliance != null) {
            allianceRepository.disbandAlliance(alliance.getAllianceId(), alliance.getMemberIds(), new UserRepository.RequestCallback() {
                @Override public void onSuccess() { actionStatus.postValue(true); }
                @Override public void onFailure(Exception e) { actionStatus.postValue(false); }
            });
        }
    }

    public void leaveAlliance() {
        Alliance alliance = currentAlliance.getValue();
        User user = loggedInUserLiveData.getValue();
        if (alliance != null && user != null) {
            allianceRepository.leaveAlliance(alliance.getAllianceId(), user.getUserId(), new UserRepository.RequestCallback() {
                @Override public void onSuccess() { actionStatus.postValue(true); }
                @Override public void onFailure(Exception e) { actionStatus.postValue(false); }
            });
        }
    }
}