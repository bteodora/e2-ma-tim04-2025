package com.example.rpgapp.fragments.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapters.MessageAdapter;
import com.example.rpgapp.model.MissionTask;
import com.example.rpgapp.model.SpecialMission;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AllianceChatFragment extends Fragment {

    private AllianceViewModel viewModel;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText editTextMessage;
    private Button buttonSendMessage;
    private boolean messageTaskCompletedToday = false; // da ne rešava više puta dnevno
    private SpecialMissionViewModel specialMissionViewModel;
    private String currentUserId;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(AllianceViewModel.class);

        specialMissionViewModel = new ViewModelProvider(requireActivity())
                .get(SpecialMissionViewModel.class);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }



        recyclerView = view.findViewById(R.id.recyclerViewChat);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSendMessage = view.findViewById(R.id.buttonSendMessage);

        setupRecyclerView();
        observeViewModel();

        buttonSendMessage.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString();
            if (!messageText.trim().isEmpty()) {
                viewModel.sendMessage(messageText);
                editTextMessage.setText("");

                // --- Specijalna misija: Poruka u savezu ---
                if (!messageTaskCompletedToday && specialMissionViewModel.getCurrentMission().getValue() != null) {
                    SpecialMission activeMission = specialMissionViewModel.getCurrentMission().getValue();
                    for (int i = 0; i < activeMission.getTasks().size(); i++) {
                        MissionTask task = activeMission.getTasks().get(i);
                        if ("Poruka u savezu".equals(task.getName())) {
                            specialMissionViewModel.completeTask(i, activeMission.getMissionId(), currentUserId);
                            messageTaskCompletedToday = true; // task rešen za danas
                            Toast.makeText(requireContext(),
                                    "Zadatak iz specijalne misije: Poruka u savezu rešen!",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }
            }
        });
    }

    private void setupRecyclerView() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String myId = currentUser.getUid();
        adapter = new MessageAdapter(myId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                adapter.setMessages(messages);
                if (messages.size() > 0) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }
}