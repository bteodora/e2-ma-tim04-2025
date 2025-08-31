package com.example.rpgapp.fragments.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rpgapp.R;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.databinding.FragmentTaskPageBinding;
import com.example.rpgapp.model.Task;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskPageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        statusSpinner = binding.spinnerStatus;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.task_status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        if (getArguments() != null) {
            taskId = getArguments().getString("taskId", null);
            if (taskId != null) loadTask(taskId);
        }

        binding.btnUpdateTask.setOnClickListener(v -> updateTask());
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());

        return root;
    }

    private void loadTask(String taskId) {
        TaskRepository.getInstance(getContext()).getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentTask = task;
                binding.taskTitle.setText(task.getTitle());
                binding.taskDescription.setText(task.getDescription());

                String[] statuses = getResources().getStringArray(R.array.task_status_array);
                for (int i = 0; i < statuses.length; i++) {
                    if (statuses[i].equals(task.getStatus())) {
                        statusSpinner.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private void updateTask() {
        if (currentTask == null) return;

        currentTask.setTitle(binding.taskTitle.getText().toString().trim());
        currentTask.setDescription(binding.taskDescription.getText().toString().trim());
        currentTask.setStatus(statusSpinner.getSelectedItem().toString());

        TaskRepository.getInstance(getContext()).updateTask(currentTask);
        Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show();
    }

    private void deleteTask() {
        if (currentTask == null) return;

        TaskRepository.getInstance(getContext()).deleteTask(currentTask);
        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
