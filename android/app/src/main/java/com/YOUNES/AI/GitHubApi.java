package com.YOUNES.AI;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class GitHubApi {

    private static final String TAG        = "GitHubApi";
    private static final String REPO_OWNER = "kassousyounes70-bit";
    private static final String REPO_NAME  = "YOUNES-AI";
    private static final String BASE_URL   = "https://api.github.com/repos/"
                                             + REPO_OWNER + "/" + REPO_NAME;

    private final OkHttpClient client;
    private final String token;

    public interface Callback {
        void onSuccess(String result);
        void onError(String error);
    }

    public GitHubApi(String token) {
        this.token = token;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ── إرسال رسالة كـ Issue جديد ──
    public void sendMessage(String message, Callback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("title", "🤖 AI Request");
                body.put("body", message);
                body.put("labels", new JSONArray().put("ai-request"));

                Request request = new Request.Builder()
                        .url(BASE_URL + "/issues")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .post(RequestBody.create(
                                body.toString(),
                                MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        int issueNumber = json.getInt("number");
                        callback.onSuccess(String.valueOf(issueNumber));
                    } else {
                        callback.onError("فشل الإرسال: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "sendMessage error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── انتظار رد الذكاء على الـ Issue ──
    public void waitForReply(int issueNumber, Callback callback) {
        new Thread(() -> {
            int attempts = 0;
            int maxAttempts = 30; // انتظر حتى 5 دقائق

            while (attempts < maxAttempts) {
                try {
                    Thread.sleep(10000); // كل 10 ثوان
                    attempts++;

                    Request request = new Request.Builder()
                            .url(BASE_URL + "/issues/" + issueNumber + "/comments")
                            .header("Authorization", "token " + token)
                            .header("Accept", "application/vnd.github.v3+json")
                            .get()
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            JSONArray comments = new JSONArray(response.body().string());
                            if (comments.length() > 0) {
                                JSONObject lastComment = comments.getJSONObject(comments.length() - 1);
                                String body = lastComment.getString("body");
                                // تحقق أن الرد من الـ bot وليس من المستخدم
                                if (body.contains("🤖 رد المساعد")) {
                                    callback.onSuccess(cleanReply(body));
                                    return;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "waitForReply error", e);
                }
            }
            callback.onError("انتهى وقت الانتظار، حاول مرة أخرى");
        }).start();
    }

    // ── تنظيف الرد من رموز Markdown ──
    private String cleanReply(String raw) {
        return raw
                .replace("## 🤖 رد المساعد", "")
                .replace("---", "")
                .replace("*⏱️ Younes AI Agent — kassousyounes70-bit/YOUNES-AI*", "")
                .replaceAll("### 📋 العمليات المنفذة:[\\s\\S]*", "")
                .trim();
    }
}
