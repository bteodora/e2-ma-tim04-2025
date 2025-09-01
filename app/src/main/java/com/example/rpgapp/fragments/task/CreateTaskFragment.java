package com.example.rpgapp.fragments.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rpgapp.R;
import com.example.rpgapp.database.CategoryRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Task;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CreateTaskFragment extends Fragment {

    private EditText titleEdit, descEdit, repeatIntervalEdit;
    private Spinner categorySpinner, frequencySpinner, difficultySpinner, importanceSpinner, repeatUnitSpinner;
    private Button createBtn, pickDueDateBtn, pickStartDateBtn, pickEndDateBtn;

    private String[] frequencies = {"jednokratni", "ponavljajući"};
    private String[] repeatUnits = {"dan", "nedelja"};
    private String[] difficultyLevels = {"Veoma lak", "Lak", "Težak", "Ekstremno težak"};
    private String[] importanceLevels = {"Normalan", "Važan", "Ekstremno važan", "Specijalan"};

    private List<Category> loadedCategories; // kategorije iz baze

    private int selectedYear, selectedMonth, selectedDay;
    private String selectedDueDate, selectedStartDate, selectedEndDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_task, container, false);

        // Inicijalizacija polja
        titleEdit = view.findViewById(R.id.editTaskTitle);
        descEdit = view.findViewById(R.id.editTaskDescription);
        repeatIntervalEdit = view.findViewById(R.id.editRepeatInterval);

        categorySpinner = view.findViewById(R.id.spinnerCategory);
        frequencySpinner = view.findViewById(R.id.spinnerFrequency);
        difficultySpinner = view.findViewById(R.id.spinnerDifficulty);
        importanceSpinner = view.findViewById(R.id.spinnerImportance);
        repeatUnitSpinner = view.findViewById(R.id.spinnerRepeatUnit);

        createBtn = view.findViewById(R.id.btnCreateTask);
        pickDueDateBtn = view.findViewById(R.id.btnPickDueDate);
        pickStartDateBtn = view.findViewById(R.id.btnPickStartDate);
        pickEndDateBtn = view.findViewById(R.id.btnPickEndDate);

        // Spinneri za ostale atribute
        frequencySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, frequencies));
        difficultySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, difficultyLevels));
        importanceSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, importanceLevels));
        repeatUnitSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, repeatUnits));

        // Učitavanje kategorija iz baze
        CategoryRepository.getInstance(getContext())
                .getAllCategories()
                .observe(getViewLifecycleOwner(), categoryList -> {
                    if (categoryList != null && !categoryList.isEmpty()) {
                        loadedCategories = categoryList;
                        String[] categoryNames = new String[categoryList.size()];
                        for (int i = 0; i < categoryList.size(); i++) {
                            categoryNames[i] = categoryList.get(i).getName();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_dropdown_item, categoryNames);
                        categorySpinner.setAdapter(adapter);
                    }
                });

        // Prikazivanje polja u zavisnosti od tipa taska
        frequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String freq = parent.getItemAtPosition(position).toString();
                if(freq.equalsIgnoreCase("ponavljajući")) {
                    repeatIntervalEdit.setVisibility(View.VISIBLE);
                    repeatUnitSpinner.setVisibility(View.VISIBLE);
                    pickStartDateBtn.setVisibility(View.VISIBLE);
                    pickEndDateBtn.setVisibility(View.VISIBLE);
                    pickDueDateBtn.setVisibility(View.GONE);
                } else {
                    repeatIntervalEdit.setVisibility(View.GONE);
                    repeatUnitSpinner.setVisibility(View.GONE);
                    pickStartDateBtn.setVisibility(View.GONE);
                    pickEndDateBtn.setVisibility(View.GONE);
                    pickDueDateBtn.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Date pickeri
        pickDueDateBtn.setOnClickListener(v -> showDatePicker("due"));
        pickStartDateBtn.setOnClickListener(v -> showDatePicker("start"));
        pickEndDateBtn.setOnClickListener(v -> showDatePicker("end"));

        // Dugme za kreiranje zadatka
        createBtn.setOnClickListener(v -> createTask());

        return view;
    }

    private void showDatePicker(String type) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;

                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);

                    switch (type) {
                        case "due":
                            selectedDueDate = dateStr;
                            pickDueDateBtn.setText(dateStr);
                            break;
                        case "start":
                            selectedStartDate = dateStr;
                            pickStartDateBtn.setText(dateStr);
                            break;
                        case "end":
                            selectedEndDate = dateStr;
                            pickEndDateBtn.setText(dateStr);
                            break;
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void createTask() {
        String title = titleEdit.getText().toString().trim();
        String desc = descEdit.getText().toString().trim();
        String category = categorySpinner.getSelectedItem() != null ? categorySpinner.getSelectedItem().toString() : "";
        String frequency = frequencySpinner.getSelectedItem().toString();

        if(title.isEmpty()) {
            Toast.makeText(requireContext(), "Enter task title", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mapiranje XP vrednosti za težinu
        int difficultyXp;
        switch (difficultySpinner.getSelectedItem().toString()) {
            case "Veoma lak": difficultyXp = 1; break;
            case "Lak": difficultyXp = 3; break;
            case "Težak": difficultyXp = 7; break;
            case "Ekstremno težak": difficultyXp = 20; break;
            default: difficultyXp = 1;
        }

        // Mapiranje XP vrednosti za bitnost
        int importanceXp;
        switch (importanceSpinner.getSelectedItem().toString()) {
            case "Normalan": importanceXp = 1; break;
            case "Važan": importanceXp = 3; break;
            case "Ekstremno važan": importanceXp = 10; break;
            case "Specijalan": importanceXp = 100; break;
            default: importanceXp = 1;
        }

        // Kreiranje Task objekta
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTitle(title);
        task.setDescription(desc);
        task.setCategory(category);
        task.setColor(getCategoryColor(category));
        task.setFrequency(frequency);
        task.setDifficultyXp(difficultyXp);
        task.setImportanceXp(importanceXp);
        task.setTotalXp(difficultyXp + importanceXp);
        task.setStatus("aktivan");
        task.setRecurring(frequency.equalsIgnoreCase("ponavljajući"));
        task.setRecurringId(task.isRecurring() ? UUID.randomUUID().toString() : null);

        if(frequency.equalsIgnoreCase("jednokratni")) {
            task.setDueDate(selectedDueDate);
            task.setStartDate(null);
            task.setEndDate(null);
            task.setInterval(0);
            task.setIntervalUnit(null);
        } else {
            int interval = repeatIntervalEdit.getText().toString().isEmpty() ? 1 :
                    Integer.parseInt(repeatIntervalEdit.getText().toString());
            task.setInterval(interval);
            task.setIntervalUnit(repeatUnitSpinner.getSelectedItem().toString());
            task.setStartDate(selectedStartDate != null ? selectedStartDate : "");
            task.setEndDate(selectedEndDate != null ? selectedEndDate : "");
            task.setDueDate(null);
        }

        if(frequency.equalsIgnoreCase("ponavljajući")) {
            if(selectedStartDate == null || selectedStartDate.isEmpty() ||
                    selectedEndDate == null || selectedEndDate.isEmpty()) {
                Toast.makeText(requireContext(), "Odaberi start i end date", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Snimanje u bazu
        TaskRepository.getInstance(getContext()).addTask(task); // ili updateTask ako menjaš postojeći

        Toast.makeText(requireContext(), "Task created", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private String getCategoryColor(String categoryName) {
        if(loadedCategories != null) {
            for(Category cat : loadedCategories) {
                if(cat.getName().equalsIgnoreCase(categoryName)) {
                    return cat.getColor();
                }
            }
        }
        return "#9E9E9E"; // default boja ako nema
    }
}
