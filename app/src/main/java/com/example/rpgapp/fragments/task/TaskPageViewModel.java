package com.example.rpgapp.fragments.task;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TaskPageViewModel extends ViewModel {

    private final MutableLiveData<String> title;
    private final MutableLiveData<String> description;

    public TaskPageViewModel() {
        title = new MutableLiveData<>();
        description = new MutableLiveData<>();
        title.setValue("Task Title");
        description.setValue("Task Description");
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getDescription() {
        return description;
    }
}
