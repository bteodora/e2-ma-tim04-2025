package com.example.rpgapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpgapp.R;
import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.fragments.alliance.SpecialMissionViewModel;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.example.rpgapp.model.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    private Context context;
    private List<Task> tasks;

    private OnItemClickListener listener;
    private SpecialMissionViewModel specialMissionViewModel;
    private String currentUserId;

    public interface OnItemClickListener {
        void onItemClick(Task task, View view);
    }

    public TaskAdapter(@NonNull Context context, @NonNull List<Task> tasks, OnItemClickListener listener) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks = new ArrayList<>(tasks);
        this.listener = listener;
        // Firebase trenutni korisnik
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Inicijalizacija ViewModel-a
        specialMissionViewModel = new ViewModelProvider((FragmentActivity) context)
                .get(SpecialMissionViewModel.class);

        // Učitavanje aktivne misije za korisnika
        loadActiveSpecialMission();
    }

    private void loadActiveSpecialMission() {
        SpecialMissionRepository.getInstance(context)
                .getActiveMissionForUser(currentUserId, new SpecialMissionRepository.MissionCallback() {
                    @Override
                    public void onMissionLoaded(SpecialMission mission) {
                        if (mission != null) {
                            specialMissionViewModel.setCurrentMission(mission);
                            ((FragmentActivity) context).runOnUiThread(() -> {
                                Toast.makeText(context,
                                        "Specijalna misija učitana! Zadaci iz misije će biti praćeni.",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        ((FragmentActivity) context).runOnUiThread(() -> {
                            Toast.makeText(context,
                                    "Greška prilikom učitavanja misije: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
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


                //----------Specijalna misija
                completeTaskForSpecialMission(currentTask);

                notifyDataSetChanged();
            });


        }



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


    private void completeTaskForSpecialMission(Task task) {
        SpecialMission activeMission = specialMissionViewModel.getCurrentMission().getValue();
        if (activeMission == null) {
            Toast.makeText(context, "Nema aktivne misije!", Toast.LENGTH_LONG).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Toast.makeText(context, "Task koji se završava: " + task.getTitle(), Toast.LENGTH_LONG).show();

        boolean assignedToCategory = false;

        for (int i = 0; i < activeMission.getTasks().size(); i++) {
            MissionTask missionTask = activeMission.getTasks().get(i);

            // 1️⃣ Laki/Normalni/Važni zadaci
            if ("Laki/Normalni/Važni zadaci".equals(missionTask.getName()) ) {
                boolean isEasyOrNormal = "Veoma lak".equals(task.getDifficultyText()) ||
                        "Lak".equals(task.getDifficultyText()) ||
                        "Normalan".equals(task.getImportanceText()) ||
                        "Važan".equals(task.getImportanceText());

                if (isEasyOrNormal) {
                    int hpToAward = 1;
                    if ("Lak".equals(task.getDifficultyText()) || "Normalan".equals(task.getImportanceText())) {
                        specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                        hpToAward *= 2;
                    }

                    Toast.makeText(context, "HP za Laki/Normalni/Važni: " + hpToAward, Toast.LENGTH_LONG).show();
                    specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                    assignedToCategory = true;
                    break;
                }

                // 2️⃣ Ostali zadaci
            } else if ("Ostali zadaci".equals(missionTask.getName()) ) {
                int hpToAward = 4;
                Toast.makeText(context, "HP za Ostale zadatke: " + hpToAward, Toast.LENGTH_LONG).show();
                specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                assignedToCategory = true;
                break;
            }
        }

        // 3️⃣ Bez nerešenih zadataka – proverava se tek nakon prve dve kategorije

        if (assignedToCategory) {
            for (int i = 0; i < activeMission.getTasks().size(); i++) {
                MissionTask missionTask = activeMission.getTasks().get(i);

                if ("Bez nerešenih zadataka".equals(missionTask.getName())) {
                    boolean allDone = TaskRepository.getInstance(context)
                            .areAllTasksCompletedDuringMission();

                    if (allDone) {
                        int hpToAward = 10;
                        Toast.makeText(context, "HP za Bez nerešenih zadataka: " + hpToAward, Toast.LENGTH_LONG).show();
                        specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                    }
                }
            }
        }


        // Update taska u TaskRepository
        TaskRepository.getInstance(context).updateTaskStatus(task, "urađen", context);
    }

//                case "Bez nerešenih zadataka":
//                    boolean allDone = TaskRepository.getInstance(getContext())
//                            .areAllTasksCompletedDuringMission(); // napravi ovu metodu
//                    if (allDone) hpToAward = 10;
//                    break;

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
