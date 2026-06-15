package com.YOUNES.AI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements ChatAdapter.OnMessageActionListener {

    private RecyclerView       recyclerView;
    private ChatAdapter        chatAdapter;
    private List<MessageModel> messageList;
    private EditText           etMessage;
    private ImageButton        btnSend;
    private ChipGroup          chipGroup;
    private ViewPager2         viewPager;
    private TabLayout          tabLayout;
    private View               layoutNormal;
    private View               layoutCompare;

    // نصوص ردود المقارنة
    private final String[] compareTexts = {"", "", ""};

    private GitHubApi gitHubApi;
    private Handler   mainHandler;
    private SharedPreferences prefs;
    private String    selectedModel  = "llama";
    private int       editingPosition = -1;

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
        recyclerView  = findViewById(R.id.recycler_view);
        etMessage     = findViewById(R.id.et_message);
        btnSend       = findViewById(R.id.btn_send);
        chipGroup     = findViewById(R.id.chip_group);
        layoutNormal  = findViewById(R.id.layout_normal);
        layoutCompare = findViewById(R.id.layout_compare);
        viewPager     = findViewById(R.id.view_pager);
        tabLayout     = findViewById(R.id.tab_layout);

        // ── إعداد RecyclerView ──
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatAdapter.setOnMessageActionListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(chatAdapter);

        // ── إعداد ViewPager للمقارنة ──
        String[] modelNames = {"🦙 Llama 4", "🐋 DeepSeek", "✨ GPT-5"};

        viewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                ScrollView scroll = new ScrollView(parent.getContext());
                TextView tv = new TextView(parent.getContext());
                tv.setPadding(32, 32, 32, 32);
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(15f);
                tv.setTag("model_" + viewType);
                scroll.addView(tv);
                scroll.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                return new RecyclerView.ViewHolder(scroll) {};
            }

            @Override
            public void onBindViewHolder(
                    @NonNull RecyclerView.ViewHolder holder, int position) {
                ScrollView scroll = (ScrollView) holder.itemView;
                TextView tv = (TextView) scroll.getChildAt(0);
                tv.setTag("model_" + position);
                tv.setText(compareTexts[position]);
            }

            @Override
            public int getItemCount() { return 3; }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(modelNames[position])
        ).attach();

        // ── أزرار النماذج ──
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_llama)    selectedModel = "llama";
            else if (id == R.id.chip_deepseek) selectedModel = "deepseek";
            else if (id == R.id.chip_gpt5)     selectedModel = "gpt5";
            else if (id == R.id.chip_compare)  selectedModel = "compare";

            tabLayout.setVisibility(
                selectedModel.equals("compare") ? View.VISIBLE : View.GONE);
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
        editingPosition = -1;
        btnSend.setEnabled(false);

        addUserMessage(text);
        int loadingIndex = addLoadingMessage();

        gitHubApi.sendMessage(text, selectedModel, new GitHubApi.Callback() {
            @Override
            public void onSuccess(String issueNumber) {
                gitHubApi.waitForReply(
                    Integer.parseInt(issueNumber),
                    new GitHubApi.Callback() {
                        @Override
                        public void onSuccess(String reply) {
                            mainHandler.post(() -> {
                                removeLoadingMessage(loadingIndex);
                                if (reply.startsWith("##COMPARE##")) {
                                    showCompareResult(reply);
                                } else {
                                    layoutNormal.setVisibility(View.VISIBLE);
                                    layoutCompare.setVisibility(View.GONE);
                                    typewriterEffect(reply);
                                }
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
                    addBotMessage("❌ " + error);
                    btnSend.setEnabled(true);
                });
            }
        });
    }

    // ── عرض المقارنة في 3 شاشات ──
    private void showCompareResult(String reply) {
        layoutNormal.setVisibility(View.GONE);
        layoutCompare.setVisibility(View.VISIBLE);

        String[] parts = reply.split("##MODEL[123]##");
        if (parts.length >= 4) {
            compareTexts[0] = parts[1].replace("##END##", "").trim();
            compareTexts[1] = parts[2].replace("##END##", "").trim();
            compareTexts[2] = parts[3].replace("##END##", "").trim();
            viewPager.getAdapter().notifyDataSetChanged();

            // تأثير الكتابة في الصفحة الأولى
            mainHandler.postDelayed(() -> {
                typewriterInPager(0, compareTexts[0]);
                typewriterInPager(1, compareTexts[1]);
                typewriterInPager(2, compareTexts[2]);
            }, 300);
        }
    }

    // ── تأثير الكتابة في صفحة ViewPager ──
    private void typewriterInPager(int page, String text) {
        RecyclerView.ViewHolder holder =
            (RecyclerView.ViewHolder) viewPager.getTag(page);
        if (holder == null) return;

        ScrollView scroll = (ScrollView) holder.itemView;
        TextView tv = (TextView) scroll.getChildAt(0);
        if (tv == null) return;

        tv.setText("");
        mainHandler.post(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (i < text.length()) {
                    int end = Math.min(i + 3, text.length());
                    tv.setText(text.substring(0, end));
                    i = end;
                    mainHandler.postDelayed(this, 20);
                }
            }
        });
    }

    // ── تأثير الكتابة في المحادثة ──
    private void typewriterEffect(String fullText) {
        MessageModel botMsg = new MessageModel("", MessageModel.TYPE_BOT);
        messageList.add(botMsg);
        int index = messageList.size() - 1;
        chatAdapter.notifyItemInserted(index);

        mainHandler.post(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (i < fullText.length()) {
                    int end = Math.min(i + 3, fullText.length());
                    botMsg.setMessage(fullText.substring(0, end));
                    chatAdapter.notifyItemChanged(index);
                    recyclerView.scrollToPosition(index);
                    i = end;
                    mainHandler.postDelayed(this, 20);
                }
            }
        });
    }

    @Override
    public void onEditMessage(String message, int position) {
        etMessage.setText(message);
        etMessage.setSelection(message.length());
        editingPosition = position;
        Toast.makeText(this, "✏️ عدّل الرسالة وأرسلها مجدداً",
                Toast.LENGTH_SHORT).show();
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
        if (index >= 0 && index < messageList.size()) {
            messageList.remove(index);
            chatAdapter.notifyItemRemoved(index);
        }
    }
}
