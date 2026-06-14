package com.YOUNES.AI;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MessageModel> messages;

    public ChatAdapter(List<MessageModel> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == MessageModel.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == MessageModel.TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_message_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = messages.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(msg.getMessage());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).tvMessage.setText(msg.getMessage());
        }
        // LoadingViewHolder لا يحتاج بيانات
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ── User ViewHolder ──
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }

    // ── Bot ViewHolder ──
    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        BotViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }

    // ── Loading ViewHolder ──
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }
}
