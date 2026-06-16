package com.YOUNES.AI;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class GitHubApi {

    private static final String TAG      = "GitHubApi";
    private static final String BASE_URL = "https://models.inference.ai.azure.com";

    // حدود الـ tokens لكل نموذج
    private static final int MAX_CHARS_DEEPSEEK = 3000;
    private static final int MAX_CHARS_GPT5     = 6000;
    private static final int MAX_CHARS_PHI4      = 12000;
    private static final int CHUNK_SIZE          = 2500;

    private final OkHttpClient client;
    private final String       token;
    private final SharedPreferences prefs;

    private static final String PREF_NAME         = "YounesAI";
    private static final String PREF_REQUESTS     = "requests_used";
    private static final String PREF_RESET_TIME   = "reset_time";

    public interface Callback {
        void onSuccess(String result);
        void onError(String error);
    }

    public GitHubApi(String token, Context context) {
        this.token  = token;
        this.prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30,  TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ── تحويل اسم النموذج ──
    private String resolveModel(String model) {
        switch (model) {
            case "phi4":     return "Phi-4";
            case "deepseek": return "DeepSeek-V3-0324";
            case "gpt5":     return "gpt-4o";
            default:         return "Phi-4";
        }
    }

    // ── حد الحروف حسب النموذج ──
    private int getMaxChars(String model) {
        switch (model) {
            case "deepseek": return MAX_CHARS_DEEPSEEK;
            case "gpt5":     return MAX_CHARS_GPT5;
            default:         return MAX_CHARS_PHI4;
        }
    }

    // ── System Prompt مع تعليمة المشاعر ──
    private String getSystemPrompt(String fileTree) {
        return "أنت مساعد ذكاء اصطناعي متعدد المهام وذكي وودود. " +
               "تستطيع المساعدة في أي موضوع: أسئلة عامة، برمجة، " +
               "تفسير أحلام، نصائح، ترجمة، رياضيات، أدب، وأي موضوع آخر. " +
               "عند إرسال كود برمجي ضعه دائماً داخل بلوك نسخ مع ذكر الامتداد مثل ```java أو ```xml.\n\n" +
               "مهم جداً: في نهاية كل رد أضف سطراً واحداً فقط بهذا الشكل بالضبط:\n" +
               "[EMOTION:HAPPY] أو [EMOTION:SAD] أو [EMOTION:EXCITED] أو " +
               "[EMOTION:ANGRY] أو [EMOTION:LAUGHING] أو [EMOTION:CELEBRATING] أو " +
               "[EMOTION:THINKING]\n" +
               "اختر المشاعر المناسبة لمحتوى ردك. لا تشرح هذا السطر ولا تذكره للمستخدم.\n\n" +
               "تحدث دائماً بالعربية بأسلوب ودي ومفيد.";
    }

    // ── ضغط الصورة ──
    public String compressImage(byte[] imageBytes) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bitmap == null) return null;

            // تصغير إذا كانت كبيرة
            int maxDim = 800;
            if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                float scale = Math.min(
                    (float) maxDim / bitmap.getWidth(),
                    (float) maxDim / bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (int)(bitmap.getWidth()  * scale),
                    (int)(bitmap.getHeight() * scale),
                    true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    // ── إرسال رسالة مباشرة ──
    public void sendMessage(
            String model,
            List<MessageModel> history,
            String userMessage,
            String fileContent,
            boolean isImage,
            byte[] imageBytes,
            Callback callback) {

        new Thread(() -> {
            try {
                // إذا كانت صورة
                if (isImage && imageBytes != null) {
                    String base64 = compressImage(imageBytes);
                    if (base64 != null) {
                        sendWithImage(model, history, userMessage,
                                base64, callback);
                    } else {
                        callback.onError("❌ فشل ضغط الصورة");
                    }
                    return;
                }

                // إذا كان ملف نصي كبير — نجزئه
                if (fileContent != null && !fileContent.isEmpty()) {
                    int maxChars = getMaxChars(model);
                    if (fileContent.length() > maxChars) {
                        sendChunked(model, history, userMessage,
                                fileContent, callback);
                        return;
                    }
                }

                // إرسال عادي
                sendSingle(model, history, userMessage,
                        fileContent, callback);

            } catch (Exception e) {
                Log.e(TAG, "sendMessage error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── إرسال رسالة عادية ──
    private void sendSingle(
            String model,
            List<MessageModel> history,
            String userMessage,
            String fileContent,
            Callback callback) throws Exception {

        String modelId = resolveModel(model);
        JSONArray messages = buildMessages(history, userMessage, fileContent);

        JSONObject body = new JSONObject();
        body.put("model",      modelId);
        body.put("max_tokens", 4096);
        body.put("messages",   messages);

        String response = executeRequest(body, 30);
        if (response != null) {
            // حساب تكلفة الطلب
            int totalChars = userMessage.length()
                    + (fileContent != null ? fileContent.length() : 0);
            decrementCounter(totalChars, false);
            callback.onSuccess(response);
        } else {
            callback.onError("❌ لم يرد النموذج — تجاوز الوقت المحدد");
        }
    }

    // ── إرسال الملف على أجزاء ──
    private void sendChunked(
            String model,
            List<MessageModel> history,
            String userMessage,
            String fileContent,
            Callback callback) throws Exception {

        String modelId = resolveModel(model);
        int totalChunks = (int) Math.ceil(
                (double) fileContent.length() / CHUNK_SIZE);

        // إرسال كل جزء
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end   = Math.min(start + CHUNK_SIZE, fileContent.length());
            String chunk = fileContent.substring(start, end);

            boolean isLast = (i == totalChunks - 1);

            String chunkMsg;
            if (isLast) {
                chunkMsg = "الجزء " + (i + 1) + " من " + totalChunks
                         + " (الأخير):\n" + chunk
                         + "\n\n---\nلديك الآن الملف كاملاً. طلبي: "
                         + userMessage;
            } else {
                chunkMsg = "الجزء " + (i + 1) + " من " + totalChunks
                         + ":\n" + chunk
                         + "\n\nاستقبل هذا الجزء فقط ولا تفعل شيئاً بعد.";
            }

            JSONArray messages = buildMessages(
                    i == 0 ? history : new java.util.ArrayList<>(),
                    chunkMsg, null);

            JSONObject body = new JSONObject();
            body.put("model",      modelId);
            body.put("max_tokens", isLast ? 4096 : 100);
            body.put("messages",   messages);

            String response = executeRequest(body, 30);

            if (isLast) {
                if (response != null) {
                    decrementCounter(fileContent.length(), true);
                    callback.onSuccess(response);
                } else {
                    callback.onError("❌ فشل إرسال الجزء الأخير");
                }
                return;
            }

            if (response == null) {
                callback.onError("❌ فشل إرسال الجزء " + (i + 1));
                return;
            }

            // انتظار بسيط بين الأجزاء
            Thread.sleep(500);
        }
    }

    // ── إرسال مع صورة ──
    private void sendWithImage(
            String model,
            List<MessageModel> history,
            String userMessage,
            String base64Image,
            Callback callback) throws Exception {

        String modelId = resolveModel(model);

        JSONArray messages = new JSONArray();

        // System
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", getSystemPrompt(""));
        messages.put(system);

        // تاريخ المحادثة
        for (MessageModel msg : history) {
            if (msg.getType() == MessageModel.TYPE_USER ||
                msg.getType() == MessageModel.TYPE_BOT) {
                JSONObject m = new JSONObject();
                m.put("role", msg.getType() == MessageModel.TYPE_USER
                        ? "user" : "assistant");
                m.put("content", msg.getMessage());
                messages.put(m);
            }
        }

        // رسالة مع صورة
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        JSONArray content = new JSONArray();

        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", userMessage);
        content.put(textPart);

        JSONObject imgPart = new JSONObject();
        imgPart.put("type", "image_url");
        JSONObject imgUrl = new JSONObject();
        imgUrl.put("url", "data:image/jpeg;base64," + base64Image);
        imgPart.put("image_url", imgUrl);
        content.put(imgPart);

        userMsg.put("content", content);
        messages.put(userMsg);

        JSONObject body = new JSONObject();
        body.put("model",      modelId);
        body.put("max_tokens", 4096);
        body.put("messages",   messages);

        String response = executeRequest(body, 30);
        if (response != null) {
            decrementCounter(userMessage.length(), true);
            callback.onSuccess(response);
        } else {
            callback.onError("❌ لم يرد النموذج");
        }
    }

    // ── بناء قائمة الرسائل ──
    private JSONArray buildMessages(
            List<MessageModel> history,
            String userMessage,
            String fileContent) throws Exception {

        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", getSystemPrompt(""));
        messages.put(system);

        for (MessageModel msg : history) {
            if (msg.getType() == MessageModel.TYPE_USER ||
                msg.getType() == MessageModel.TYPE_BOT) {
                JSONObject m = new JSONObject();
                m.put("role", msg.getType() == MessageModel.TYPE_USER
                        ? "user" : "assistant");
                m.put("content", msg.getMessage());
                messages.put(m);
            }
        }

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        String content = userMessage;
        if (fileContent != null && !fileContent.isEmpty()) {
            content = "محتوى الملف:\n```\n" + fileContent
                    + "\n```\n\nطلبي: " + userMessage;
        }
        userMsg.put("content", content);
        messages.put(userMsg);

        return messages;
    }

    // ── تنفيذ الطلب مع timeout ──
    private String executeRequest(JSONObject body, int timeoutSeconds)
            throws Exception {

        OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds,    TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds,   TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/chat/completions")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type",  "application/json")
                .post(RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = timeoutClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONObject data = new JSONObject(json);

                if (data.has("error")) {
                    String errMsg = data.getJSONObject("error")
                            .optString("message", "خطأ غير معروف");
                    throw new Exception(errMsg);
                }

                return data.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            } else {
                String err = response.body() != null
                        ? response.body().string() : "خطأ " + response.code();
                throw new Exception(err);
            }
        } catch (java.net.SocketTimeoutException e) {
            return null; // timeout
        }
    }

    // ── إرسال المقارنة ──
    public void sendCompare(
            List<MessageModel> history,
            String userMessage,
            String fileContent,
            boolean isImage,
            byte[] imageBytes,
            Callback callback) {

        new Thread(() -> {
            try {
                String[]   models  = {"phi4", "deepseek", "gpt5"};
                String[]   results = new String[3];
                int[]      done    = {0};

                for (int i = 0; i < 3; i++) {
                    final int idx = i;
                    sendMessage(models[i], history, userMessage,
                            fileContent, isImage, imageBytes,
                            new Callback() {
                        @Override
                        public void onSuccess(String result) {
                            results[idx] = result;
                            done[0]++;
                            if (done[0] == 3) {
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
    private void decrementCounter(int charCount, boolean hasFile) {
        int cost;
        if (hasFile) {
            cost = 4;
        } else if (charCount < 500) {
            cost = 1;
        } else if (charCount < 2000) {
            cost = 2;
        } else {
            cost = 3;
        }

        int current = prefs.getInt(PREF_REQUESTS, 0);
        long resetTime = prefs.getLong(PREF_RESET_TIME, 0);

        // تجديد العداد إذا انتهى الوقت
        if (resetTime > 0 && System.currentTimeMillis() > resetTime) {
            current = 0;
            resetTime = System.currentTimeMillis() + 24L * 60 * 60 * 1000;
        }

        if (resetTime == 0) {
            resetTime = System.currentTimeMillis() + 24L * 60 * 60 * 1000;
        }

        current += cost;
        prefs.edit()
             .putInt(PREF_REQUESTS, current)
             .putLong(PREF_RESET_TIME, resetTime)
             .apply();
    }

    public int getRequestsUsed() {
        resetIfNeeded();
        return prefs.getInt(PREF_REQUESTS, 0);
    }

    public int getRequestsLimit()  { return 150; }

    public int getRequestsLeft() {
        return Math.max(0, 150 - getRequestsUsed());
    }

    public String getResetTime() {
        long resetTime = prefs.getLong(PREF_RESET_TIME, 0);
        if (resetTime == 0) return "غير معروف";
        long diff    = resetTime - System.currentTimeMillis();
        if (diff <= 0) return "جاهز للتجديد";
        long hours   = diff / (1000 * 60 * 60);
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "س " + minutes + "د";
    }

    public void resetIfNeeded() {
        long resetTime = prefs.getLong(PREF_RESET_TIME, 0);
        if (resetTime > 0 && System.currentTimeMillis() > resetTime) {
            prefs.edit()
                 .putInt(PREF_REQUESTS, 0)
                 .putLong(PREF_RESET_TIME,
                     System.currentTimeMillis() + 24L * 60 * 60 * 1000)
                 .apply();
        }
    }
}
