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

    // ── Views المحادثة العادية ──
    private RecyclerView         recyclerView;
    private ChatAdapter          chatAdapter;
    private List<MessageModel>   messageList;

    // ── Views المقارنة ──
    private ViewPager2  viewPager;
    private TabLayout   tabLayout;
    private View        layoutNormal;
    private View        layoutCompare;

    // ── قوائم رسائل منفصلة لكل نموذج في المقارنة ──
    private final List<MessageModel>[] compareLists = new List[3];
    private final ChatAdapter[]        compareAdapters = new ChatAdapter[3];

    // ── Input ──
    private EditText    etMessage;
    private ImageButton btnSend;
    private ChipGroup   chipGroup;

    private GitHubApi gitHubApi;
    private Handler   mainHandler;
    private SharedPreferences prefs;
    private String    selectedModel   = "phi4";
    private int       editingPosition = -1;

    private static final String PREF_NAME  = "YounesAI";
    private static final String PREF_TOKEN = "github_token";

    private static final String[] MODEL_NAMES =
            {"⚡ Phi-4", "🐋 DeepSeek", "✨ GPT-5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs       = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // تهيئة قوائم المقارنة
        for (int i = 0; i < 3; i++) {
            compareLists[i]    = new ArrayList<>();
            compareAdapters[i] = new ChatAdapter(compareLists[i]);
        }

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

        // ── RecyclerView للمحادثة العادية ──
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatAdapter.setOnMessageActionListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(chatAdapter);

        // ── ViewPager — كل صفحة لها RecyclerView مستقل ──
        viewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                RecyclerView rv = new RecyclerView(parent.getContext());
                rv.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                LinearLayoutManager llm =
                        new LinearLayoutManager(parent.getContext());
                llm.setStackFromEnd(true);
                rv.setLayoutManager(llm);
                return new RecyclerView.ViewHolder(rv) {};
            }

            @Override
            public void onBindViewHolder(
                    @NonNull RecyclerView.ViewHolder holder, int position) {
                RecyclerView rv = (RecyclerView) holder.itemView;
                rv.setAdapter(compareAdapters[position]);
            }

            @Override
            public int getItemCount() { return 3; }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(MODEL_NAMES[pos])
        ).attach();

        // ── أزرار النماذج ──
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_llama)    selectedModel = "phi4";
            else if (id == R.id.chip_deepseek) selectedModel = "deepseek";
            else if (id == R.id.chip_gpt5)     selectedModel = "gpt5";
            else if (id == R.id.chip_compare)  selectedModel = "compare";

            if (selectedModel.equals("compare")) {
                layoutNormal.setVisibility(View.GONE);
                layoutCompare.setVisibility(View.VISIBLE);
                tabLayout.setVisibility(View.VISIBLE);
            } else {
                layoutNormal.setVisibility(View.VISIBLE);
                layoutCompare.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void checkToken() {
        String token = prefs.getString(PREF_TOKEN, "");
        if (TextUtils.isEmpty(token)) {
            showTokenDialog();
        } else {
            gitHubApi = new GitHubApi(token);
            addBotMessage("مرحباً! أنا Younes AI 🤖\nاختر النموذج وأرسل أي سؤال!");
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
                        addBotMessage("مرحباً! أنا Younes AI 🤖\nاختر النموذج وأرسل أي سؤال!");
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

        if (selectedModel.equals("compare")) {
            // ── وضع المقارنة: أضف رسالة المستخدم للثلاثة ──
            for (int i = 0; i < 3; i++) {
                compareLists[i].add(new MessageModel(text, MessageModel.TYPE_USER));
                compareLists[i].add(new MessageModel("...", MessageModel.TYPE_LOADING));
                compareAdapters[i].notifyDataSetChanged();
            }
        } else {
            addUserMessage(text);
        }

        int loadingIndex = selectedModel.equals("compare") ? -1 : addLoadingMessage();

        gitHubApi.sendMessage(text, selectedModel, new GitHubApi.Callback() {
            @Override
            public void onSuccess(String issueNumber) {
                gitHubApi.waitForReply(
                    Integer.parseInt(issueNumber),
                    new GitHubApi.Callback() {
                        @Override
                        public void onSuccess(String reply) {
                            mainHandler.post(() -> {
                                if (reply.contains("##COMPARE##")) {
                                    showCompareResult(reply);
                                } else {
                                    if (loadingIndex >= 0)
                                        removeLoadingMessage(loadingIndex);
                                    typewriterEffect(reply);
                                }
                                btnSend.setEnabled(true);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            mainHandler.post(() -> {
                                if (loadingIndex >= 0)
                                    removeLoadingMessage(loadingIndex);
                                if (selectedModel.equals("compare")) {
                                    for (int i = 0; i < 3; i++) {
                                        removeLastLoading(i);
                                        compareLists[i].add(new MessageModel(
                                            "⚠️ " + error, MessageModel.TYPE_BOT));
                                        compareAdapters[i].notifyDataSetChanged();
                                    }
                                } else {
                                    addBotMessage("⚠️ " + error);
                                }
                                btnSend.setEnabled(true);
                            });
                        }
                    });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    if (loadingIndex >= 0) removeLoadingMessage(loadingIndex);
                    addBotMessage("❌ " + error);
                    btnSend.setEnabled(true);
                });
            }
        });
    }

    // ── عرض المقارنة — كل نموذج في تبويبه المستقل ──
    private void showCompareResult(String reply) {
        String[] parts = reply.split("##MODEL[123]##");
        if (parts.length >= 4) {
            String[] responses = {
                parts[1].replace("##END##", "").trim(),
                parts[2].replace("##END##", "").trim(),
                parts[3].replace("##END##", "").trim()
            };

            for (int i = 0; i < 3; i++) {
                removeLastLoading(i);
                final int idx      = i;
                final String text  = responses[i];
                MessageModel msg   = new MessageModel("", MessageModel.TYPE_BOT);
                compareLists[idx].add(msg);
                compareAdapters[idx].notifyDataSetChanged();
                typewriterInList(msg, text, idx);
            }
        }
    }

    // ── إزالة آخر loading في قائمة محددة ──
    private void removeLastLoading(int listIndex) {
        List<MessageModel> list = compareLists[listIndex];
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getType() == MessageModel.TYPE_LOADING) {
                list.remove(i);
                compareAdapters[listIndex].notifyItemRemoved(i);
                break;
            }
        }
    }

    // ── تأثير الكتابة في قائمة مقارنة محددة ──
    private void typewriterInList(MessageModel msg, String text, int listIdx) {
        mainHandler.post(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (i < text.length()) {
                    int end = Math.min(i + 3, text.length());
                    msg.setMessage(text.substring(0, end));
                    compareAdapters[listIdx].notifyItemChanged(
                            compareLists[listIdx].indexOf(msg));
                    i = end;
                    mainHandler.postDelayed(this, 20);
                }
            }
        });
    }

    // ── تأثير الكتابة في المحادثة العادية ──
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
        Toast.makeText(this, "✏️ عدّل الرسالة وأعد إرسالها",
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
