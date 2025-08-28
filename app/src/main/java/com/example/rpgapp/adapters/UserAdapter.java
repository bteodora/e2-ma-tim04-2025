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

    private List<User> userList = new ArrayList<>();

    public void setUsers(List<User> users) {
        this.userList.clear();
        this.userList.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false);
        return new UserViewHolder(view);
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

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.imageViewUserAvatar);
            username = itemView.findViewById(R.id.textViewUserName);
            levelAndTitle = itemView.findViewById(R.id.textViewUserLevel);
            actionButton = itemView.findViewById(R.id.buttonUserAction);
        }

        public void bind(User user) {
            username.setText(user.getUsername());
            String levelInfo = "Level " + user.getLevel() + " - " + user.getTitle();
            levelAndTitle.setText(levelInfo);

            // TODO: Postavi pravu sliku avatara na osnovu user.getAvatarId()
            // TODO: Implementiraj logiku za actionButton (npr. "Add Friend", "Pending", "Friends")
        }
    }
}