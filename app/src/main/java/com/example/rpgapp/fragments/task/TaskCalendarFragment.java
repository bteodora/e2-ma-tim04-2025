package com.example.rpgapp.fragments.task;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.TaskAdapter;
import com.example.rpgapp.database.CategoryRepository;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskCalendarFragment extends Fragment {

    private CalendarView calendarView;
    private ListView listView;
    private TaskAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout layoutLegend;
    private List<Category> categories;

    // Na vrhu klase, dodaj mapu boja

    private String currentUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    private final Map<String, String> colorNames = new HashMap<String, String>() {{
        put("#F44336", "Crvena");
        put("#E91E63", "Roze");
        put("#9C27B0", "Ljubičasta");
        put("#3F51B5", "Indigo");
        put("#2196F3", "Plava");
        put("#4CAF50", "Zelena");
        put("#FF9800", "Narandžasta");
        put("#FFEB3B", "Žuta");
    }};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        currentUserId = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .getString("userId", null);

        View view = inflater.inflate(R.layout.fragment_task_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        listView = view.findViewById(R.id.listViewTasks);
        layoutLegend = view.findViewById(R.id.layoutLegend);

        loadTasks();
        loadLegend(); // prikaži legendu odmah

        // Klik na datum u kalendaru
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clickedDay = eventDay.getCalendar();
            filterTasksByDate(clickedDay);
        });

        return view;
    }

    private void loadTasks() {
        TaskRepository repository = TaskRepository.getInstance(getContext());

        repository.getAllTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks == null) tasks = new ArrayList<>();
            allTasks = tasks;

            final List<Task> tasksCopy = new ArrayList<>(tasks);

            executor.execute(() -> {
                List<EventDay> events = new ArrayList<>();

                for (Task task : tasksCopy) {
                    try {
                        String colorStr = getCategoryColor(task.getCategory());
                        int colorInt = Color.parseColor(colorStr);
                        ColorDrawable drawable = new ColorDrawable(colorInt);

                        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(TaskRepository.dateFormat.parse(task.getDueDate()));
                            events.add(new EventDay(cal, drawable));

                        } else if (task.getStartDate() != null && !task.getStartDate().isEmpty()
                                && task.getEndDate() != null && !task.getEndDate().isEmpty()) {

                            Calendar start = Calendar.getInstance();
                            Calendar end = Calendar.getInstance();
                            start.setTime(TaskRepository.dateFormat.parse(task.getStartDate()));
                            end.setTime(TaskRepository.dateFormat.parse(task.getEndDate()));

                            int interval = task.getInterval();
                            String unit = task.getIntervalUnit();
                            Calendar current = (Calendar) start.clone();

                            while (!current.after(end)) {
                                events.add(new EventDay((Calendar) current.clone(), new ColorDrawable(colorInt)));

                                if ("dan".equalsIgnoreCase(unit)) {
                                    current.add(Calendar.DAY_OF_MONTH, interval);
                                } else if ("nedelja".equalsIgnoreCase(unit)) {
                                    current.add(Calendar.WEEK_OF_YEAR, interval);
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> calendarView.setEvents(events));
                }
            });
        });
    }

    private void filterTasksByDate(Calendar clickedDate) {
        List<Task> filtered = new ArrayList<>();

        executor.execute(() -> {
            for (Task task : allTasks) {
                try {
                    if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(TaskRepository.dateFormat.parse(task.getDueDate()));

                        if (isSameDay(cal, clickedDate)) {
                            filtered.add(task);
                        }

                    } else if (task.getStartDate() != null && !task.getEndDate().isEmpty()) {
                        Calendar start = Calendar.getInstance();
                        Calendar end = Calendar.getInstance();
                        start.setTime(TaskRepository.dateFormat.parse(task.getStartDate()));
                        end.setTime(TaskRepository.dateFormat.parse(task.getEndDate()));

                        int interval = task.getInterval();
                        String unit = task.getIntervalUnit();
                        Calendar current = (Calendar) start.clone();

                        while (!current.after(end)) {
                            if (isSameDay(current, clickedDate)) {
                                filtered.add(task);
                                break;
                            }

                            if ("dan".equalsIgnoreCase(unit)) {
                                current.add(Calendar.DAY_OF_MONTH, interval);
                            } else if ("nedelja".equalsIgnoreCase(unit)) {
                                current.add(Calendar.WEEK_OF_YEAR, interval);
                            }
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new TaskAdapter(getContext(), filtered, new TaskAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(Task task, View view) {
                            Bundle bundle = new Bundle();
                            bundle.putString("taskId", task.getTaskId());
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.action_taskCalendar_to_taskPage, bundle);
                        }
                    });
                    listView.setAdapter(adapter);
                });
            }

        });
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private String getCategoryColor(String categoryName) {
        categories = CategoryRepository.getInstance(getContext()).getAllCategories().getValue();
        if (categories != null) {
            for (Category cat : categories) {
                if (cat.getName().equalsIgnoreCase(categoryName)) {
                    return cat.getColor();
                }
            }
        }
        return "#9E9E9E";
    }


    private void loadLegend() {
        // Observer se registruje jednom
        CategoryRepository.getInstance(getContext()).getAllCategories()
                .observe(getViewLifecycleOwner(), cats -> {
                    categories = cats != null ? cats : new ArrayList<>();

                    layoutLegend.removeAllViews(); // briše sve dugmiće pre dodavanja

                    Set<String> addedCategoryNames = new HashSet<>();

                    for (Category category : categories) {
                        // dodaj samo ako još nije dodat
                        if (!addedCategoryNames.contains(category.getName())) {
                            Button btn = new Button(requireContext());
                            btn.setText(category.getName());
                            btn.setBackgroundColor(Color.parseColor(category.getColor()));
                            btn.setTextColor(Color.WHITE);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(8, 0, 8, 0);
                            btn.setLayoutParams(params);
                            btn.setOnClickListener(v -> showEditCategoryDialog(category));

                            layoutLegend.addView(btn);
                            addedCategoryNames.add(category.getName());
                        }
                    }

                    // Dugme za dodavanje nove kategorije
                    Button btnAdd = new Button(requireContext());
                    btnAdd.setText("+");
                    btnAdd.setBackgroundColor(Color.DKGRAY);
                    btnAdd.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams paramsAdd = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    paramsAdd.setMargins(8, 0, 8, 0);
                    btnAdd.setLayoutParams(paramsAdd);
                    btnAdd.setOnClickListener(v -> showAddCategoryDialog());
                    layoutLegend.addView(btnAdd);
                });
    }



    private void showEditCategoryDialog(Category category) {
        // Svi dostupni heks kodovi
        String[] colors = {"#F44336","#E91E63","#9C27B0","#3F51B5","#2196F3","#4CAF50","#FF9800","#FFEB3B"};

        // Mapa hex → ime boje
        Map<String, String> colorNames = new HashMap<>();
        colorNames.put("#F44336", "Crvena");
        colorNames.put("#E91E63", "Roze");
        colorNames.put("#9C27B0", "Ljubičasta");
        colorNames.put("#3F51B5", "Indigo");
        colorNames.put("#2196F3", "Plava");
        colorNames.put("#4CAF50", "Zelena");
        colorNames.put("#FF9800", "Narandžasta");
        colorNames.put("#FFEB3B", "Žuta");

        // filtriranje već zauzetih boja
        List<String> availableColors = new ArrayList<>();
        List<String> availableColorNames = new ArrayList<>();
        for (String color : colors) {
            boolean exists = false;
            for (Category c : categories) {
                if (c.getUserId().equals(currentUserId) && c.getColor().equalsIgnoreCase(color)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                availableColors.add(color);
                availableColorNames.add(colorNames.get(color)); // dodaj ime boje
            }
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16,16,16,16);

        EditText editName = new EditText(requireContext());
        editName.setHint("Naziv kategorije");
        editName.setText(category.getName());
        layout.addView(editName);

        Spinner colorSpinner = new Spinner(requireContext());
        colorSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                availableColorNames)); // Prikaz imena boja
        layout.addView(colorSpinner);

        new AlertDialog.Builder(requireContext())
                .setTitle("Izmeni kategoriju")
                .setView(layout)
                .setPositiveButton("Sačuvaj", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String newColor = availableColors.get(colorSpinner.getSelectedItemPosition()); // hex kod za backend

                    if(newName.isEmpty()){
                        Toast.makeText(requireContext(),"Unesi naziv", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for(Category c: categories){
                        if( c.getUserId().equals(currentUserId) && c.getId() != category.getId() && c.getName().equalsIgnoreCase(newName)) {
                            Toast.makeText(requireContext(),"Naziv već postoji", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    category.setName(newName);
                    category.setColor(newColor);
                    CategoryRepository.getInstance(getContext()).updateCategory(category);

                    //loadLegend();
                    refreshTasksColors(category);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void showAddCategoryDialog() {
        // Svi dostupni heks kodovi
        String[] colors = {"#F44336","#E91E63","#9C27B0","#3F51B5","#2196F3","#4CAF50","#FF9800","#FFEB3B"};

        // Mapa hex → ime boje
        Map<String, String> colorNames = new HashMap<>();
        colorNames.put("#F44336", "Crvena");
        colorNames.put("#E91E63", "Roze");
        colorNames.put("#9C27B0", "Ljubičasta");
        colorNames.put("#3F51B5", "Indigo");
        colorNames.put("#2196F3", "Plava");
        colorNames.put("#4CAF50", "Zelena");
        colorNames.put("#FF9800", "Narandžasta");
        colorNames.put("#FFEB3B", "Žuta");

        // filtriranje već zauzetih boja
        List<String> availableColors = new ArrayList<>();
        List<String> availableColorNames = new ArrayList<>();


        for (String color : colors) {
            boolean exists = false;

            if (categories != null) {
                for (Category c : categories) {
                    if (c.getUserId().equals(currentUserId) && c.getColor().equalsIgnoreCase(color)) {
                        exists = true;
                        break;
                    }
                }

            }
            if (!exists) {
                availableColors.add(color);
                availableColorNames.add(colorNames.get(color));
            }
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16,16,16,16);

        EditText editName = new EditText(requireContext());
        editName.setHint("Naziv kategorije");
        layout.addView(editName);

        Spinner colorSpinner = new Spinner(requireContext());
        colorSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                availableColorNames));
        layout.addView(colorSpinner);

        new AlertDialog.Builder(requireContext())
                .setTitle("Dodaj novu kategoriju")
                .setView(layout)
                .setPositiveButton("Sačuvaj", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    if(newName.isEmpty()){
                        Toast.makeText(requireContext(),"Unesi naziv", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(categories != null){
                        for(Category c: categories){
                            if(c.getUserId().equals(currentUserId) && c.getName().equalsIgnoreCase(newName)) {
                                Toast.makeText(requireContext(),"Naziv već postoji", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                    String newColor = availableColors.get(colorSpinner.getSelectedItemPosition());
                    Category newCategory = new Category();
                    newCategory.setName(newName);
                    newCategory.setColor(newColor);
                    newCategory.setUserId(currentUserId);

                    CategoryRepository.getInstance(getContext()).addCategory(newCategory);
                    //loadLegend(); // osveži legendu
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }


    private void refreshTasksColors(Category updatedCategory){
        TaskRepository.getInstance(getContext()).getAllTasksLiveData()
                .observe(getViewLifecycleOwner(), tasks -> {
                    if(tasks != null){
                        for(Task task: tasks){
                            if(task.getCategory().equalsIgnoreCase(updatedCategory.getName())){
                                task.setColor(updatedCategory.getColor());
                                TaskRepository.getInstance(getContext()).updateTask(task);
                            }
                        }
                        listView.invalidateViews();
                        reloadCalendarEvents();

                    }
                });
    }

    private void reloadCalendarEvents() {
        loadTasks(); // poziva tvoju metodu koja učitava sve EventDay iz TaskRepository
    }
    private void updateLegendButtons() {
        layoutLegend.removeAllViews();
        if (categories != null) {
            for (Category category : categories) {
                Button btn = new Button(requireContext());
                btn.setText(category.getName());
                btn.setBackgroundColor(Color.parseColor(category.getColor()));
                btn.setTextColor(Color.WHITE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 0, 8, 0);
                btn.setLayoutParams(params);
                btn.setOnClickListener(v -> showEditCategoryDialog(category));
                layoutLegend.addView(btn);
            }
        }

        // Dugme za dodavanje nove kategorije
        Button btnAdd = new Button(requireContext());
        btnAdd.setText("+");
        btnAdd.setBackgroundColor(Color.DKGRAY);
        btnAdd.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams paramsAdd = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsAdd.setMargins(8, 0, 8, 0);
        btnAdd.setLayoutParams(paramsAdd);
        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
        layoutLegend.addView(btnAdd);
    }

}
