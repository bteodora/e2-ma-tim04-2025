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
import com.example.rpgapp.model.Message;
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

    private MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    public LiveData<List<Message>> getMessages() { return messages; }

    public AllianceViewModel(@NonNull Application application) {
        super(application);
        allianceRepository = AllianceRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        loggedInUserLiveData = userRepository.getLoggedInUserLiveData();

        loggedInUserLiveData.observeForever(user -> {
            if (user != null && user.getAllianceId() != null && !user.getAllianceId().isEmpty()) {
                allianceRepository.listenToAlliance(user.getAllianceId(), alliance -> {
                    currentAlliance.postValue(alliance);
                });
            } else {
                currentAlliance.postValue(null);
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

        currentAlliance.observeForever(alliance -> {
            if (alliance != null) {
                allianceRepository.listenForMessages(alliance.getAllianceId(), messageList -> {
                    messages.postValue(messageList);
                });
            } else {
                allianceRepository.stopListeningForMessages();
                messages.postValue(new ArrayList<>());
            }
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

    public void sendMessage(String text) {
        Alliance alliance = currentAlliance.getValue();
        if (alliance != null && text != null && !text.trim().isEmpty()) {
            allianceRepository.sendMessage(alliance.getAllianceId(), text.trim(), new UserRepository.RequestCallback() {
                @Override public void onSuccess() { }
                @Override public void onFailure(Exception e) { }
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        allianceRepository.stopListeningForMessages();
    }
}