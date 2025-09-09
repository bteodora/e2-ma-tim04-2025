package com.example.rpgapp.fragments.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.rpgapp.R;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.Calendar;
import java.util.Locale;

public class EditTaskFragment extends Fragment {

    private EditText etTitle, etDescription, etStartDate, etEndDate, etDueDate;
    private Spinner spinnerDifficulty, spinnerImportance;
    private Button btnUpdate;

    private Task currentTask;
    private String taskId;
    private TaskRepository repository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_edit, container, false);

        // Povezivanje polja
        etTitle = view.findViewById(R.id.etTaskTitle);
        etDescription = view.findViewById(R.id.etTaskDescription);
        etStartDate = view.findViewById(R.id.etTaskStartDate);
        etEndDate = view.findViewById(R.id.etTaskEndDate);
        etDueDate = view.findViewById(R.id.etTaskDueDate);

        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        spinnerImportance = view.findViewById(R.id.spinnerImportance);
        btnUpdate = view.findViewById(R.id.btnUpdateTask);

        repository = TaskRepository.getInstance(requireContext());

        // Spinner setup
        ArrayAdapter<CharSequence> adapterDiff = ArrayAdapter.createFromResource(requireContext(),
                R.array.task_difficulty_array, android.R.layout.simple_spinner_item);
        adapterDiff.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapterDiff);

        ArrayAdapter<CharSequence> adapterImp = ArrayAdapter.createFromResource(requireContext(),
                R.array.task_importance_array, android.R.layout.simple_spinner_item);
        adapterImp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(adapterImp);

        // Dobavljanje taskId iz argumenata
        if (getArguments() != null) {
            taskId = getArguments().getString("taskId");
            loadTask(taskId);
        }

        btnUpdate.setOnClickListener(v -> updateTask());

        // Date pickeri
        etDueDate.setOnClickListener(v -> showDatePicker(etDueDate));
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        return view;
    }

    private void loadTask(String taskId) {
        repository.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentTask = task;

                etTitle.setText(task.getTitle());
                etDescription.setText(task.getDescription());
                spinnerDifficulty.setSelection(task.getDifficultyXp() - 1);
                spinnerImportance.setSelection(task.getImportanceXp() - 1);

                if (task.isRecurring()) {
                    // Ponavljajući zadatak
                    etDueDate.setVisibility(View.GONE);
                    etStartDate.setVisibility(View.VISIBLE);
                    etEndDate.setVisibility(View.VISIBLE);
                    etStartDate.setText(task.getStartDate());
                    etEndDate.setText(task.getEndDate());
                } else {
                    // Jednokratni zadatak
                    etDueDate.setVisibility(View.VISIBLE);
                    etStartDate.setVisibility(View.GONE);
                    etEndDate.setVisibility(View.GONE);
                    etDueDate.setText(task.getDueDate());
                }
            }
        });
    }

    private void showDatePicker(EditText targetEditText) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    targetEditText.setText(dateStr);
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateTask() {
        if (currentTask == null) return;

        if ("done".equalsIgnoreCase(currentTask.getStatus()) || isTaskPast(currentTask)) {
            Toast.makeText(requireContext(), "Završeni zadaci se ne mogu menjati", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTask.setTitle(etTitle.getText().toString().trim());
        currentTask.setDescription(etDescription.getText().toString().trim());
        currentTask.setDifficultyXp(spinnerDifficulty.getSelectedItemPosition() + 1);
        currentTask.setImportanceXp(spinnerImportance.getSelectedItemPosition() + 1);
        currentTask.setTotalXp(currentTask.getDifficultyXp() + currentTask.getImportanceXp());

        if (currentTask.isRecurring()) {
            currentTask.setStartDate(etStartDate.getText().toString().trim());
            currentTask.setEndDate(etEndDate.getText().toString().trim());
            currentTask.setDueDate(null); // Sakrivamo dueDate za ponavljajuće
            repository.updateFutureRecurringTasks(currentTask);
        } else {
            currentTask.setDueDate(etDueDate.getText().toString().trim());
            currentTask.setStartDate(null);
            currentTask.setEndDate(null);
            repository.updateTask(currentTask);
        }

        Toast.makeText(requireContext(), "Zadatak izmenjen", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private boolean isTaskPast(Task task) {
        try {
            long now = System.currentTimeMillis();
            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                return TaskRepository.dateFormat.parse(task.getDueDate()).getTime() < now;
            }
            if (task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                return TaskRepository.dateFormat.parse(task.getEndDate()).getTime() < now;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
