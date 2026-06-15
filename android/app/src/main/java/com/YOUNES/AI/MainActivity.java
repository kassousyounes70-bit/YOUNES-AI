package com.YOUNES.AI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter  chatAdapter;
    private List<MessageModel> messageList;
    private EditText     etMessage;
    private ImageButton  btnSend;
    private ChipGroup    chipGroup;
    private GitHubApi    gitHubApi;
    private Handler      mainHandler;
    private SharedPreferences prefs;

    private String selectedModel = "llama"; // افتراضي

    private static final String PREF_NAME  = "YounesAI";
    private static final String PREF_TOKEN = "github_token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        prefs       = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        checkToken();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        chipGroup    = findViewById(R.id.chip_group);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        // ── أزرار النماذج ──
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_llama)    selectedModel = "llama";
            else if (id == R.id.chip_deepseek) selectedModel = "deepseek";
            else if (id == R.id.chip_gpt5)     selectedModel = "gpt5";
            else if (id == R.id.chip_compare)  selectedModel = "compare";
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void checkToken() {
        String token = prefs.getString(PREF_TOKEN, "");
        if (TextUtils.isEmpty(token)) {
            showTokenDialog();
        } else {
            gitHubApi = new GitHubApi(token);
            addBotMessage("مرحباً! أنا Younes AI 🤖\nاختر النموذج وأرسل لي أي سؤال!");
        }
    }

    private void showTokenDialog() {
        EditText input = new EditText(this);
        input.setHint("ghp_xxxxxxxxxxxxxxxxxxxx");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("🔑 GitHub Token")
                .setMessage("أدخل GitHub Personal Access Token")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String token = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(token)) {
                        prefs.edit().putString(PREF_TOKEN, token).apply();
                        gitHubApi = new GitHubApi(token);
                        addBotMessage("مرحباً! أنا Younes AI 🤖\nاختر النموذج وأرسل لي أي سؤال!");
                    } else {
                        showTokenDialog();
                    }
                })
                .setNegativeButton("تغيير", (dialog, which) -> {
                    prefs.edit().remove(PREF_TOKEN).apply();
                    showTokenDialog();
                })
                .show();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");
        btnSend.setEnabled(false);

        addUserMessage(text);
        int loadingIndex = addLoadingMessage();

        gitHubApi.sendMessage(text, selectedModel, new GitHubApi.Callback() {
            @Override
            public void onSuccess(String issueNumber) {
                int number = Integer.parseInt(issueNumber);
                gitHubApi.waitForReply(number, new GitHubApi.Callback() {
                    @Override
                    public void onSuccess(String reply) {
                        mainHandler.post(() -> {
                            removeLoadingMessage(loadingIndex);
                            // تأثير الكتابة التدريجية
                            typewriterEffect(reply);
                            btnSend.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            removeLoadingMessage(loadingIndex);
                            addBotMessage("⚠️ " + error);
                            btnSend.setEnabled(true);
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    removeLoadingMessage(loadingIndex);
                    addBotMessage("❌ فشل الإرسال: " + error);
                    btnSend.setEnabled(true);
                });
            }
        });
    }

    // ── تأثير الكتابة التدريجية ──
    private void typewriterEffect(String fullText) {
        // أضف رسالة فارغة أولاً
        MessageModel botMsg = new MessageModel("", MessageModel.TYPE_BOT);
        messageList.add(botMsg);
        int index = messageList.size() - 1;
        chatAdapter.notifyItemInserted(index);

        // اكتب حرفاً حرفاً
        mainHandler.post(new Runnable() {
            int charIndex = 0;
            @Override
            public void run() {
                if (charIndex < fullText.length()) {
                    // أضف حرفاً أو كلمة كاملة كل مرة
                    int end = Math.min(charIndex + 3, fullText.length());
                    botMsg.setMessage(fullText.substring(0, end));
                    chatAdapter.notifyItemChanged(index);
                    recyclerView.scrollToPosition(index);
                    charIndex = end;
                    // سرعة الكتابة: 20ms بين كل 3 أحرف
                    mainHandler.postDelayed(this, 20);
                }
            }
        });
    }

    private void addUserMessage(String text) {
        messageList.add(new MessageModel(text, MessageModel.TYPE_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void addBotMessage(String text) {
        messageList.add(new MessageModel(text, MessageModel.TYPE_BOT));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private int addLoadingMessage() {
        messageList.add(new MessageModel("...", MessageModel.TYPE_LOADING));
        int index = messageList.size() - 1;
        chatAdapter.notifyItemInserted(index);
        recyclerView.scrollToPosition(index);
        return index;
    }

    private void removeLoadingMessage(int index) {
        if (index < messageList.size()) {
            messageList.remove(index);
            chatAdapter.notifyItemRemoved(index);
        }
    }
}
