package com.example.rpgapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.MissionTask;

import java.util.ArrayList;
import java.util.List;

public class MissionTaskAdapter extends RecyclerView.Adapter<MissionTaskAdapter.TaskViewHolder> {

    private List<MissionTask> tasks = new ArrayList<>();
    private final TaskClickListener listener;
    private String userId; // ID korisnika čiji progres pratimo

    public interface TaskClickListener {
        void onTaskCompleted(int position);
    }

    public MissionTaskAdapter(TaskClickListener listener, String userId) {
        this.listener = listener;
        this.userId = userId;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_mission_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        MissionTask task = tasks.get(position);

        holder.textViewTaskName.setText(task.getName());

        holder.progressBarTask.setMax(task.getMaxCompletions());
        holder.progressBarTask.setProgress(task.getCurrentCompletions(userId));

        // Prikaz ukupnog i današnjeg napretka
        String progressText = task.getCurrentCompletions(userId) + "/" + task.getMaxCompletions()
                + " (danas " + task.getTodayProgress(userId) + "/" + task.getDailyMax() + ")";
        holder.textViewProgress.setText(progressText);

        if (task.isCompleted(userId) || task.getTodayProgress(userId) >= task.getDailyMax()) {
            holder.buttonComplete.setEnabled(false);
            holder.buttonComplete.setText("Completed");
        } else {
            holder.buttonComplete.setEnabled(true);
            holder.buttonComplete.setText("Complete");
        }

        holder.buttonComplete.setOnClickListener(v -> {
            boolean incremented = task.incrementProgress(userId);
            if (incremented) {
                listener.onTaskCompleted(position);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void setTasks(List<MissionTask> taskList) {
        if (taskList != null) {
            tasks = taskList;
            notifyDataSetChanged();
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTaskName, textViewProgress;
        ProgressBar progressBarTask;
        Button buttonComplete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTaskName = itemView.findViewById(R.id.textViewTaskName);
            textViewProgress = itemView.findViewById(R.id.textViewTaskProgress);
            progressBarTask = itemView.findViewById(R.id.progressBarTask);
            buttonComplete = itemView.findViewById(R.id.buttonCompleteTask);
        }
    }
}
