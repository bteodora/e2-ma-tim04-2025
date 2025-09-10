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
import com.example.rpgapp.database.CategoryRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Task;

import java.util.Calendar;
import java.util.Locale;

public class EditTaskFragment extends Fragment {

    private EditText etTitle, etDescription, etStartDate, etEndDate, etDueDate;
    private Spinner spinnerDifficulty, spinnerImportance, spinnerCategory;
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
        spinnerCategory = view.findViewById(R.id.spinnerCategory);


        // Dohvati sve kategorije iz CategoryRepository
        CategoryRepository categoryRepo = CategoryRepository.getInstance(requireContext());
        categoryRepo.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                // Napravi niz naziva kategorija
                String[] catNames = new String[categories.size()];
                for (int i = 0; i < categories.size(); i++) {
                    catNames[i] = categories.get(i).getName();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, catNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);

//                if (currentTask != null && currentTask.getCategory() != null) {
//                    for (int i = 0; i < catNames.length; i++) {
//                        if (catNames[i].equals(currentTask.getCategory())) {
//                            spinnerCategory.setSelection(i);
//                            break;
//                        }
//                    }
//                }
                if (currentTask != null && currentTask.getCategory() != null) {
                    int pos = adapter.getPosition(currentTask.getCategory());
                    if (pos >= 0) spinnerCategory.setSelection(pos);
                }

            }
        });


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

//    private void loadTask(String taskId) {
//        repository.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
//            if (task != null) {
//                currentTask = task;
//
//                etTitle.setText(task.getTitle());
//                etDescription.setText(task.getDescription());
//                spinnerDifficulty.setSelection(task.getDifficultyXp() - 1);
//                spinnerImportance.setSelection(task.getImportanceXp() - 1);
//
//                if (task.isRecurring()) {
//                    // Ponavljajući zadatak
//                    etDueDate.setVisibility(View.GONE);
//                    etStartDate.setVisibility(View.VISIBLE);
//                    etEndDate.setVisibility(View.VISIBLE);
//                    etStartDate.setText(task.getStartDate());
//                    etEndDate.setText(task.getEndDate());
//                } else {
//                    // Jednokratni zadatak
//                    etDueDate.setVisibility(View.VISIBLE);
//                    etStartDate.setVisibility(View.GONE);
//                    etEndDate.setVisibility(View.GONE);
//                    etDueDate.setText(task.getDueDate());
//                }
//                // Spinner kategorija
//                if (spinnerCategory.getAdapter() != null) {
//                    for (int i = 0; i < spinnerCategory.getAdapter().getCount(); i++) {
//                        if (spinnerCategory.getItemAtPosition(i).toString().equals(task.getCategory())) {
//                            spinnerCategory.setSelection(i);
//                            break;
//                        }
//                    }
//                }
//
//            }
//        });
//    }
private void loadTask(String taskId) {
    repository.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
        if (task != null) {
            currentTask = task;

            // Popuni osnovne podatke
            etTitle.setText(task.getTitle());
            etDescription.setText(task.getDescription());

            // --- Spinner Difficulty ---
            if (spinnerDifficulty.getAdapter() != null) {
                int diffPos = task.getDifficultyXp() - 1;
                if (diffPos >= 0 && diffPos < spinnerDifficulty.getAdapter().getCount()) {
                    spinnerDifficulty.setSelection(diffPos);
                }
            }

            // --- Spinner Importance ---
            if (spinnerImportance.getAdapter() != null) {
                int impPos = task.getImportanceXp() - 1;
                if (impPos >= 0 && impPos < spinnerImportance.getAdapter().getCount()) {
                    spinnerImportance.setSelection(impPos);
                }
            }

            // --- Datumi ---
            if (task.isRecurring()) {
                etDueDate.setVisibility(View.GONE);
                etStartDate.setVisibility(View.VISIBLE);
                etEndDate.setVisibility(View.VISIBLE);
                etStartDate.setText(task.getStartDate());
                etEndDate.setText(task.getEndDate());
            } else {
                etDueDate.setVisibility(View.VISIBLE);
                etStartDate.setVisibility(View.GONE);
                etEndDate.setVisibility(View.GONE);
                etDueDate.setText(task.getDueDate());
            }

            // --- Spinner Category ---
            spinnerCategory.post(() -> {
                if (spinnerCategory.getAdapter() != null && task.getCategory() != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
                    int pos = adapter.getPosition(task.getCategory());
                    if (pos >= 0 && pos < adapter.getCount()) {
                        spinnerCategory.setSelection(pos);
                    }
                }
            });
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

        // Provera da li je zadatak završen ili prošao
        if ("done".equalsIgnoreCase(currentTask.getStatus()) || isTaskPast(currentTask)) {
            Toast.makeText(requireContext(), "Završeni zadaci se ne mogu menjati", Toast.LENGTH_SHORT).show();
            return;
        }

        // Osnovni podaci
        currentTask.setTitle(etTitle.getText().toString().trim());
        currentTask.setDescription(etDescription.getText().toString().trim());
        currentTask.setDifficultyXp(spinnerDifficulty.getSelectedItemPosition() + 1);
        currentTask.setImportanceXp(spinnerImportance.getSelectedItemPosition() + 1);
        currentTask.setTotalXp(currentTask.getDifficultyXp() + currentTask.getImportanceXp());

        // --- KATEGORIJA I BOJA ---
        String selectedCategoryName = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString() : null;

        if (selectedCategoryName != null) {
            CategoryRepository categoryRepo = CategoryRepository.getInstance(requireContext());
            if (categoryRepo.getAllCategories().getValue() != null) {
                Category selectedCategory = categoryRepo.getAllCategories().getValue().stream()
                        .filter(c -> c.getName().equals(selectedCategoryName))
                        .findFirst()
                        .orElse(null);

                if (selectedCategory != null) {
                    currentTask.setCategory(selectedCategory.getName());
                    currentTask.setColor(selectedCategory.getColor()); // boja kategorije
                } else {
                    currentTask.setCategory(selectedCategoryName);
                    currentTask.setColor("#808080"); // default siva
                }
            } else {
                // fallback ako lista kategorija još nije učitana
                currentTask.setCategory(selectedCategoryName);
                currentTask.setColor("#808080"); // default siva
            }
        }

        // --- Datumi ---
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
