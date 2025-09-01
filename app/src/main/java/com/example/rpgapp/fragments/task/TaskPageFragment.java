package com.example.rpgapp.fragments.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.rpgapp.R;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.databinding.FragmentTaskPageBinding;
import com.example.rpgapp.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskPageFragment extends Fragment {

    private FragmentTaskPageBinding binding;
    private Spinner statusSpinner;
    private String taskId;
    private Task currentTask;

    public static TaskPageFragment newInstance(String taskId) {
        TaskPageFragment fragment = new TaskPageFragment();
        Bundle args = new Bundle();
        args.putString("taskId", taskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskPageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        statusSpinner = binding.spinnerStatus;

        if (getArguments() != null) {
            taskId = getArguments().getString("taskId", null);
            if (taskId != null) loadTask(taskId);
        }

        binding.btnUpdateTask.setOnClickListener(v -> updateTask());
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());

        return root;
    }

    private void loadTask(String taskId) {
        TaskRepository.getInstance(getContext()).getTaskById(taskId)
                .observe(getViewLifecycleOwner(), task -> {
                    if (task != null) {
                        currentTask = task;
                        binding.taskTitle.setText(task.getTitle());
                        binding.taskDescription.setText(task.getDescription());
                        binding.taskCategory.setText("Kategorija: " + task.getCategory());
                        binding.taskDates.setText("Due: " + task.getDueDate() +
                                "\nStart: " + task.getStartDate() +
                                "\nEnd: " + task.getEndDate());
                        binding.taskXp.setText("XP: " + task.getTotalXp());

                        setupStatusSpinner(task);
                    }
                });
    }

    private void setupStatusSpinner(Task task) {
        String[] allStatuses = getResources().getStringArray(R.array.task_status_array);
        List<String> availableStatuses = new ArrayList<>();

        if ("active".equalsIgnoreCase(task.getStatus())) {
            availableStatuses.add("active");
            availableStatuses.add("done");
            availableStatuses.add("cancelled");
            if (task.isRecurring()) availableStatuses.add("paused");
        } else if ("paused".equalsIgnoreCase(task.getStatus())) {
            availableStatuses.add("paused");
            availableStatuses.add("active");
        } else {
            // Statusi koji se više ne mogu menjati
            availableStatuses.add(task.getStatus());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, availableStatuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        statusSpinner.setSelection(availableStatuses.indexOf(task.getStatus()));
    }

    private void updateTask() {
        if (currentTask == null) return;

        String selectedStatus = statusSpinner.getSelectedItem().toString();

        // Provera da li je zadatak prošao ili je neurađen/otkazan
        if ("undone".equalsIgnoreCase(currentTask.getStatus()) ||
                "cancelled".equalsIgnoreCase(currentTask.getStatus()) ||
                isTaskPast(currentTask)) {
            Toast.makeText(requireContext(), "Ovaj zadatak se ne može menjati", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pravila promene statusa
        if ("paused".equalsIgnoreCase(currentTask.getStatus()) &&
                "active".equalsIgnoreCase(selectedStatus)) {
            currentTask.setStatus("active");
        } else if ("active".equalsIgnoreCase(currentTask.getStatus())) {
            if ("done".equalsIgnoreCase(selectedStatus) ||
                    "cancelled".equalsIgnoreCase(selectedStatus) ||
                    ("paused".equalsIgnoreCase(selectedStatus) && currentTask.isRecurring())) {
                currentTask.setStatus(selectedStatus);
            } else {
                Toast.makeText(requireContext(), "Nevažeća promena statusa", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(requireContext(), "Nevažeća promena statusa", Toast.LENGTH_SHORT).show();
            return;
        }

        TaskRepository.getInstance(getContext()).updateTask(currentTask);
        Toast.makeText(requireContext(), "Status zadatka ažuriran", Toast.LENGTH_SHORT).show();
        loadTask(currentTask.getTaskId()); // osveži prikaz
    }

    private void deleteTask() {
        if (currentTask == null) return;

        if ("done".equalsIgnoreCase(currentTask.getStatus()) || isTaskPast(currentTask)) {
            Toast.makeText(requireContext(), "Završeni zadaci se ne mogu obrisati", Toast.LENGTH_SHORT).show();
            return;
        }

        TaskRepository repository = TaskRepository.getInstance(getContext());

        if (currentTask.isRecurring()) {
            repository.deleteFutureRecurringTasks(currentTask);
        } else {
            repository.deleteTask(currentTask);
        }

        Toast.makeText(requireContext(), "Zadatak je obrisan", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private boolean isTaskPast(Task task) {
        try {
            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                return TaskRepository.dateFormat.parse(task.getDueDate()).getTime() < System.currentTimeMillis();
            }
            if (task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                return TaskRepository.dateFormat.parse(task.getEndDate()).getTime() < System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
