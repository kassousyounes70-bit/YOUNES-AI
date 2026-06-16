package com.YOUNES.AI;

public class NpcAnimator {

    private final NpcView npcView;

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

    // ── عند انتهاء الرد ──
    public void onResponseComplete() {
        npcView.setState(NpcView.STATE_HAPPY);

        // بعد 3 ثوانٍ يرجع للوضع العادي
        npcView.postDelayed(() ->
            npcView.setState(NpcView.STATE_IDLE), 3000);
    }

    // ── عند نجاح عملية ──
    public void onSuccess() {
        npcView.setState(NpcView.STATE_SUCCESS);
        npcView.postDelayed(() ->
            npcView.setState(NpcView.STATE_IDLE), 3000);
    }

    // ── عند خطأ ──
    public void onError() {
        npcView.setState(NpcView.STATE_IDLE);
    }

    // ── تغيير مباشر ──
    public void setState(int state) {
        npcView.setState(state);
    }
}
