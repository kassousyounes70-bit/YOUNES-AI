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

    private final List<MessageModel> messages;
    private OnMessageActionListener actionListener;

    public interface OnMessageActionListener {
        void onEditMessage(String message, int position);
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
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
            UserViewHolder h = (UserViewHolder) holder;
            h.tvMessage.setText(msg.getMessage());

            // ── ضغط مطول = تعديل الرسالة ──
            h.tvMessage.setOnLongClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEditMessage(msg.getMessage(), position);
                }
                return true;
            });

            // ── ضغط مزدوج = نسخ ──
            h.tvMessage.setOnClickListener(new View.OnClickListener() {
                int clickCount = 0;
                @Override
                public void onClick(View v) {
                    clickCount++;
                    if (clickCount == 2) {
                        clickCount = 0;
                        copyToClipboard(v.getContext(), msg.getMessage());
                    }
                    v.postDelayed(() -> clickCount = 0, 400);
                }
            });

        } else if (holder instanceof BotViewHolder) {
            BotViewHolder h = (BotViewHolder) holder;
            h.tvMessage.setText(msg.getMessage());

            // ── ضغط مطول = نسخ رسالة البوت ──
            h.tvMessage.setOnLongClickListener(v -> {
                copyToClipboard(v.getContext(), msg.getMessage());
                return true;
            });
        }
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard =
            (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("رسالة", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "✅ تم النسخ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        BotViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }
}
