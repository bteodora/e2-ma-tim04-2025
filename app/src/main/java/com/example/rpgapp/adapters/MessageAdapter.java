package com.example.rpgapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Message;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages = new ArrayList<>();
    private String currentUserId;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    private class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageBody, timestamp;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.textViewMessageBody);
            timestamp = itemView.findViewById(R.id.textViewMessageTimestamp);
        }

        void bind(Message message) {
            messageBody.setText(message.getText());
            if (message.getTimestamp() != null) {
                timestamp.setText(timeFormat.format(message.getTimestamp()));
            } else {
                timestamp.setText("");
            }
        }
    }
    private class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderName, messageBody, timestamp;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.textViewSenderName);
            messageBody = itemView.findViewById(R.id.textViewMessageBody);
            timestamp = itemView.findViewById(R.id.textViewMessageTimestamp);
        }

        void bind(Message message) {
            senderName.setText(message.getSenderUsername());
            messageBody.setText(message.getText());
            if (message.getTimestamp() != null) {
                timestamp.setText(timeFormat.format(message.getTimestamp()));
            } else {
                timestamp.setText("");
            }
        }
    }
}