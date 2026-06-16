package com.YOUNES.AI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements ChatAdapter.OnMessageActionListener {

    // ── Views المحادثة ──
    private RecyclerView       recyclerView;
    private ChatAdapter        chatAdapter;
    private List<MessageModel> messageList;

    // ── Views المقارنة ──
    private ViewPager2 viewPager;
    private TabLayout  tabLayout;
    private View       layoutNormal;
    private View       layoutCompare;

    // ── قوائم المقارنة المستقلة ──
    private final List<MessageModel>[] compareLists   = new List[3];
    private final ChatAdapter[]        compareAdapters = new ChatAdapter[3];

    // ── Views الحساب ──
    private View layoutAccount;

    // ── Input ──
    private EditText    etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private ChipGroup   chipGroup;

    // ── عداد التوكن ──
    private TextView tvRequestsLeft;
    private TextView tvResetTime;

    private GitHubApi gitHubApi;
    private Handler   mainHandler;
    private SharedPreferences prefs;

    private String selectedModel    = "phi4";
    private String attachedFile     = null;
    private String attachedFileName = null;

    private static final String PREF_NAME  = "YounesAI";
    private static final String PREF_TOKEN = "github_token";

    private static final String[] MODEL_NAMES =
            {"⚡ Phi-4", "🐋 DeepSeek", "✨ GPT-5"};

    // ── ذاكرة منفصلة لكل نموذج ──
    private final List<MessageModel> memoryPhi4     = new ArrayList<>();
    private final List<MessageModel> memoryDeepSeek = new ArrayList<>();
    private final List<MessageModel> memoryGpt5     = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs       = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        for (int i = 0; i < 3; i++) {
            compareLists[i]    = new ArrayList<>();
            compareAdapters[i] = new ChatAdapter(compareLists[i]);
        }

        initViews();
        checkToken();
    }

    private void initViews() {
        recyclerView    = findViewById(R.id.recycler_view);
        etMessage       = findViewById(R.id.et_message);
        btnSend         = findViewById(R.id.btn_send);
        btnAttach       = findViewById(R.id.btn_attach);
        chipGroup       = findViewById(R.id.chip_group);
        layoutNormal    = findViewById(R.id.layout_normal);
        layoutCompare   = findViewById(R.id.layout_compare);
        layoutAccount   = findViewById(R.id.layout_account);

        // ── إعداد شاشة الحساب ──
        AccountManager accountManager = new AccountManager(this);

        TextView tvTempEmail = findViewById(R.id.tv_temp_email);
        TextView tvPassword  = findViewById(R.id.tv_password);
        TextView tvInbox     = findViewById(R.id.tv_inbox);

        String[] generatedPassword = {""};

        findViewById(R.id.btn_gen_email).setOnClickListener(v ->
            accountManager.generateEmail(tvTempEmail, null));

        findViewById(R.id.btn_copy_email).setOnClickListener(v -> {
            if (accountManager.getTempEmail() != null)
                accountManager.copyText(accountManager.getTempEmail(), "البريد");
        });

        findViewById(R.id.btn_gen_password).setOnClickListener(v -> {
            generatedPassword[0] = accountManager.generatePassword();
            tvPassword.setText(generatedPassword[0]);
            tvPassword.setTextColor(0xFFFFFFFF);
        });

        findViewById(R.id.btn_copy_password).setOnClickListener(v -> {
            if (!generatedPassword[0].isEmpty())
                accountManager.copyText(generatedPassword[0], "كلمة السر");
        });

        findViewById(R.id.btn_open_signup).setOnClickListener(v ->
            accountManager.openSignup());

        findViewById(R.id.btn_open_token).setOnClickListener(v ->
            accountManager.openTokenPage());

        findViewById(R.id.btn_enter_token).setOnClickListener(v ->
            showTokenDialog());

        findViewById(R.id.btn_refresh_inbox).setOnClickListener(v ->
            accountManager.checkInbox(tvInbox));

        viewPager       = findViewById(R.id.view_pager);
        tabLayout       = findViewById(R.id.tab_layout);
        tvRequestsLeft  = findViewById(R.id.tv_requests_left);
        tvResetTime     = findViewById(R.id.tv_reset_time);

        // ── RecyclerView المحادثة العادية ──
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatAdapter.setOnMessageActionListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(chatAdapter);

        // ── ViewPager المقارنة ──
        viewPager.setAdapter(
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                    @NonNull RecyclerView.ViewHolder holder, int pos) {
                ((RecyclerView) holder.itemView)
                        .setAdapter(compareAdapters[pos]);
            }

            @Override
            public int getItemCount() { return 3; }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(MODEL_NAMES[pos])
        ).attach();

        // ── أزرار النماذج ──
        chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == R.id.chip_llama)    selectedModel = "phi4";
            else if (id == R.id.chip_deepseek) selectedModel = "deepseek";
            else if (id == R.id.chip_gpt5)     selectedModel = "gpt5";
            else if (id == R.id.chip_compare)  selectedModel = "compare";

            boolean isCompare = selectedModel.equals("compare");
            layoutNormal.setVisibility(isCompare ? View.GONE  : View.VISIBLE);
            layoutCompare.setVisibility(isCompare ? View.VISIBLE : View.GONE);
            tabLayout.setVisibility(isCompare ? View.VISIBLE : View.GONE);
            layoutAccount.setVisibility(View.GONE);
        });

        // ── زر إرفاق ملف ──
        btnAttach.setOnClickListener(v -> openFilePicker());

        // ── زر الإرسال ──
        btnSend.setOnClickListener(v -> sendMessage());

        // ── Bottom Navigation ──
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) {
                showChat();
            } else if (id == R.id.nav_account) {
                showAccount();
            }
            return true;
        });
    }

    // ── عرض شاشة المحادثة ──
    private void showChat() {
        boolean isCompare = selectedModel.equals("compare");
        layoutNormal.setVisibility(isCompare ? View.GONE  : View.VISIBLE);
        layoutCompare.setVisibility(isCompare ? View.VISIBLE : View.GONE);
        tabLayout.setVisibility(isCompare ? View.VISIBLE : View.GONE);
        layoutAccount.setVisibility(View.GONE);
    }

    // ── عرض شاشة الحساب ──
    private void showAccount() {
        layoutNormal.setVisibility(View.GONE);
        layoutCompare.setVisibility(View.GONE);
        tabLayout.setVisibility(View.GONE);
        layoutAccount.setVisibility(View.VISIBLE);
        updateCounter();
    }

    // ── تحديث عداد التوكن ──
    private void updateCounter() {
        if (gitHubApi == null) return;
        gitHubApi.resetCounterIfNeeded();
        tvRequestsLeft.setText(
            "الطلبات المتبقية: " + gitHubApi.getRequestsLeft()
            + " / " + gitHubApi.getRequestsLimit());
        tvResetTime.setText("يتجدد خلال: " + gitHubApi.getResetTime());
    }

    // ── فتح منتقي الملفات ──
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(
            Intent.createChooser(intent, "اختر ملفاً"), 100);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                // قراءة محتوى الملف
                java.io.InputStream is =
                        getContentResolver().openInputStream(uri);
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                is.close();
                attachedFile     = new String(bytes);
                attachedFileName = uri.getLastPathSegment();
                etMessage.setHint("📎 " + attachedFileName
                        + " — اكتب طلبك...");
                Toast.makeText(this, "✅ تم إرفاق: " + attachedFileName,
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "❌ فشل قراءة الملف",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkToken() {
        String token = prefs.getString(PREF_TOKEN, "");
        if (TextUtils.isEmpty(token)) {
            showTokenDialog();
        } else {
            gitHubApi = new GitHubApi(token);
            addBotMessage(
                "مرحباً! أنا Younes AI 🤖\nاختر النموذج وأرسل أي سؤال!",
                "system");
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
                .setPositiveButton("حفظ", (d, w) -> {
                    String token = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(token)) {
                        prefs.edit().putString(PREF_TOKEN, token).apply();
                        gitHubApi = new GitHubApi(token);
                        addBotMessage(
                            "مرحباً! أنا Younes AI 🤖\n" +
                            "اختر النموذج وأرسل أي سؤال!",
                            "system");
                    } else {
                        showTokenDialog();
                    }
                })
                .setNegativeButton("تغيير", (d, w) -> {
                    prefs.edit().remove(PREF_TOKEN).apply();
                    showTokenDialog();
                })
                .show();
    }

    // ── الحصول على ذاكرة النموذج المحدد ──
    private List<MessageModel> getMemory(String model) {
        switch (model) {
            case "deepseek": return memoryDeepSeek;
            case "gpt5":     return memoryGpt5;
            default:         return memoryPhi4;
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");
        etMessage.setHint("اكتب رسالتك...");
        btnSend.setEnabled(false);

        String fileContent    = attachedFile;
        String fileName       = attachedFileName;
        attachedFile          = null;
        attachedFileName      = null;

        if (selectedModel.equals("compare")) {
            // ── وضع المقارنة ──
            for (int i = 0; i < 3; i++) {
                compareLists[i].add(
                    new MessageModel(text, MessageModel.TYPE_USER));
                compareLists[i].add(
                    new MessageModel("...", MessageModel.TYPE_LOADING));
                compareAdapters[i].notifyDataSetChanged();
            }

            // نستخدم ذاكرة Phi4 للمقارنة
            gitHubApi.sendCompare(memoryPhi4, text, fileContent,
                    new GitHubApi.Callback() {
                @Override
                public void onSuccess(String reply) {
                    mainHandler.post(() -> {
                        showCompareResult(reply);
                        updateCounter();
                        btnSend.setEnabled(true);
                    });
                }

                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        for (int i = 0; i < 3; i++) {
                            removeLastLoading(i);
                            compareLists[i].add(new MessageModel(
                                "⚠️ " + error, MessageModel.TYPE_BOT));
                            compareAdapters[i].notifyDataSetChanged();
                        }
                        btnSend.setEnabled(true);
                    });
                }
            });

        } else {
            // ── نموذج واحد ──
            List<MessageModel> memory = getMemory(selectedModel);

            if (fileName != null) {
                addUserMessage("📎 " + fileName + "\n" + text);
            } else {
                addUserMessage(text);
            }
            int loadingIdx = addLoadingMessage();

            gitHubApi.sendMessage(selectedModel, memory, text,
                    fileContent, new GitHubApi.Callback() {
                @Override
                public void onSuccess(String reply) {
                    mainHandler.post(() -> {
                        removeLoadingMessage(loadingIdx);
                        // حفظ في الذاكرة
                        memory.add(new MessageModel(
                            text, MessageModel.TYPE_USER));
                        memory.add(new MessageModel(
                            reply, MessageModel.TYPE_BOT));
                        // تأثير الكتابة
                        typewriterEffect(reply, selectedModel);
                        updateCounter();
                        btnSend.setEnabled(true);
                    });
                }

                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        removeLoadingMessage(loadingIdx);
                        addBotMessage("❌ " + error, selectedModel);
                        btnSend.setEnabled(true);
                    });
                }
            });
        }
    }

    // ── عرض نتائج المقارنة ──
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
                final int      idx  = i;
                final String   text = responses[i];
                MessageModel   msg  = new MessageModel(
                        "", MessageModel.TYPE_BOT, MODEL_NAMES[i]);
                compareLists[idx].add(msg);
                compareAdapters[idx].notifyDataSetChanged();
                typewriterInList(msg, text, idx);
            }
        }
    }

    private void removeLastLoading(int idx) {
        List<MessageModel> list = compareLists[idx];
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getType() == MessageModel.TYPE_LOADING) {
                list.remove(i);
                compareAdapters[idx].notifyItemRemoved(i);
                break;
            }
        }
    }

    private void typewriterInList(MessageModel msg, String text, int idx) {
        mainHandler.post(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (i < text.length()) {
                    int end = Math.min(i + 3, text.length());
                    msg.setMessage(text.substring(0, end));
                    int pos = compareLists[idx].indexOf(msg);
                    if (pos >= 0) compareAdapters[idx].notifyItemChanged(pos);
                    i = end;
                    mainHandler.postDelayed(this, 20);
                }
            }
        });
    }

    private void typewriterEffect(String fullText, String model) {
        MessageModel botMsg = new MessageModel(
                "", MessageModel.TYPE_BOT, model);
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
        Toast.makeText(this, "✏️ عدّل الرسالة وأعد إرسالها",
                Toast.LENGTH_SHORT).show();
    }

    private void addUserMessage(String text) {
        messageList.add(new MessageModel(text, MessageModel.TYPE_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void addBotMessage(String text, String model) {
        messageList.add(new MessageModel(text, MessageModel.TYPE_BOT, model));
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
