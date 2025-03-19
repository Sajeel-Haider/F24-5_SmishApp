package com.example.f24_5;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<Chat> chatList;

    public ChatAdapter(List<Chat> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each chat item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.tvName.setText(chat.getName());
        holder.tvMessagePreview.setText(chat.getMessage());
        holder.tvTime.setText(chat.getTime());
        holder.tvSource.setText(chat.getSource());

        // Set click listener to open ChatDetailActivity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, ChatDetailActivity.class);
                intent.putExtra("name", chat.getName());
                intent.putExtra("message", chat.getMessage());
                intent.putExtra("time", chat.getTime());
                intent.putExtra("source", chat.getSource());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMessagePreview, tvTime, tvSource;
        ImageView ivAvatar;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMessagePreview = itemView.findViewById(R.id.tvMessagePreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSource = itemView.findViewById(R.id.tvSource);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
