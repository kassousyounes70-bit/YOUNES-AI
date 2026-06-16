package com.YOUNES.AI;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    // ── قائمة بيضاء من النطاقات المقبولة ──
    private static final String[] ALLOWED_DOMAINS = {
        "mail.com",
        "inbox.com",
        "icloud.com",
        "outlook.com",
        "hotmail.com"
    };

    public AccountManager(Context context) {
        this.context = context;
        this.client  = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15,  TimeUnit.SECONDS)
                .build();
    }

    // ── توليد بريد مؤقت من mail.tm مع فلترة النطاقات ──
    public void generateEmail(TextView tvEmail, Runnable onDone) {
        new Thread(() -> {
            try {
                // جلب قائمة النطاقات
                Request req = new Request.Builder()
                        .url("https://api.mail.tm/domains")
                        .get().build();

                String selectedDomain = null;

                try (Response res = client.newCall(req).execute()) {
                    String body     = res.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray domains = json.getJSONArray("hydra:member");

                    // ── فلترة: نختار فقط النطاقات المقبولة ──
                    for (int i = 0; i < domains.length(); i++) {
                        String domain = domains.getJSONObject(i)
                                .getString("domain");
                        if (isAllowedDomain(domain)) {
                            selectedDomain = domain;
                            break;
                        }
                    }

                    // إذا لم نجد نطاقاً مناسباً نستخدم mail.com مباشرة
                    if (selectedDomain == null) {
                        selectedDomain = "mail.com";
                    }
                }

                // إنشاء بريد بسيط
                String user     = randomSimpleName();
                String password = generatePassword();
                tempEmail       = user + "@" + selectedDomain;

                // إنشاء الحساب على mail.tm
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
                    mailToken = tokenJson.optString("token", null);
                }

                final String finalEmail = tempEmail;
                ((Activity) context).runOnUiThread(() -> {
                    tvEmail.setText(finalEmail);
                    tvEmail.setTextColor(0xFFFFFFFF);
                    if (onDone != null) onDone.run();
                });

            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                    Toast.makeText(context,
                        "❌ فشل توليد البريد: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── التحقق من النطاق ──
    private boolean isAllowedDomain(String domain) {
        if (domain == null) return false;
        // يجب أن ينتهي بـ .com
        if (!domain.endsWith(".com")) return false;
        // يجب أن يكون من القائمة البيضاء
        for (String allowed : ALLOWED_DOMAINS) {
            if (domain.equals(allowed)) return true;
        }
        return false;
    }

    // ── اسم بسيط وقابل للقراءة ──
    private String randomSimpleName() {
        String[] firstParts  = {
            "ahmed", "sara", "omar", "lina", "younes",
            "adam", "nora", "kareem", "hana", "tarek"
        };
        String[] secondParts = {
            "tech", "pro", "dev", "user", "mail",
            "2025", "2026", "app", "work", "net"
        };
        Random rnd = new Random();
        return firstParts[rnd.nextInt(firstParts.length)]
             + secondParts[rnd.nextInt(secondParts.length)]
             + (rnd.nextInt(900) + 100); // رقم 3 أرقام
    }

    // ── كلمة سر قوية ──
    public String generatePassword() {
        String upper  = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower  = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%";

        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();

        // حرف كبير، حرف صغير، رقم، رمز
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(special.charAt(rnd.nextInt(special.length())));

        // باقي الكلمة
        String all = upper + lower + digits;
        for (int i = 0; i < 8; i++)
            sb.append(all.charAt(rnd.nextInt(all.length())));

        // خلط الأحرف
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    // ── WebView للتسجيل داخل التطبيق ──
    public void openSignupInWebView(WebView webView) {
        // حذف الكوكيز والبيانات
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                // البقاء داخل التطبيق دائماً
                if (url.startsWith("https://github.com")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl("https://github.com/signup");
    }

    // ── WebView لصفحة التوكن ──
    public void openTokenPageInWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                if (url.startsWith("https://github.com")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(
            "https://github.com/settings/tokens/new" +
            "?scopes=repo,workflow&description=YounesAI");
    }

    // ── نسخ نص ──
    public void copyText(String text, String label) {
        ClipboardManager cm = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(context, "✅ تم نسخ " + label,
                Toast.LENGTH_SHORT).show();
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
                    JSONObject json =
                            new JSONObject(res.body().string());
                    JSONArray msgs =
                            json.getJSONArray("hydra:member");

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

                    ((Activity) context).runOnUiThread(() ->
                        tvInbox.setText(sb.toString()));
                }
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                    tvInbox.setText("❌ فشل التحديث"));
            }
        }).start();
    }

    public String getTempEmail() { return tempEmail; }
}
