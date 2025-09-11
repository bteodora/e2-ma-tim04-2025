package com.example.rpgapp.fragments.profile;

import android.app.Application;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.database.AuthRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;
import com.example.rpgapp.model.TaskSummary;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class StatisticsViewModel extends AndroidViewModel {

    private TaskRepository taskRepository;
    private AuthRepository authRepository;

    public MutableLiveData<String> activeDaysStreak = new MutableLiveData<>("0");
    public MutableLiveData<String> longestSuccessStreak = new MutableLiveData<>("0");
    public MutableLiveData<TaskSummary> taskSummaryData = new MutableLiveData<>();
    public MutableLiveData<BarData> categoryData = new MutableLiveData<>();
    public MutableLiveData<ArrayList<String>> categoryLabels = new MutableLiveData<>();
    public MutableLiveData<LineData> avgDifficultyData = new MutableLiveData<>();
    public MutableLiveData<LineData> xpLast7DaysData = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        authRepository = new AuthRepository(application);

        taskRepository.getAllTasksLiveData().observeForever(this::processAllTasks);
    }

    private void processAllTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        tasks.sort(Comparator.comparingLong(Task::getCompletionTimestamp));

        calculateStreaks(tasks);
        createTaskSummaryChart(tasks);
        createCategoryChart(tasks);
        createXpLast7DaysChart(tasks);
        createAvgDifficultyChart(tasks);
    }


    private void createTaskSummaryChart(List<Task> allTasks) {
        int totalCreated = allTasks.size();
        int urađen = 0, neurađen = 0, otkazan = 0, aktivan = 0;

        for (Task task : allTasks) {
            if (task.getStatus() == null) continue;
            switch (task.getStatus().toLowerCase()) {
                case "urađen":
                    urađen++;
                    break;
                case "neurađen":
                    neurađen++;
                    break;
                case "otkazan":
                    otkazan++;
                    break;
                case "aktivan":
                    aktivan++;
                    break;
            }
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (urađen > 0) entries.add(new PieEntry(urađen, "Urađeni"));
        if (neurađen > 0) entries.add(new PieEntry(neurađen, "Neurađeni"));
        if (otkazan > 0) entries.add(new PieEntry(otkazan, "Otkazani"));
        if (aktivan > 0) entries.add(new PieEntry(aktivan, "Aktivni"));

        PieData pieData = null;
        if (!entries.isEmpty()) {
            PieDataSet dataSet = new PieDataSet(entries, "Pregled Zadataka");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(12f);
            dataSet.setSliceSpace(2f);
            pieData = new PieData(dataSet);
        }
        taskSummaryData.postValue(new TaskSummary(pieData, totalCreated));
    }

    // U StatisticsViewModel.java

    // U StatisticsViewModel.java

    private void calculateStreaks(List<Task> allTasks) {
        // =====================================================================
        // PRAVILO 1: Broj dana aktivnog korišćenja aplikacije
        // =====================================================================
        activeDaysStreak.postValue("0"); // Placeholder, zahteva posebno praćenje

        // =====================================================================
        // PRAVILO 2: Najduži niz uspešno urađenih zadataka
        // =====================================================================

        // 1. Kreiramo dva seta: jedan za uspešne, jedan za neuspešne dane
        Set<Long> successfulDays = new HashSet<>();
        Set<Long> failedDays = new HashSet<>();

        for (Task task : allTasks) {
            if (task.getCompletionTimestamp() > 0 && task.getStatus() != null) {
                long dayTimestamp = task.getCompletionTimestamp(); // Već je normalizovan

                if ("urađen".equalsIgnoreCase(task.getStatus())) {
                    successfulDays.add(dayTimestamp);
                } else if ("neurađen".equalsIgnoreCase(task.getStatus())) {
                    failedDays.add(dayTimestamp);
                }
            }
        }

        // Uklonimo sve dane koji su bili i uspešni i neuspešni (ako npr. jedan task uradi, a drugi ne)
        // iz seta uspešnih. Takav dan je neuspešan.
        successfulDays.removeAll(failedDays);

        if (successfulDays.isEmpty()) {
            longestSuccessStreak.postValue("0");
            return;
        }

        // 2. Sortiramo samo uspešne dane
        List<Long> sortedSuccessfulDays = new ArrayList<>(successfulDays);
        Collections.sort(sortedSuccessfulDays);

        // 3. Tražimo najduži niz
        int maxStreak = 1;
        int currentStreak = 1;
        for (int i = 1; i < sortedSuccessfulDays.size(); i++) {
            long previousSuccessDay = sortedSuccessfulDays.get(i - 1);
            long currentSuccessDay = sortedSuccessfulDays.get(i);

            // 4. Proveravamo da li postoji dan neuspeha IZMEĐU dva uspešna dana
            boolean streakBroken = false;
            for (Long failedDay : failedDays) {
                if (failedDay > previousSuccessDay && failedDay < currentSuccessDay) {
                    streakBroken = true;
                    break; // Našli smo neuspeh, niz je prekinut
                }
            }

            if (streakBroken) {
                // Ako je niz prekinut, sačuvaj dosadašnji i resetuj
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            } else {
                // Ako niz NIJE prekinut, samo ga nastavi
                currentStreak++;
            }
        }

        // Finalna provera za poslednji niz
        maxStreak = Math.max(maxStreak, currentStreak);
        longestSuccessStreak.postValue(String.valueOf(maxStreak));
    }

    private void createCategoryChart(List<Task> tasks) {
        Map<String, Integer> categoryCount = new HashMap<>();
        for (Task task : tasks) {
            if ("urađen".equalsIgnoreCase(task.getStatus())) {
                String category = task.getCategory();
                if (category != null && !category.isEmpty()) {
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                }
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>(categoryCount.keySet());
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new BarEntry(i, categoryCount.get(labels.get(i))));
        }

        if(entries.isEmpty()) return;

        BarDataSet dataSet = new BarDataSet(entries, "Kategorije");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
        categoryLabels.postValue(labels);
        categoryData.postValue(new BarData(dataSet));
    }

    private void createXpLast7DaysChart(List<Task> tasks) {
        long now = System.currentTimeMillis();
        Map<Integer, Integer> dailyXp = new HashMap<>();
        for (int i = 0; i < 7; i++) dailyXp.put(i, 0);

        for (Task task : tasks) {
            if ("urađen".equalsIgnoreCase(task.getStatus())) {
                long diff = now - task.getCompletionTimestamp();
                if (diff >= 0) {
                    long daysAgo = TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysAgo < 7) {
                        int dayIndex = 6 - (int) daysAgo;
                        dailyXp.put(dayIndex, dailyXp.get(dayIndex) + task.getTotalXp());
                    }
                }
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, dailyXp.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP (poslednjih 7 dana)");
        xpLast7DaysData.postValue(new LineData(dataSet));
    }

    private void createAvgDifficultyChart(List<Task> tasks) {
        Map<Long, List<Integer>> xpPerDay = new HashMap<>();
        for(Task task : tasks){
            if("urađen".equalsIgnoreCase(task.getStatus())){
                long dayTimestamp = task.getCompletionTimestamp(); // Već je normalizovan u get-eru
                if(!xpPerDay.containsKey(dayTimestamp)) {
                    xpPerDay.put(dayTimestamp, new ArrayList<>());
                }
                xpPerDay.get(dayTimestamp).add(task.getTotalXp());
            }
        }

        List<Long> sortedDays = new ArrayList<>(xpPerDay.keySet());
        Collections.sort(sortedDays);

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < sortedDays.size(); i++) {
            List<Integer> dailyXps = xpPerDay.get(sortedDays.get(i));
            double sum = 0;
            for(int xp : dailyXps) sum += xp;
            float avg = (float) (sum / dailyXps.size());
            entries.add(new Entry(i, avg));
        }

        if(entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, "Prosečna težina zadatka");
        avgDifficultyData.postValue(new LineData(dataSet));
    }
}