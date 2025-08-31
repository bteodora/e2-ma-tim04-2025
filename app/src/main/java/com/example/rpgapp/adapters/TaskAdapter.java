package com.example.rpgapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Task;

import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    private Context context;
    private List<Task> tasks;

    public TaskAdapter(@NonNull Context context, @NonNull List<Task> tasks) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        Task currentTask = tasks.get(position);

        TextView taskTitle = listItem.findViewById(R.id.textViewTaskTitle);
        TextView taskCategory = listItem.findViewById(R.id.textViewTaskCategory);
        TextView taskStatus = listItem.findViewById(R.id.textViewTaskStatus);
        Button actionButton = listItem.findViewById(R.id.buttonTaskAction);

        taskTitle.setText(currentTask.getTitle());
        taskCategory.setText(currentTask.getCategory());

        // Postavljanje boje, ako nije null
        if (currentTask.getColor() != null && !currentTask.getColor().isEmpty()) {
            try {
                taskCategory.setBackgroundColor(Color.parseColor(currentTask.getColor()));
            } catch (IllegalArgumentException e) {
                taskCategory.setBackgroundColor(Color.GRAY);
            }
        }

        taskStatus.setText(currentTask.getStatus());

        // Action button (npr. mark done)
        actionButton.setText("Mark Done");
        actionButton.setOnClickListener(v -> {
            currentTask.setStatus("urađen");
            notifyDataSetChanged();
        });

        return listItem;
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Nullable
    @Override
    public Task getItem(int position) {
        return tasks.get(position);
    }
}
