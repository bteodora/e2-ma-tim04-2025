package com.example.rpgapp.fragments.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.rpgapp.R;
import com.example.rpgapp.database.CategoryRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.databinding.FragmentTaskPageBinding;
import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Task;
import com.example.rpgapp.model.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TaskPageFragment extends Fragment {

    private FragmentTaskPageBinding binding;
    private Spinner statusSpinner;
    private String taskId;
    private Task currentTask;

    private static final int MAX_DAYS_PAST = 3; // limit od 3 dana

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskPageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        statusSpinner = binding.spinnerStatus;

        // Dobavljanje taskId iz argumenata
        if (getArguments() != null) {
            taskId = getArguments().getString("taskId", null);
            if (taskId != null) loadTask(taskId);
        }

        // Dugme za brisanje zadatka
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());

        // Dugme za ažuriranje (edit) zadatka
        binding.btnUpdateTask.setOnClickListener(v -> {
            if (currentTask == null) return;

            String status = currentTask.getStatus().toLowerCase();
            if ("urađen".equals(status) || "neurađen".equals(status) || "otkazan".equals(status) || isTaskPast(currentTask)) {
                Toast.makeText(requireContext(), "Ovaj zadatak se ne može menjati", Toast.LENGTH_SHORT).show();
                return;
            }

            // Dohvati trenutnu listu kategorija
            List<Category> categories = CategoryRepository.getInstance(getContext()).getAllCategories().getValue();

            if (categories == null || categories.isEmpty()) {
                Toast.makeText(requireContext(), "Prvo unesite kategoriju pre nego što ažurirate zadatak", Toast.LENGTH_SHORT).show();
                return;
            }

            // Navigacija ka EditTaskFragment
            Bundle bundle = new Bundle();
            bundle.putString("taskId", currentTask.getTaskId());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_taskPage_to_editTask, bundle);
        });


        return root;
    }


    private void loadTask(String taskId) {
        TaskRepository.getInstance(getContext()).getTaskById(taskId)
                .observe(getViewLifecycleOwner(), task -> {
                    if (task != null) {
                        currentTask = task;

                        // Provera da li zadatak treba automatski da postane "neurađen"
                        autoMarkTaskAsUnfinished(task);

                        binding.taskTitle.setText(task.getTitle());
                        binding.taskDescription.setText(task.getDescription());
                        binding.taskCategory.setText("Kategorija: " + task.getCategory());

                        // Prikaz datuma zavisno od tipa zadatka
                        if (task.isRecurring()) {
                            binding.taskDates.setText("Start: " + task.getStartDate() +
                                    "\nEnd: " + task.getEndDate());
                        } else {
                            binding.taskDates.setText("Due: " + task.getDueDate());
                        }

                        binding.taskXp.setText("XP: " + task.getTotalXp());

                        setupStatusSpinner(task);
                    }
                });
    }


    private void setupStatusSpinner(Task task) {
        List<String> availableStatuses = new ArrayList<>();
        String status = task.getStatus().toLowerCase();

        switch (status) {
            case "aktivan":
                availableStatuses.add("aktivan");
                availableStatuses.add("urađen");
                availableStatuses.add("otkazan");
                if (task.isRecurring()) availableStatuses.add("pauziran");
                break;
            case "pauziran":
                availableStatuses.add("pauziran");
                availableStatuses.add("aktivan");
                break;
            default: // neurađen, urađen, otkazan
                availableStatuses.add(status);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, availableStatuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        statusSpinner.setSelection(availableStatuses.indexOf(status));

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstSelection) {
                    firstSelection = false;
                    return;
                }
                String selectedStatus = availableStatuses.get(position);
                //updateTaskStatus(selectedStatus);
                TaskRepository taskRepo = TaskRepository.getInstance(requireContext());
                taskRepo.updateTaskStatus(task, selectedStatus, requireContext());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void autoMarkTaskAsUnfinished(Task task) {
        if (!"aktivan".equals(task.getStatus().toLowerCase()) || task.isRecurring()) return;

        try {
            Date dueDate = TaskRepository.dateFormat.parse(task.getDueDate());
            long diff = System.currentTimeMillis() - dueDate.getTime();
            long daysPast = diff / (1000 * 60 * 60 * 24);

            if (daysPast > MAX_DAYS_PAST) {
                task.setStatus("neurađen");
                TaskRepository.getInstance(getContext()).updateTask(task);
                Toast.makeText(requireContext(), "Zadatak je automatski označen kao neurađen", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteTask() {
        if (currentTask == null) return;

        String status = currentTask.getStatus().toLowerCase();
        if ("urađen".equals(status) || "neurađen".equals(status) || "otkazan".equals(status) || isTaskPast(currentTask)) {
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
