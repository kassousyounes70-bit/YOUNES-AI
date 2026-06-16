package com.YOUNES.AI;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MessageModel>    messages;
    private       OnMessageActionListener actionListener;

    public interface OnMessageActionListener {
        void onEditMessage(String message, int position);
    }

    public void setOnMessageActionListener(OnMessageActionListener l) {
        this.actionListener = l;
    }

    public ChatAdapter(List<MessageModel> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == MessageModel.TYPE_USER) {
            return new UserViewHolder(
                inf.inflate(R.layout.item_message_user, parent, false));
        } else if (viewType == MessageModel.TYPE_LOADING) {
            return new LoadingViewHolder(
                inf.inflate(R.layout.item_message_loading, parent, false));
        } else {
            return new BotViewHolder(
                inf.inflate(R.layout.item_message_bot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = messages.get(position);

        if (holder instanceof UserViewHolder) {
            UserViewHolder h = (UserViewHolder) holder;
            h.tvMessage.setText(msg.getMessage());
            h.tvTime.setText(msg.getTimeFormatted());

            // ضغط مطول = تعديل
            h.tvMessage.setOnLongClickListener(v -> {
                if (actionListener != null)
                    actionListener.onEditMessage(
                        msg.getMessage(), position);
                return true;
            });

        } else if (holder instanceof BotViewHolder) {
            BotViewHolder h = (BotViewHolder) holder;
            h.tvMessage.setText(msg.getMessage());
            h.tvTime.setText(msg.getTimeFormatted());

            // اسم النموذج
            if (msg.getModelName() != null) {
                h.tvModel.setText(msg.getModelName());
                h.tvModel.setVisibility(View.VISIBLE);
            } else {
                h.tvModel.setVisibility(View.GONE);
            }

            // ضغط مطول = نسخ
            h.tvMessage.setOnLongClickListener(v -> {
                copyText(v.getContext(), msg.getMessage());
                return true;
            });
        }
    }

    private void copyText(Context ctx, String text) {
        ClipboardManager cm =
            (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("رسالة", text));
        Toast.makeText(ctx, "✅ تم النسخ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ── ViewHolders ──
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        UserViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_message);
            tvTime    = v.findViewById(R.id.tv_time);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvModel;
        BotViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_message);
            tvTime    = v.findViewById(R.id.tv_time);
            tvModel   = v.findViewById(R.id.tv_model);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View v) { super(v); }
    }
}
