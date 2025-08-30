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
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class CreateTaskFragment extends Fragment {

    private EditText titleEdit, descEdit, repeatIntervalEdit;
    private Spinner categorySpinner, frequencySpinner, difficultySpinner, importanceSpinner, repeatUnitSpinner;
    private Button createBtn, pickDateBtn;

    private String[] categories = {"zdravlje", "učenje", "zabava", "sređivanje"};
    private String[] frequencies = {"jednokratni", "ponavljajući"};
    private String[] repeatUnits = {"dan", "nedelja"};
    private String[] difficultyLevels = {"Veoma lak", "Lak", "Težak", "Ekstremno težak"};
    private String[] importanceLevels = {"Normalan", "Važan", "Ekstremno važan", "Specijalan"};

    private int selectedYear, selectedMonth, selectedDay;

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
        pickDateBtn = view.findViewById(R.id.btnPickDateTime);

        // Popunjavanje Spinner-a
        categorySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
        frequencySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, frequencies));
        difficultySpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, difficultyLevels));
        importanceSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, importanceLevels));
        repeatUnitSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, repeatUnits));

        // Prikaz interval polja samo kada je ponavljajući zadatak
        frequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String freq = parent.getItemAtPosition(position).toString();
                if(freq.equalsIgnoreCase("ponavljajući")) {
                    repeatIntervalEdit.setVisibility(View.VISIBLE);
                    repeatUnitSpinner.setVisibility(View.VISIBLE);
                } else {
                    repeatIntervalEdit.setVisibility(View.GONE);
                    repeatUnitSpinner.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Date picker dugme
        pickDateBtn.setOnClickListener(v -> showDatePicker());

        // Dugme za kreiranje zadatka
        createBtn.setOnClickListener(v -> createTask());

        return view;
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;

                    String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    pickDateBtn.setText(dateStr);
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void createTask() {
        String title = titleEdit.getText().toString().trim();
        String desc = descEdit.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String frequency = frequencySpinner.getSelectedItem().toString();
        String repeatUnit = repeatUnitSpinner.getSelectedItem().toString();
        int interval = repeatIntervalEdit.getText().toString().isEmpty() ? 1 :
                Integer.parseInt(repeatIntervalEdit.getText().toString());

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

        // Formatiranje izabranog datuma
        String dueDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedYear, selectedMonth + 1, selectedDay);

        // Kreiranje Task objekta
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTitle(title);
        task.setDescription(desc);
        task.setCategory(category);
        task.setColor(getCategoryColor(category));
        task.setFrequency(frequency);
        task.setInterval(interval);
        task.setIntervalUnit(repeatUnit);
        task.setDifficultyXp(difficultyXp);
        task.setImportanceXp(importanceXp);
        task.setTotalXp(difficultyXp + importanceXp);
        task.setDueDate(dueDate); // <--- Čuva izabrani datum
        task.setStatus("aktivan");

        // Snimanje u bazu
        TaskRepository.getInstance(getContext()).updateTask(task);
        Toast.makeText(requireContext(), "Task created", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private String getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "zdravlje": return "#FF5722";
            case "učenje": return "#3F51B5";
            case "zabava": return "#4CAF50";
            case "sređivanje": return "#FFC107";
            default: return "#9E9E9E";
        }
    }
}
