package com.example.rpgapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemberProgressAdapter extends RecyclerView.Adapter<MemberProgressAdapter.ViewHolder> {

    private List<User> members = new ArrayList<>();
    private Map<String, Integer> userProgressMap;

    // Prazan konstruktor za inicijalizaciju u fragmentu
    public MemberProgressAdapter() {}

    // Konstruktor sa podacima (opciono)
    public MemberProgressAdapter(List<User> members, Map<String, Integer> userProgressMap) {
        if (members != null) this.members = members;
        this.userProgressMap = userProgressMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_member_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User member = members.get(position);
        int progress = 0;
        if (userProgressMap != null) {
            progress = userProgressMap.getOrDefault(member.getUserId(), 0);
        }
        holder.textName.setText(member.getUsername());
        holder.textProgress.setText("Progress: " + progress);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    // Metoda za postavljanje članova i napretka i osvežavanje adaptera
    public void setMembersAndProgress(List<User> members, Map<String, Integer> progressMap) {
        if (members != null) this.members = members;
        this.userProgressMap = progressMap;
        notifyDataSetChanged();
    }

    // Samo osvežava progres kada se izvrši task
    public void updateProgress(Map<String, Integer> progressMap) {
        this.userProgressMap = progressMap;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textViewMemberName);
            textProgress = itemView.findViewById(R.id.textViewMemberProgress);
        }
    }
}