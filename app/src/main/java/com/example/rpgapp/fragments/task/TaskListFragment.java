package com.example.rpgapp.fragments.task;

import android.os.Bundle;
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
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends ListFragment implements AdapterView.OnItemLongClickListener {

    private TaskAdapter adapter;
    private ImageButton btnCalendar;
    private Spinner spinnerFilter;
    private List<Task> allTasks = new ArrayList<>(); // Čuva originalnu, nefiltriranu listu

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

        // Inicijalizuj prazan adapter
        adapter = new TaskAdapter(getActivity(), new ArrayList<>(), (task, v) -> {
            Bundle bundle = new Bundle();
            bundle.putString("taskId", task.getTaskId());
            Navigation.findNavController(v).navigate(R.id.action_taskList_to_taskPage, bundle);
        });
        setListAdapter(adapter);

        // Pozovi učitavanje podataka
        fillData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Osveži podatke svaki put kad se vratiš na fragment
        fillData();
    }

    private void fillData() {
        TaskRepository repository = TaskRepository.getInstance(getContext());

        // <<< IZMENA: Pozivamo NOVU metodu koja vraća OBRAĐENE podatke >>>
        repository.getProcessedTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            // Logika unutar observe bloka je sada SUPER JEDNOSTAVNA!
            if (tasks != null) {
                this.allTasks = tasks; // Sačuvaj originalnu listu
                applyFilter(spinnerFilter.getSelectedItemPosition()); // Primeni trenutni filter
            }
        });
    }

    private void applyFilter(int filterType) {
        if (adapter == null) return;

        List<Task> filteredList = new ArrayList<>();
        // Filtriranje se sada radi na već obrađenoj listi, što je brzo
        for (Task task : allTasks) {
            switch (filterType) {
                case 0: // Svi
                    filteredList.add(task);
                    break;
                case 1: // Jednokratni
                    if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                        filteredList.add(task);
                    }
                    break;
                case 2: // Ponavljajući
                    if (task.getStartDate() != null && !task.getStartDate().isEmpty()) {
                        filteredList.add(task);
                    }
                    break;
            }
        }
        // Ažuriraj adapter sa filtriranom listom
        adapter.setTasks(filteredList);
    }

    private void openCalendar() {
        Navigation.findNavController(requireView()).navigate(R.id.action_taskList_to_taskCalendar);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Task task = (Task) adapterView.getItemAtPosition(position);
        TaskRepository.getInstance(getContext()).deleteTask(task);
        // Lista će se automatski osvežiti preko LiveData, nema potrebe za notifyDataSetChanged()
        Toast.makeText(getContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
        return true;
    }
}