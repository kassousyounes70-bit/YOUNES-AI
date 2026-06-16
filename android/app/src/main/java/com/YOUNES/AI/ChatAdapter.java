package com.YOUNES.AI;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MessageModel>  messages;
    private OnMessageActionListener   actionListener;

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
            if (msg.getModelName() != null
                    && !msg.getModelName().isEmpty()) {
                h.tvModel.setText(msg.getModelName());
                h.tvModel.setVisibility(View.VISIBLE);
            } else {
                h.tvModel.setVisibility(View.GONE);
            }

            // ── حالة NPC حسب المشاعر ──
            if (msg.getMessage().isEmpty()) {
                h.npcView.setState(NpcView.STATE_TYPING);
            } else {
                int state = emotionToState(msg.getEmotion());
                h.npcView.setState(state);
            }

            // ── كشف الكود وإظهار زر التحميل ──
            String code     = extractCode(msg.getMessage());
            String ext      = extractExtension(msg.getMessage());
            if (code != null) {
                h.btnDownload.setVisibility(View.VISIBLE);
                h.btnDownload.setOnClickListener(v ->
                    saveCodeFile(v.getContext(), code, ext));
            } else {
                h.btnDownload.setVisibility(View.GONE);
            }

            // ضغط مطول = نسخ
            h.tvMessage.setOnLongClickListener(v -> {
                copyText(v.getContext(), msg.getMessage());
                return true;
            });

        } else if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).npcView
                    .setState(NpcView.STATE_THINKING);
        }
    }

    // ── استخراج الكود من الرد ──
    private String extractCode(String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("```[\\w]*\\n([\\s\\S]*?)```");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    // ── استخراج الامتداد ──
    private String extractExtension(String text) {
        if (text == null) return "txt";
        Pattern p = Pattern.compile("```(\\w+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String lang = m.group(1).toLowerCase();
            switch (lang) {
                case "java":       return "java";
                case "xml":        return "xml";
                case "kotlin":     return "kt";
                case "javascript":
                case "js":         return "js";
                case "python":
                case "py":         return "py";
                case "html":       return "html";
                case "css":        return "css";
                case "json":       return "json";
                case "gradle":     return "gradle";
                case "yaml":
                case "yml":        return "yml";
                default:           return "txt";
            }
        }
        return "txt";
    }

    // ── حفظ الكود كملف ──
    private void saveCodeFile(Context ctx, String code, String ext) {
        try {
            File dir = new File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "YounesAI");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "code_"
                + System.currentTimeMillis() + "." + ext;
            File file = new File(dir, fileName);
            FileWriter fw = new FileWriter(file);
            fw.write(code);
            fw.close();

            Toast.makeText(ctx,
                "✅ تم الحفظ: " + fileName,
                Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(ctx,
                "❌ فشل الحفظ: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    private int emotionToState(String emotion) {
        if (emotion == null) return NpcView.STATE_HAPPY;
        switch (emotion.toUpperCase()) {
            case "SAD":         return NpcView.STATE_SAD;
            case "EXCITED":     return NpcView.STATE_EXCITED;
            case "ANGRY":       return NpcView.STATE_ANGRY;
            case "LAUGHING":    return NpcView.STATE_LAUGHING;
            case "CELEBRATING": return NpcView.STATE_CELEBRATING;
            case "THINKING":    return NpcView.STATE_THINKING;
            default:            return NpcView.STATE_HAPPY;
        }
    }

    private void copyText(Context ctx, String text) {
        ClipboardManager cm = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("رسالة", text));
        Toast.makeText(ctx, "✅ تم النسخ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() { return messages.size(); }

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
        NpcView  npcView;
        android.widget.ImageButton btnDownload;
        BotViewHolder(View v) {
            super(v);
            tvMessage   = v.findViewById(R.id.tv_message);
            tvTime      = v.findViewById(R.id.tv_time);
            tvModel     = v.findViewById(R.id.tv_model);
            npcView     = v.findViewById(R.id.npc_view);
            btnDownload = v.findViewById(R.id.btn_download);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        NpcView npcView;
        LoadingViewHolder(View v) {
            super(v);
            npcView = v.findViewById(R.id.npc_view);
        }
    }
}
