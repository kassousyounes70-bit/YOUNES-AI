package com.YOUNES.AI;

public class NpcAnimator {

    private final NpcView npcView;

    // ── كلمات تحليل المشاعر ──
    private static final String[] SAD_WORDS = {
        "آسف", "حزين", "مؤلم", "أسف", "مأساة", "فقدان",
        "وفاة", "مات", "توفي", "مريض", "صعب", "للأسف",
        "أتمنى لو", "يؤسفني", "خسارة", "أبكي", "دموع"
    };

    private static final String[] EXCITED_WORDS = {
        "رائع", "ممتاز", "مذهل", "استثنائي", "لا يصدق",
        "إبداع", "عبقري", "خيالي", "شيء لم أره", "مثير",
        "مثير للاهتمام", "واو", "يا إلهي", "لا يمكنني التصديق"
    };

    private static final String[] ANGRY_WORDS = {
        "لا أوافق", "خطأ", "غلط", "مزعج", "محبط",
        "انتبه", "تحذير", "خطر", "مشكلة", "عيب",
        "لماذا", "هذا سيء", "لا يقبل", "رفض"
    };

    private static final String[] LAUGHING_WORDS = {
        "هاها", "مضحك", "طريف", "فكاهة", "هههه",
        "لol", "ضحك", "نكتة", "مسلي", "خفيف الظل"
    };

    private static final String[] CELEBRATING_WORDS = {
        "تهانيّ", "مبروك", "نجحت", "أحسنت", "بالتوفيق",
        "عظيم", "فزت", "أنجزت", "تم بنجاح", "ممتاز جداً",
        "🎉", "🏆", "⭐", "✨", "🎊"
    };

    private static final String[] HAPPY_WORDS = {
        "سعيد", "فرح", "جيد", "حسناً", "ممتاز",
        "شكراً", "أهلاً", "مرحباً", "يسعدني", "بكل سرور"
    };

    public NpcAnimator(NpcView npcView) {
        this.npcView = npcView;
    }

    // ── عند إرسال رسالة ──
    public void onMessageSent() {
        npcView.setState(NpcView.STATE_THINKING);
    }

    // ── عند بدء الكتابة ──
    public void onTypingStarted() {
        npcView.setState(NpcView.STATE_TYPING);
    }

    // ── عند اكتمال الرد — يحلل النص تلقائياً ──
    public void onResponseComplete(String responseText) {
        int state = analyzeEmotion(responseText);
        npcView.setState(state);

        // يرجع للوضع الهادئ بعد 4 ثوانٍ
        npcView.postDelayed(() ->
            npcView.setState(NpcView.STATE_IDLE), 4000);
    }

    // ── تحليل المشاعر من النص ──
    public int analyzeEmotion(String text) {
        if (text == null || text.isEmpty())
            return NpcView.STATE_HAPPY;

        String lower = text.toLowerCase();

        // الأولوية للاحتفال
        if (containsAny(lower, CELEBRATING_WORDS))
            return NpcView.STATE_CELEBRATING;

        // الضحك
        if (containsAny(lower, LAUGHING_WORDS))
            return NpcView.STATE_LAUGHING;

        // الحزن
        if (containsAny(lower, SAD_WORDS))
            return NpcView.STATE_SAD;

        // الغضب
        if (containsAny(lower, ANGRY_WORDS))
            return NpcView.STATE_ANGRY;

        // التحمس
        if (containsAny(lower, EXCITED_WORDS))
            return NpcView.STATE_EXCITED;

        // الفرح العام
        if (containsAny(lower, HAPPY_WORDS))
            return NpcView.STATE_HAPPY;

        // افتراضي
        return NpcView.STATE_HAPPY;
    }

    private boolean containsAny(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    // ── عند خطأ ──
    public void onError() {
        npcView.setState(NpcView.STATE_ANGRY);
        npcView.postDelayed(() ->
            npcView.setState(NpcView.STATE_IDLE), 3000);
    }

    // ── عند نجاح عملية ──
    public void onSuccess() {
        npcView.setState(NpcView.STATE_CELEBRATING);
        npcView.postDelayed(() ->
            npcView.setState(NpcView.STATE_IDLE), 4000);
    }

    // ── تغيير مباشر ──
    public void setState(int state) {
        npcView.setState(state);
    }
}
