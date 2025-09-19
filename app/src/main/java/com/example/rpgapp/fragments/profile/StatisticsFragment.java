package com.example.rpgapp.fragments.profile;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.rpgapp.R;
import com.example.rpgapp.model.TaskSummary;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class StatisticsFragment extends Fragment {

    private StatisticsViewModel viewModel;
    private TextView textViewActiveDays, textViewLongestStreak;
    private PieChart chartTaskSummary;
    private BarChart chartCategory;
    private LineChart chartXpLast7Days;
    private LineChart chartAvgDifficulty;
    private TextView textViewMissionsStarted, textViewMissionsCompleted;
    private TextView textViewUserTitle, textViewUserPowerPoints, textViewUserXp;
    private ProgressBar progressBarUserXp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        bindViews(view);
        observeViewModel();
    }

    private void bindViews(View view) {
        textViewActiveDays = view.findViewById(R.id.textViewActiveDays);
        textViewLongestStreak = view.findViewById(R.id.textViewLongestStreak);
        chartTaskSummary = view.findViewById(R.id.chartTaskSummary);
        chartCategory = view.findViewById(R.id.chartCategory);
        chartXpLast7Days = view.findViewById(R.id.chartXpLast7Days);
        chartAvgDifficulty = view.findViewById(R.id.chartAvgDifficulty);
        textViewMissionsStarted = view.findViewById(R.id.textViewMissionsStarted);
        textViewMissionsCompleted = view.findViewById(R.id.textViewMissionsCompleted);
        textViewUserTitle = view.findViewById(R.id.textViewUserTitle);
        textViewUserPowerPoints = view.findViewById(R.id.textViewUserPowerPoints);
        textViewUserXp = view.findViewById(R.id.textViewUserXp);
        progressBarUserXp = view.findViewById(R.id.progressBarUserXp);
    }

    private void observeViewModel() {
        viewModel.activeDaysStreak.observe(getViewLifecycleOwner(), s -> textViewActiveDays.setText(s));
        viewModel.longestSuccessStreak.observe(getViewLifecycleOwner(), s -> textViewLongestStreak.setText(s));

        viewModel.taskSummaryData.observe(getViewLifecycleOwner(), this::setupPieChart);
        viewModel.categoryData.observe(getViewLifecycleOwner(), this::setupBarChart);
        viewModel.userTitle.observe(getViewLifecycleOwner(), title -> {
            textViewUserTitle.setText(title);
        });

        viewModel.userPowerPoints.observe(getViewLifecycleOwner(), pp -> {
            textViewUserPowerPoints.setText(pp);
        });

        viewModel.userXpDisplay.observe(getViewLifecycleOwner(), xpText -> {
            textViewUserXp.setText(xpText);
        });
        viewModel.userXpMax.observe(getViewLifecycleOwner(), max -> {
            progressBarUserXp.setMax(max);
        });
        viewModel.userXpProgress.observe(getViewLifecycleOwner(), progress -> {
            progressBarUserXp.setProgress(progress);
        });
        viewModel.xpLast7DaysData.observe(getViewLifecycleOwner(), dataWithLabels -> {
            if (dataWithLabels != null && dataWithLabels.lineData != null) {
                setupLineChart(chartXpLast7Days, dataWithLabels.lineData);

                XAxis xAxis = chartXpLast7Days.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(dataWithLabels.labels));
                xAxis.setLabelCount(dataWithLabels.labels.size());
                xAxis.setGranularity(1f);

                chartXpLast7Days.invalidate();
            } else {
                chartXpLast7Days.clear();
                chartXpLast7Days.invalidate();
            }
        });

        viewModel.avgDifficultyData.observe(getViewLifecycleOwner(), dataWithLabels -> {
            if (dataWithLabels != null && dataWithLabels.lineData != null) {
                setupLineChart(chartAvgDifficulty, dataWithLabels.lineData);

                XAxis xAxis = chartAvgDifficulty.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(dataWithLabels.labels));
                xAxis.setLabelCount(dataWithLabels.labels.size());
                xAxis.setGranularity(1f);

                chartAvgDifficulty.invalidate();
            } else {
                chartAvgDifficulty.clear();
                chartAvgDifficulty.invalidate();
            }
        });

        viewModel.categoryLabels.observe(getViewLifecycleOwner(), labels -> {
            if (chartCategory.getData() != null && labels != null) {
                XAxis xAxis = chartCategory.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                chartCategory.invalidate();
            }
        });

        viewModel.missionsStartedCount.observe(getViewLifecycleOwner(), count -> {
            textViewMissionsStarted.setText(count);
        });

        viewModel.missionsCompletedCount.observe(getViewLifecycleOwner(), count -> {
            textViewMissionsCompleted.setText(count);
        });
    }

    private void setupPieChart(TaskSummary summary) {
        if (summary == null || summary.pieData == null) {
            chartTaskSummary.clear();
            chartTaskSummary.invalidate();
            return;
        }

        PieData data = summary.pieData;
        int accentColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);

        chartTaskSummary.setCenterText("Total\n" + summary.totalTasks);
        chartTaskSummary.setCenterTextSize(20f);
        chartTaskSummary.setCenterTextColor(accentColor);

        chartTaskSummary.getDescription().setEnabled(false);
        chartTaskSummary.setDrawHoleEnabled(true);
        chartTaskSummary.setHoleColor(Color.TRANSPARENT);
        chartTaskSummary.getLegend().setTextColor(accentColor);
        chartTaskSummary.getLegend().setTextSize(12f);

        data.setValueTextSize(12f);
        data.setValueTextColor(accentColor);

        // ((PieDataSet)data.getDataSet()).setColors(ColorTemplate.MATERIAL_COLORS);

        chartTaskSummary.setData(data);
        chartTaskSummary.animateY(1000);
        chartTaskSummary.invalidate();
    }

    private void setupLineChart(LineChart chart, LineData data) {
        if (data == null) {
            chart.clear();
            chart.invalidate();
            return;
        }

        int accentColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(accentColor);
        xAxis.setDrawGridLines(false);

        chart.getAxisLeft().setTextColor(accentColor);
        chart.getAxisLeft().setDrawGridLines(true);

        if (data.getDataSetCount() > 0) {
            LineDataSet dataSet = (LineDataSet) data.getDataSetByIndex(0);
            dataSet.setLineWidth(2.5f);
            dataSet.setColor(accentColor);
            dataSet.setCircleColor(accentColor);
            dataSet.setDrawValues(false);

            dataSet.setDrawFilled(true);
            dataSet.setFillColor(accentColor);
            dataSet.setFillAlpha(100);
        }

        chart.setData(data);
        chart.animateX(1000);
        chart.invalidate();
    }

    private void setupBarChart(BarData data) {
        if (data == null) {
            chartCategory.clear();
            chartCategory.invalidate();
            return;
        }
        int accentColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);

        XAxis xAxis = chartCategory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(accentColor);

        ArrayList<String> labels = viewModel.categoryLabels.getValue();
        if (labels != null) {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        }

        chartCategory.getAxisLeft().setTextColor(accentColor);
        chartCategory.getAxisRight().setEnabled(false);
        chartCategory.getDescription().setEnabled(false);
        chartCategory.getLegend().setEnabled(false);

        if (data.getDataSetCount() > 0) {
            data.setValueTextColor(accentColor);
        }

        chartCategory.setData(data);
        chartCategory.invalidate();
    }

}