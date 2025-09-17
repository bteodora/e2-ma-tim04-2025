package com.example.rpgapp.fragments.task;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.ListFragment;
import androidx.navigation.Navigation;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.TaskAdapter;
import com.example.rpgapp.database.CategoryRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskListFragment extends ListFragment implements AdapterView.OnItemLongClickListener {

    private TaskAdapter adapter;
    private static final String TAG = "TaskListFragment";
    private ImageButton btnCalendar;
    private Spinner spinnerFilter;
    private List<Task> allTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnCalendar = view.findViewById(R.id.btnCalendar);
        btnCalendar.setOnClickListener(v -> openCalendar());

        spinnerFilter = view.findViewById(R.id.spinnerFilter);
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Svi zadaci", "Jednokratni", "Ponavljajući"}
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        getListView().setDividerHeight(2);
        getListView().setOnItemLongClickListener(this);

        fillData();
    }

    @Override
    public void onResume() {
        super.onResume();
        fillData();
    }

    private void fillData() {
        TaskRepository repository = TaskRepository.getInstance(getContext());
        repository.getAllTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks == null) tasks = new ArrayList<>();

            List<Task> currentAndFutureTasks = new ArrayList<>();
            long today = System.currentTimeMillis();

            for (Task task : tasks) {
                try {
                    if (isTaskPast(task)) {
                        continue;
                    }

                    // Postavljanje boje kategorije odmah
                    if (task.getCategory() != null) {
                        String color = getCategoryColor(task.getCategory());
                        task.setColor(color);
                    }

                    // Jednokratni zadaci
                    if (task.getDueDate() != null && parseDateToMillis(task.getDueDate()) >= today) {
                        currentAndFutureTasks.add(task);
                    }
                    // Ponavljajući zadaci
                    else if (task.getStartDate() != null && task.getEndDate() != null) {
                        Calendar start = Calendar.getInstance();
                        Calendar end = Calendar.getInstance();
                        start.setTime(TaskRepository.dateFormat.parse(task.getStartDate()));
                        end.setTime(TaskRepository.dateFormat.parse(task.getEndDate()));

                        Calendar current = (Calendar) start.clone();
                        while (!current.after(end)) {
                            if (current.getTimeInMillis() >= today) {
                                currentAndFutureTasks.add(task);
                                break;
                            }

                            if ("dan".equalsIgnoreCase(task.getIntervalUnit())) {
                                current.add(Calendar.DAY_OF_MONTH, task.getInterval());
                            } else if ("nedelja".equalsIgnoreCase(task.getIntervalUnit())) {
                                current.add(Calendar.WEEK_OF_YEAR, task.getInterval());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            allTasks = currentAndFutureTasks;

            adapter = new TaskAdapter(getActivity(), allTasks, (task, view) -> {
                Toast.makeText(requireContext(), "Navigating to taskId: " + task.getTaskId(), Toast.LENGTH_SHORT).show();

                Bundle bundle = new Bundle();
                bundle.putString("taskId", task.getTaskId());
                Navigation.findNavController(view).navigate(R.id.action_taskList_to_taskPage, bundle);
            });

            setListAdapter(adapter);

            // primeni filter nakon punjenja liste
            applyFilter(spinnerFilter.getSelectedItemPosition());
        });
    }


    private void applyFilter(int filterType) {
        if (adapter == null) return;

        List<Task> filtered = new ArrayList<>();

        for (Task task : allTasks) {
            switch (filterType) {
                case 0: // svi
                    filtered.add(task);
                    break;
                case 1: // jednokratni
                    if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                        filtered.add(task);
                    }
                    break;
                case 2: // ponavljajući
                    if (task.getStartDate() != null && !task.getStartDate().isEmpty()
                            && task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                        filtered.add(task);
                    }
                    break;
            }
        }

        adapter.setTasks(filtered);
    }

    private long parseDateToMillis(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private void openCalendar() {
        Navigation.findNavController(requireView()).navigate(R.id.action_taskList_to_taskCalendar);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Task task = (Task) adapterView.getItemAtPosition(position);
        TaskRepository.getInstance(getContext()).deleteTask(task);
        adapter.notifyDataSetChanged();
        return true;
    }

    private boolean isTaskPast(Task task) {
        try {
            long now = System.currentTimeMillis();
            long threeDaysMillis = 3L * 24 * 60 * 60 * 1000;

            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                long dueMillis = TaskRepository.dateFormat.parse(task.getDueDate()).getTime();
                if (now > dueMillis + threeDaysMillis) {
                    task.setStatus("neurađen");
                    TaskRepository.getInstance(getContext()).updateTask(task);
                    return true;
                }
            }

            if (task.getEndDate() != null && !task.getEndDate().isEmpty()) {
                long endMillis = TaskRepository.dateFormat.parse(task.getEndDate()).getTime();
                if (now > endMillis + threeDaysMillis) {
                    task.setStatus("neurađen");
                    TaskRepository.getInstance(getContext()).updateTask(task);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getCategoryColor(String categoryName) {
        // Dohvati sve kategorije iz repository-ja
        List<Category> categories = CategoryRepository.getInstance(getContext())
                .getAllCategories().getValue();

        if (categories != null) {
            for (Category cat : categories) {
                if (cat.getName().equalsIgnoreCase(categoryName)) {
                    return cat.getColor(); // vrati boju kategorije
                }
            }
        }
        return "#9E9E9E"; // default siva boja ako nije pronađena
    }

}
