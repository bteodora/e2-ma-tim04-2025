package com.example.rpgapp.services;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.List;

public class TaskService {

    private static TaskService instance;
    private final TaskRepository taskRepository;

    private TaskService(Context context) {
        this.taskRepository = TaskRepository.getInstance(context.getApplicationContext());
    }

    // Singleton getter
    public static TaskService getInstance(Context context) {
        if (instance == null) {
            instance = new TaskService(context);
        }
        return instance;
    }

    // Vrati sve taskove kao LiveData
    public LiveData<List<Task>> getAllTasks() {
        return taskRepository.getAllTasksLiveData();
    }

    // Dodaj task
    public void addTask(Task task) {
        taskRepository.addTask(task);
    }

    // Update task
    public void updateTask(Task task) {
        taskRepository.updateTask(task);
    }

    // Delete task
    public void deleteTask(Task task) {
        taskRepository.deleteTask(task);
    }

    // Vrati task po ID
    public LiveData<Task> getTaskById(String id) {
        return taskRepository.getTaskById(id);
    }

    // Success rate (procenat urađenih taskova)
    public LiveData<Double> getSuccessRate() {
        return Transformations.map(taskRepository.getAllTasksLiveData(), tasks -> {
            if (tasks == null || tasks.isEmpty()) return 0.0;
            long doneCount = tasks.stream()
                    .filter(t -> "urađen".equalsIgnoreCase(t.getStatus()))
                    .count();
            return (doneCount * 100.0) / tasks.size();
        });
    }
}
