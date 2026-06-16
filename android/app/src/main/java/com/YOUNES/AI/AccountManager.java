package com.YOUNES.AI;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class AccountManager {

    private final Context      context;
    private final OkHttpClient client;
    private       String       tempEmail;
    private       String       mailToken;

    public AccountManager(Context context) {
        this.context = context;
        this.client  = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // ── توليد بريد مؤقت من mail.tm ──
    public void generateEmail(TextView tvEmail, Runnable onDone) {
        new Thread(() -> {
            try {
                // الحصول على قائمة الدومينات
                Request req = new Request.Builder()
                        .url("https://api.mail.tm/domains")
                        .get().build();

                try (Response res = client.newCall(req).execute()) {
                    String body    = res.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray domains = json.getJSONArray("hydra:member");
                    String domain = domains.getJSONObject(0)
                            .getString("domain");

                    // إنشاء بريد عشوائي
                    String user     = randomString(10);
                    String password = generatePassword();
                    tempEmail       = user + "@" + domain;

                    // إنشاء الحساب
                    JSONObject regBody = new JSONObject();
                    regBody.put("address",  tempEmail);
                    regBody.put("password", password);

                    Request regReq = new Request.Builder()
                            .url("https://api.mail.tm/accounts")
                            .post(RequestBody.create(
                                    regBody.toString(),
                                    MediaType.parse("application/json")))
                            .build();

                    client.newCall(regReq).execute();

                    // الحصول على توكن mail.tm
                    JSONObject tokenBody = new JSONObject();
                    tokenBody.put("address",  tempEmail);
                    tokenBody.put("password", password);

                    Request tokenReq = new Request.Builder()
                            .url("https://api.mail.tm/token")
                            .post(RequestBody.create(
                                    tokenBody.toString(),
                                    MediaType.parse("application/json")))
                            .build();

                    try (Response tokenRes = client.newCall(tokenReq).execute()) {
                        JSONObject tokenJson =
                                new JSONObject(tokenRes.body().string());
                        mailToken = tokenJson.getString("token");
                    }

                    // تحديث الواجهة
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        tvEmail.setText(tempEmail);
                        tvEmail.setTextColor(0xFFFFFFFF);
                        if (onDone != null) onDone.run();
                    });
                }
            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() ->
                    Toast.makeText(context,
                        "❌ فشل توليد البريد: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── توليد كلمة سر قوية ──
    public String generatePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz"
                     + "23456789!@#$%";
        StringBuilder sb  = new StringBuilder();
        Random        rnd = new Random();
        for (int i = 0; i < 12; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // ── نص عشوائي ──
    private String randomString(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb  = new StringBuilder();
        Random        rnd = new Random();
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // ── نسخ النص ──
    public void copyText(String text, String label) {
        ClipboardManager cm = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(context, "✅ تم نسخ " + label,
                Toast.LENGTH_SHORT).show();
    }

    // ── فتح صفحة التسجيل ──
    public void openSignup() {
        context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/signup")));
    }

    // ── فتح صفحة التوكن ──
    public void openTokenPage() {
        context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/settings/tokens/new" +
                          "?scopes=repo,workflow&description=YounesAI")));
    }

    // ── قراءة البريد الوارد ──
    public void checkInbox(TextView tvInbox) {
        if (mailToken == null) {
            tvInbox.setText("⚠️ ولّد بريداً أولاً");
            return;
        }
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url("https://api.mail.tm/messages")
                        .header("Authorization", "Bearer " + mailToken)
                        .get().build();

                try (Response res = client.newCall(req).execute()) {
                    JSONObject json = new JSONObject(res.body().string());
                    JSONArray  msgs = json.getJSONArray("hydra:member");

                    StringBuilder sb = new StringBuilder();
                    if (msgs.length() == 0) {
                        sb.append("لا توجد رسائل بعد...");
                    } else {
                        for (int i = 0; i < msgs.length(); i++) {
                            JSONObject msg = msgs.getJSONObject(i);
                            sb.append("📩 ")
                              .append(msg.getString("subject"))
                              .append("\n")
                              .append(msg.getString("intro"))
                              .append("\n\n");
                        }
                    }

                    ((android.app.Activity) context).runOnUiThread(() ->
                        tvInbox.setText(sb.toString()));
                }
            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() ->
                    tvInbox.setText("❌ فشل التحديث"));
            }
        }).start();
    }

    public String getTempEmail() { return tempEmail; }
                                  }
