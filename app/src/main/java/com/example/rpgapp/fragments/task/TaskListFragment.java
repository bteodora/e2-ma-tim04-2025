package com.example.rpgapp.fragments.task;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.TaskAdapter;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskListFragment extends ListFragment implements AdapterView.OnItemLongClickListener {

    private TaskAdapter adapter;
    private static final String TAG = "TaskListFragment";
    private ImageButton btnCalendar;

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
                // Jednokratni zadaci
                if (task.getDueDate() != null && parseDateToMillis(task.getDueDate()) >= today) {
                    currentAndFutureTasks.add(task);

                    // Ponavljajući zadaci
                } else if (task.getStartDate() != null && task.getEndDate() != null) {
                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    start.setTime(TaskRepository.dateFormat.parse(task.getStartDate()));
                    end.setTime(TaskRepository.dateFormat.parse(task.getEndDate()));

                    Calendar current = (Calendar) start.clone();
                    while (!current.after(end)) {
                        if (current.getTimeInMillis() >= today) {
                            currentAndFutureTasks.add(task);
                            break; // dodajemo zadatak samo jednom u listu
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

        adapter = new TaskAdapter(getActivity(), currentAndFutureTasks, task -> {
            Log.d(TAG, "Navigating to taskId: " + task.getTaskId());
            Bundle bundle = new Bundle();
            bundle.putString("taskId", task.getTaskId());
            Navigation.findNavController(requireView()).navigate(R.id.action_taskList_to_taskPage, bundle);
        });

        setListAdapter(adapter);
    });
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
}
