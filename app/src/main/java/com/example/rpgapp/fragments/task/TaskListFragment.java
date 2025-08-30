package com.example.rpgapp.fragments.task;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.rpgapp.R;
import com.example.rpgapp.adapters.TaskAdapter;
import com.example.rpgapp.database.SQLiteHelper;
import com.example.rpgapp.database.TaskRepository;
import com.example.rpgapp.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends ListFragment implements AdapterView.OnItemLongClickListener {

    private TaskAdapter adapter;
    private static final String TAG = "TaskListFragment";
    private MenuProvider menuProvider;

    public static TaskListFragment newInstance() {
        return new TaskListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView Task List Fragment");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setDividerHeight(2);
        fillData();
        getListView().setOnItemLongClickListener(this);

        addMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        fillData();
    }

    private void addMenu() {
        menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.nav_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.nav_create_task) {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_nav_content_main);
                    navController.navigate(R.id.nav_create_task);
                    return true;
                }
                return false; // druge stavke se ignorišu
            }

        };

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void fillData() {
        TaskRepository repository = TaskRepository.getInstance(getContext());
        List<Task> tasks = repository.getAllTasksLiveData().getValue();
        if (tasks == null) tasks = new ArrayList<>();

        adapter = new TaskAdapter(getActivity(), tasks); // prilagođeni ListAdapter
        setListAdapter(adapter);
    }

//    @Override
//    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
//        Task selectedTask = (Task) l.getItemAtPosition(position);
//
//        // otvaranje TaskPageFragment
//        requireActivity().getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_nav_content_main, TaskPageFragment.newInstance(selectedTask.getTaskId()))
//                .addToBackStack(null)
//                .commit();
//    }
@Override
public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Task selectedTask = (Task) l.getItemAtPosition(position);


    Bundle bundle = new Bundle();
    bundle.putString("taskId", selectedTask.getTaskId());

    Navigation.findNavController(v).navigate(R.id.action_taskList_to_taskPage, bundle);
}


    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Task task = (Task) adapterView.getItemAtPosition(position);
        TaskRepository.getInstance(getContext()).deleteTask(task);
        adapter.notifyDataSetChanged();
        return true;
    }


}
