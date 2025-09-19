package com.example.rpgapp.fragments.profile;

import android.app.Application;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rpgapp.database.AuthRepository;
import com.example.rpgapp.database.SpecialMissionRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.database.UserRepository;
import com.example.rpgapp.model.SpecialMission;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private SpecialMissionRepository specialMissionRepository;
    private UserRepository userRepository;

    public MutableLiveData<String> activeDaysStreak = new MutableLiveData<>("0");
    public MutableLiveData<String> longestSuccessStreak = new MutableLiveData<>("0");
    public MutableLiveData<TaskSummary> taskSummaryData = new MutableLiveData<>();
    public MutableLiveData<BarData> categoryData = new MutableLiveData<>();
    public MutableLiveData<ArrayList<String>> categoryLabels = new MutableLiveData<>();
    private MutableLiveData<ChartDataWithLabels> _xpLast7DaysData = new MutableLiveData<>();
    public LiveData<ChartDataWithLabels> xpLast7DaysData = _xpLast7DaysData;
    private MutableLiveData<ChartDataWithLabels> _avgDifficultyData = new MutableLiveData<>();
    public LiveData<ChartDataWithLabels> avgDifficultyData = _avgDifficultyData;
    private MutableLiveData<String> _missionsStartedCount = new MutableLiveData<>("0");
    public LiveData<String> missionsStartedCount = _missionsStartedCount;
    private MutableLiveData<String> _missionsCompletedCount = new MutableLiveData<>("0");
    public LiveData<String> missionsCompletedCount = _missionsCompletedCount;
    public MutableLiveData<String> userTitle = new MutableLiveData<>();
    public MutableLiveData<String> userPowerPoints = new MutableLiveData<>();
    public MutableLiveData<String> userXpDisplay = new MutableLiveData<>();
    public MutableLiveData<Integer> userXpProgress = new MutableLiveData<>();
    public MutableLiveData<Integer> userXpMax = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        authRepository = new AuthRepository(application);
        specialMissionRepository = SpecialMissionRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);

        taskRepository.getAllTasksLiveData().observeForever(this::processAllTasks);
        userRepository.getLoggedInUserLiveData().observeForever(user -> {
            if (user != null) {
                loadMissionStats(user.getUserId());

                userTitle.postValue(user.getTitle());
                userPowerPoints.postValue(String.valueOf(user.getPowerPoints()));

                long currentXp = user.getXp();
                long requiredXp = user.getRequiredXpForNextLevel();

                userXpDisplay.postValue(currentXp + " / " + requiredXp + " XP");

                userXpMax.postValue((int) requiredXp);
                userXpProgress.postValue((int) currentXp);

            } else {
                userTitle.postValue("");
                userPowerPoints.postValue("0");
                userXpDisplay.postValue("0 / 0 XP");
                userXpProgress.postValue(0);
            }
        });
    }

    private void processAllTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        tasks.sort(Comparator.comparingLong(Task::getCompletionTimestamp));

        calculateStreaks(tasks);
        createTaskSummaryChart(tasks);
        createCategoryChart(tasks);
        createXpLast7DaysChart(tasks);
        calculateAverageDifficultyForLast7Days(tasks);
        calculateActiveDaysStreak(tasks);
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

    private void calculateStreaks(List<Task> allTasks) {
        // activeDaysStreak.postValue("0");

        Set<Long> successfulDays = new HashSet<>();
        Set<Long> failedDays = new HashSet<>();

        for (Task task : allTasks) {
            if (task.getCompletionTimestamp() > 0 && task.getStatus() != null) {
                long dayTimestamp = task.getCompletionTimestamp();
                if ("urađen".equalsIgnoreCase(task.getStatus())) {
                    successfulDays.add(dayTimestamp);
                } else if ("neurađen".equalsIgnoreCase(task.getStatus())) {
                    failedDays.add(dayTimestamp);
                }
            }
        }

        successfulDays.removeAll(failedDays);

        if (successfulDays.isEmpty()) {
            longestSuccessStreak.postValue("0");
            return;
        }

        List<Long> sortedSuccessfulDays = new ArrayList<>(successfulDays);
        Collections.sort(sortedSuccessfulDays);

        int maxStreak = 1;
        int currentStreak = 1;
        for (int i = 1; i < sortedSuccessfulDays.size(); i++) {
            long previousSuccessDay = sortedSuccessfulDays.get(i - 1);
            long currentSuccessDay = sortedSuccessfulDays.get(i);

            boolean streakBroken = false;
            for (Long failedDay : failedDays) {
                if (failedDay > previousSuccessDay && failedDay < currentSuccessDay) {
                    streakBroken = true;
                    break;
                }
            }

            if (streakBroken) {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            } else {
                currentStreak++;
            }
        }

        maxStreak = Math.max(maxStreak, currentStreak);
        longestSuccessStreak.postValue(String.valueOf(maxStreak));
    }

    private void calculateActiveDaysStreak(List<Task> allTasks) {
        if (allTasks == null || allTasks.isEmpty()) {
            activeDaysStreak.postValue("0");
            return;
        }

        Set<LocalDate> activeDays = new HashSet<>();
        for (Task task : allTasks) {
            if (task.getCreationTimestamp() > 0) {
                LocalDate creationDate = Instant.ofEpochMilli(task.getCreationTimestamp())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                activeDays.add(creationDate);
            }

            if (task.getLastActionTimestamp() > 0) {
                LocalDate actionDate = Instant.ofEpochMilli(task.getLastActionTimestamp())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                activeDays.add(actionDate);
            }
        }

        if (activeDays.isEmpty()) {
            activeDaysStreak.postValue("0");
            return;
        }

        List<LocalDate> sortedActiveDays = new ArrayList<>(activeDays);
        Collections.sort(sortedActiveDays);

        int currentStreak = 0;
        LocalDate today = LocalDate.now();
        LocalDate lastDayInList = sortedActiveDays.get(sortedActiveDays.size() - 1);

        if (lastDayInList.isEqual(today) || lastDayInList.isEqual(today.minusDays(1))) {
            currentStreak = 1;
            for (int i = sortedActiveDays.size() - 2; i >= 0; i--) {
                LocalDate currentDay = sortedActiveDays.get(i + 1);
                LocalDate previousDay = sortedActiveDays.get(i);

                if (previousDay.isEqual(currentDay.minusDays(1))) {
                    currentStreak++;
                } else {
                    break;
                }
            }
        }

        activeDaysStreak.postValue(String.valueOf(currentStreak));
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

        if (entries.isEmpty()) return;

        BarDataSet dataSet = new BarDataSet(entries, "Kategorije");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
        categoryLabels.postValue(labels);
        categoryData.postValue(new BarData(dataSet));
    }

    private void createXpLast7DaysChart(List<Task> tasks) {
        long now = System.currentTimeMillis();
        Map<Integer, Integer> dailyXp = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyXp.put(i, 0);
        }

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
        ArrayList<String> labels = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, dailyXp.get(i)));
            LocalDate day = today.minusDays(6 - i);
            labels.add(day.format(formatter));
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP (poslednjih 7 dana)");
        LineData lineData = new LineData(dataSet);
        ChartDataWithLabels finalData = new ChartDataWithLabels(lineData, labels);
        _xpLast7DaysData.postValue(finalData);
    }

    private void calculateAverageDifficultyForLast7Days(List<Task> allTasks) {
        List<Task> completedTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if ("urađen".equalsIgnoreCase(task.getStatus())) {
                completedTasks.add(task);
            }
        }

        Map<LocalDate, List<Integer>> xpPerDay = new HashMap<>();
        for (Task task : completedTasks) {
            LocalDate date = Instant.ofEpochMilli(task.getCompletionTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            xpPerDay.computeIfAbsent(date, k -> new ArrayList<>()).add(task.getDifficultyXp());
        }

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            float avgXp = 0f;
            if (xpPerDay.containsKey(day)) {
                List<Integer> dailyXps = xpPerDay.get(day);
                if (dailyXps != null && !dailyXps.isEmpty()) {
                    double sum = 0;
                    for (int xp : dailyXps) sum += xp;
                    avgXp = (float) (sum / dailyXps.size());
                }
            }
            entries.add(new Entry(6 - i, avgXp));
            labels.add(day.format(formatter));
        }

        if (entries.isEmpty()) {
            _avgDifficultyData.postValue(null);
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Average task difficulty");
        LineData lineData = new LineData(dataSet);
        ChartDataWithLabels finalData = new ChartDataWithLabels(lineData, labels);
        _avgDifficultyData.postValue(finalData);
    }

    public class ChartDataWithLabels {
        public final LineData lineData;
        public final List<String> labels;

        public ChartDataWithLabels(LineData lineData, List<String> labels) {
            this.lineData = lineData;
            this.labels = labels;
        }
    }

    private void loadMissionStats(String currentUserId) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        specialMissionRepository.getAllMissions().observeForever(allMissions -> {
            if (allMissions == null) {
                return;
            }


            int startedCount = 0;
            int completedCount = 0;

            for (SpecialMission mission : allMissions) {
                Map<String, Integer> progressMap = mission.getUserTaskProgress();

                if (progressMap.containsKey(currentUserId.trim())) {
                    startedCount++;
                    if (!mission.isActive()) {
                        completedCount++;
                    }
                }
            }

            _missionsStartedCount.postValue(String.valueOf(startedCount));
            _missionsCompletedCount.postValue(String.valueOf(completedCount));
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
