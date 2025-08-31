package com.example.rpgapp.adapters;

import android.util.TypedValue;
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
import com.google.android.material.card.MaterialCardView; // <-- VAŽAN IMPORT
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList = new ArrayList<>();
    private OnUserClickListener cardClickListener;
    private OnActionButtonClickListener actionButtonClickListener;
    private String actionButtonText = null;
    private Map<String, Boolean> selectionMap = null;

    public interface OnUserClickListener {
        void onUserClick(String userId);
    }

    public interface OnActionButtonClickListener {
        void onActionButtonClick(User user);
    }

    public void setOnActionButtonClickListener(OnActionButtonClickListener listener) {
        this.actionButtonClickListener = listener;
    }

    public void setSelectionMap(Map<String, Boolean> selectionMap) {
        this.selectionMap = selectionMap;
    }

    public UserAdapter(OnUserClickListener listener) {
        this.cardClickListener = listener;
    }

    public void setUsers(List<User> users, String buttonText) {
        this.userList.clear();
        this.userList.addAll(users);
        this.actionButtonText = buttonText;
        notifyDataSetChanged();
    }

    public User getUserAt(int position) {
        return userList.get(position);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false);
        return new UserViewHolder(view, cardClickListener, userList);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, actionButtonText, actionButtonClickListener, selectionMap);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


    static class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatar;
        private TextView username, levelAndTitle;
        private Button actionButton;
        private View selectionOverlay;

        public UserViewHolder(@NonNull View itemView, OnUserClickListener listener, List<User> userList) {
            super(itemView);
            avatar = itemView.findViewById(R.id.imageViewUserAvatar);
            username = itemView.findViewById(R.id.textViewUserName);
            levelAndTitle = itemView.findViewById(R.id.textViewUserLevel);
            actionButton = itemView.findViewById(R.id.buttonUserAction);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(userList.get(position).getUserId());
                }
            });
        }

        public void bind(User user, String buttonText, OnActionButtonClickListener actionListener, Map<String, Boolean> selectionMap) {
            username.setText(user.getUsername());
            String levelInfo = "Level " + user.getLevel() + " - " + user.getTitle();
            levelAndTitle.setText(levelInfo);
            // TODO: Postavi avatar

            if (buttonText != null && !buttonText.isEmpty() && actionListener != null) {
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(buttonText);
                actionButton.setOnClickListener(v -> actionListener.onActionButtonClick(user));
            } else {
                actionButton.setVisibility(View.GONE);
                actionButton.setOnClickListener(null);
            }

            if (selectionMap != null) {
                boolean isSelected = selectionMap.containsKey(user.getUserId()) && Boolean.TRUE.equals(selectionMap.get(user.getUserId()));
                selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            } else {
                selectionOverlay.setVisibility(View.GONE);
            }
        }
    }
}