package com.YOUNES.AI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList;
    private EditText etMessage;
    private ImageButton btnSend;
    private GitHubApi gitHubApi;
    private Handler mainHandler;
    private SharedPreferences prefs;

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

        messageList  = new ArrayList<>();
        chatAdapter  = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    // ── التحقق من وجود التوكن ──
    private void checkToken() {
        String token = prefs.getString(PREF_TOKEN, "");
        if (TextUtils.isEmpty(token)) {
            showTokenDialog();
        } else {
            gitHubApi = new GitHubApi(token);
            addBotMessage("مرحباً! أنا Younes AI 🤖\nأرسل لي أي سؤال أو أمر برمجي وسأنفذه فوراً!");
        }
    }

    // ── طلب التوكن من المستخدم ──
    private void showTokenDialog() {
        EditText input = new EditText(this);
        input.setHint("ghp_xxxxxxxxxxxxxxxxxxxx");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("🔑 GitHub Token")
                .setMessage("أدخل GitHub Personal Access Token الخاص بك\n(Settings → Developer settings → Tokens)")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String token = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(token)) {
                        prefs.edit().putString(PREF_TOKEN, token).apply();
                        gitHubApi = new GitHubApi(token);
                        addBotMessage("مرحباً! أنا Younes AI 🤖\nأرسل لي أي سؤال أو أمر برمجي وسأنفذه فوراً!");
                    } else {
                        Toast.makeText(this, "يجب إدخال التوكن", Toast.LENGTH_SHORT).show();
                        showTokenDialog();
                    }
                })
                .setNegativeButton("تغيير التوكن", (dialog, which) -> {
                    prefs.edit().remove(PREF_TOKEN).apply();
                    showTokenDialog();
                })
                .show();
    }

    // ── إرسال الرسالة ──
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");
        btnSend.setEnabled(false);

        // أضف رسالة المستخدم
        addUserMessage(text);

        // أضف مؤشر التحميل
        int loadingIndex = addLoadingMessage();

        // أرسل إلى GitHub
        gitHubApi.sendMessage(text, new GitHubApi.Callback() {
            @Override
            public void onSuccess(String issueNumber) {
                int number = Integer.parseInt(issueNumber);
                // انتظر رد الذكاء
                gitHubApi.waitForReply(number, new GitHubApi.Callback() {
                    @Override
                    public void onSuccess(String reply) {
                        mainHandler.post(() -> {
                            removeLoadingMessage(loadingIndex);
                            addBotMessage(reply);
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

    // ── إضافة رسالة مستخدم ──
    private void addUserMessage(String text) {
        messageList.add(new MessageModel(text, MessageModel.TYPE_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    // ── إضافة رسالة بوت ──
    private void addBotMessage(String text) {
        messageList.add(new MessageModel(text, MessageModel.TYPE_BOT));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    // ── إضافة مؤشر التحميل ──
    private int addLoadingMessage() {
        messageList.add(new MessageModel("...", MessageModel.TYPE_LOADING));
        int index = messageList.size() - 1;
        chatAdapter.notifyItemInserted(index);
        recyclerView.scrollToPosition(index);
        return index;
    }

    // ── إزالة مؤشر التحميل ──
    private void removeLoadingMessage(int index) {
        if (index < messageList.size()) {
            messageList.remove(index);
            chatAdapter.notifyItemRemoved(index);
        }
    }
}
