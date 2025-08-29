package com.example.rpgapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.User;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private static List<User> userList = new ArrayList<>();
    private OnUserClickListener listener; // <<-- DODATO

    // Interfejs za obradu klika
    public interface OnUserClickListener {
        void onUserClick(String userId);
    }

    // Konstruktor koji prihvata listener
    public UserAdapter(OnUserClickListener listener) { // <<-- MODIFIKOVANO
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.userList.clear();
        this.userList.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false);
        return new UserViewHolder(view, listener); // <<-- MODIFIKOVANO
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatar;
        private TextView username, levelAndTitle;
        private Button actionButton;

        // Prosleđujemo listener i u ViewHolder
        public UserViewHolder(@NonNull View itemView, OnUserClickListener listener) { // <<-- MODIFIKOVANO
            super(itemView);
            avatar = itemView.findViewById(R.id.imageViewUserAvatar);
            username = itemView.findViewById(R.id.textViewUserName);
            levelAndTitle = itemView.findViewById(R.id.textViewUserLevel);
            actionButton = itemView.findViewById(R.id.buttonUserAction);

            // Postavljamo OnClickListener na celu karticu
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(userList.get(position).getUserId());
                }
            });
        }

        public void bind(User user) {
            username.setText(user.getUsername());
            String levelInfo = "Level " + user.getLevel() + " - " + user.getTitle();
            levelAndTitle.setText(levelInfo);
        }
    }
}