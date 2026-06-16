package com.YOUNES.AI;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

public class GitHubApi {

    private static final String TAG      = "GitHubApi";
    private static final String BASE_URL =
            "https://models.inference.ai.azure.com";

    private final OkHttpClient client;
    private final String       token;

    private static int  requestsUsed    = 0;
    private static int  requestsLimit   = 150;
    private static long resetTimeMillis = 0;

    public interface Callback {
        void onSuccess(String result);
        void onError(String error);
    }

    public GitHubApi(String token) {
        this.token  = token;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60,  TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String resolveModel(String model) {
        switch (model) {
            case "phi4":     return "Phi-4";
            case "deepseek": return "DeepSeek-V3-0324";
            case "gpt5":     return "gpt-4o";
            default:         return "Phi-4";
        }
    }

    // ── إرسال رسالة مباشرة إلى GitHub Models ──
    public void sendMessage(
            String model,
            List<MessageModel> history,
            String userMessage,
            String fileContent,
            Callback callback) {

        new Thread(() -> {
            try {
                String modelId = resolveModel(model);

                JSONArray messages = new JSONArray();

                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content",
                    "أنت مساعد ذكاء اصطناعي متعدد المهام وذكي وودود. " +
                    "تستطيع المساعدة في أي موضوع: أسئلة عامة، برمجة، " +
                    "تفسير أحلام، نصائح، ترجمة، رياضيات، أدب، وأي موضوع آخر. " +
                    "عند إرسال كود برمجي ضعه دائماً داخل بلوك نسخ واضح. " +
                    "تحدث دائماً بالعربية بأسلوب ودي ومفيد.");
                messages.put(system);

                for (MessageModel msg : history) {
                    if (msg.getType() == MessageModel.TYPE_USER ||
                        msg.getType() == MessageModel.TYPE_BOT) {
                        JSONObject m = new JSONObject();
                        m.put("role",
                            msg.getType() == MessageModel.TYPE_USER
                                ? "user" : "assistant");
                        m.put("content", msg.getMessage());
                        messages.put(m);
                    }
                }

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                String content = userMessage;
                if (fileContent != null && !fileContent.isEmpty()) {
                    content = "محتوى الملف:\n```\n"
                            + fileContent + "\n```\n\nطلبي: " + userMessage;
                }
                userMsg.put("content", content);
                messages.put(userMsg);

                JSONObject body = new JSONObject();
                body.put("model",      modelId);
                body.put("max_tokens", 4096);
                body.put("messages",   messages);

                Request request = new Request.Builder()
                        .url(BASE_URL + "/chat/completions")
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(
                                body.toString(),
                                MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String     json = response.body().string();
                        JSONObject data = new JSONObject(json);

                        // ── عداد طلب واحد ──
                        requestsUsed++;
                        if (resetTimeMillis == 0) {
                            resetTimeMillis = System.currentTimeMillis()
                                    + 24L * 60 * 60 * 1000;
                        }

                        String reply = data
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        callback.onSuccess(reply);

                    } else {
                        String err = response.body() != null
                                ? response.body().string()
                                : "خطأ غير معروف";
                        callback.onError("خطأ " + response.code() + ": " + err);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "sendMessage error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── إرسال لوضع المقارنة — 3 نماذج ──
    public void sendCompare(
            List<MessageModel> history,
            String userMessage,
            String fileContent,
            Callback callback) {

        new Thread(() -> {
            try {
                String[]   models  = {"phi4", "deepseek", "gpt5"};
                String[]   results = new String[3];
                int[]      done    = {0};

                for (int i = 0; i < 3; i++) {
                    final int idx = i;
                    sendMessage(models[i], history, userMessage,
                            fileContent, new Callback() {
                        @Override
                        public void onSuccess(String result) {
                            results[idx] = result;
                            done[0]++;
                            if (done[0] == 3) {
                                // ── عداد المقارنة = 3 طلبات ──
                                requestsUsed += 2; // +2 لأن كل sendMessage أضاف 1
                                String combined = "##COMPARE##"
                                    + "##MODEL1##" + results[0]
                                    + "##MODEL2##" + results[1]
                                    + "##MODEL3##" + results[2]
                                    + "##END##";
                                callback.onSuccess(combined);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            results[idx] = "⚠️ فشل: " + error;
                            done[0]++;
                            if (done[0] == 3) {
                                requestsUsed += 2;
                                String combined = "##COMPARE##"
                                    + "##MODEL1##" + results[0]
                                    + "##MODEL2##" + results[1]
                                    + "##MODEL3##" + results[2]
                                    + "##END##";
                                callback.onSuccess(combined);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── العداد ──
    public int    getRequestsUsed()  { return requestsUsed; }
    public int    getRequestsLimit() { return requestsLimit; }
    public int    getRequestsLeft()  { return requestsLimit - requestsUsed; }

    public String getResetTime() {
        if (resetTimeMillis == 0) return "غير معروف";
        long diff    = resetTimeMillis - System.currentTimeMillis();
        if (diff <= 0) return "جاهز للتجديد";
        long hours   = diff / (1000 * 60 * 60);
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "س " + minutes + "د";
    }

    public void resetCounterIfNeeded() {
        if (resetTimeMillis > 0 &&
                System.currentTimeMillis() > resetTimeMillis) {
            requestsUsed    = 0;
            resetTimeMillis = System.currentTimeMillis()
                    + 24L * 60 * 60 * 1000;
        }
    }
}
