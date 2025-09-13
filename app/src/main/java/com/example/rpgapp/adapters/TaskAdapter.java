package com.example.rpgapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rpgapp.R;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    private Context context;
    private List<Task> tasks;

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Task task, View view);
    }

    public TaskAdapter(@NonNull Context context, @NonNull List<Task> tasks, OnItemClickListener listener) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks = new ArrayList<>(tasks);
        this.listener = listener;
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

        // Postavljanje naslova sa oznakom ako je ponavljajući
        if (currentTask.isRecurring()) {
            taskTitle.setText(currentTask.getTitle() + " (ponavljajući)");
        } else {
            taskTitle.setText(currentTask.getTitle());
        }

        TextView categoryText = listItem.findViewById(R.id.textViewTaskCategory); // ID iz XML-a
        taskCategory.setText(currentTask.getCategory());

        // Postavljanje boje kategorije
        if (currentTask.getColor() != null && !currentTask.getColor().isEmpty()) {
            try {
                categoryText.setBackgroundColor(Color.parseColor(currentTask.getColor()));
            } catch (IllegalArgumentException e) {
                categoryText.setBackgroundColor(Color.GRAY);
            }
        } else {
            categoryText.setBackgroundColor(Color.GRAY);
        }

        taskStatus.setText(currentTask.getStatus());

        // Sakrij dugme ako je task urađen
        if ("urađen".equalsIgnoreCase(currentTask.getStatus())) {
            actionButton.setVisibility(View.GONE);
        } else {
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setText("Mark Done");
            actionButton.setOnClickListener(v -> {

                // Pozovi repozitorijum da ažurira status
                TaskRepository.getInstance(context).updateTaskStatus(currentTask, "urađen", context);

                notifyDataSetChanged();
            });


        }



        // Klik na ceo item
// Klik na ceo item
        listItem.setOnClickListener(v -> {
            if (listener != null) {
                // Toast da proverimo taskId
                String taskId = currentTask.getTaskId();
                if (taskId != null) {
                    Toast.makeText(context, "taskId = " + taskId + " | type = " + taskId.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "taskId je null!", Toast.LENGTH_LONG).show();
                }

                listener.onItemClick(currentTask, v);
            }
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

    public void setTasks(List<Task> newTasks) {
        this.tasks.clear();
        this.tasks.addAll(newTasks);
        notifyDataSetChanged();
    }

}
