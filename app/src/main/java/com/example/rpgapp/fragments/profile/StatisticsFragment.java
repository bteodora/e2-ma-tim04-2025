package com.example.rpgapp.fragments.profile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class StatisticsFragment extends Fragment {

    private StatisticsViewModel viewModel;
    private TextView textViewActiveDays, textViewLongestStreak;
    private PieChart chartTaskSummary;
    private BarChart chartCategory;
    private LineChart chartXpLast7Days;
    private LineChart chartAvgDifficulty;

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
    }

    private void observeViewModel() {
        viewModel.activeDaysStreak.observe(getViewLifecycleOwner(), s -> textViewActiveDays.setText(s));
        viewModel.longestSuccessStreak.observe(getViewLifecycleOwner(), s -> textViewLongestStreak.setText(s));

        viewModel.taskSummaryData.observe(getViewLifecycleOwner(), taskSummary -> {
            if (taskSummary != null) {
                setupPieChart(taskSummary);
            }
        });

        viewModel.categoryData.observe(getViewLifecycleOwner(), barData -> {
            // Dodajemo proveru i ovde, za svaki slučaj
            if (barData != null) {
                setupBarChart(barData);
            }
        });

        viewModel.xpLast7DaysData.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                setupLineChart(chartXpLast7Days, data, "XP Gained");
            }
        });

        viewModel.avgDifficultyData.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                setupLineChart(chartAvgDifficulty, data, "Avg. Difficulty");
            }
        });

        viewModel.categoryLabels.observe(getViewLifecycleOwner(), labels -> {
            if (chartCategory.getData() != null && labels != null) {
                XAxis xAxis = chartCategory.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                chartCategory.invalidate();
            }
        });
    }

    private void setupPieChart(TaskSummary summary) {
        if (summary == null || summary.pieData == null) {
            chartTaskSummary.clear();
            chartTaskSummary.invalidate();
            return;
        }

        PieData data = summary.pieData;
        String totalTasks = String.valueOf(summary.totalTasks);

        chartTaskSummary.setCenterText("Total\n" + totalTasks);
        chartTaskSummary.setCenterTextSize(20f);
        chartTaskSummary.setCenterTextColor(Color.WHITE);

        chartTaskSummary.getDescription().setEnabled(false);
        chartTaskSummary.setDrawHoleEnabled(true);
        chartTaskSummary.setHoleColor(Color.TRANSPARENT);
        chartTaskSummary.getLegend().setEnabled(true);
        chartTaskSummary.getLegend().setTextColor(Color.WHITE);
        chartTaskSummary.getLegend().setTextSize(12f);

        data.setValueTextSize(12f);
        data.setDrawValues(true);

        chartTaskSummary.setData(data);
        chartTaskSummary.animateY(1000);
        chartTaskSummary.invalidate();
    }

    private void setupLineChart(LineChart chart, LineData data, String label) {
        if (data == null) {
            chart.clear();
            chart.invalidate();
            return;
        }

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisLeft().setDrawGridLines(true);

        if (data.getDataSetCount() > 0) {
            LineDataSet dataSet = (LineDataSet) data.getDataSetByIndex(0);
            dataSet.setLineWidth(2.5f);
            dataSet.setColor(ColorTemplate.getHoloBlue());
            dataSet.setCircleColor(Color.WHITE);
            dataSet.setDrawValues(false);
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

        XAxis xAxis = chartCategory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);

        ArrayList<String> labels = viewModel.categoryLabels.getValue();
        if (labels != null) {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        }

        chartCategory.getAxisLeft().setTextColor(Color.WHITE);
        chartCategory.getAxisRight().setEnabled(false);
        chartCategory.getDescription().setEnabled(false);
        chartCategory.getLegend().setEnabled(false);

        chartCategory.setData(data);
        chartCategory.invalidate();
    }

}