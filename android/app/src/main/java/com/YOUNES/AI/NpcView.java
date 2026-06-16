package com.YOUNES.AI;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class NpcView extends View {

    // ── حالات الشخصية ──
    public static final int STATE_IDLE        = 0;
    public static final int STATE_THINKING    = 1;
    public static final int STATE_TYPING      = 2;
    public static final int STATE_HAPPY       = 3;
    public static final int STATE_SUCCESS     = 4;
    public static final int STATE_SAD         = 5;
    public static final int STATE_EXCITED     = 6;
    public static final int STATE_ANGRY       = 7;
    public static final int STATE_LAUGHING    = 8;
    public static final int STATE_CELEBRATING = 9;

    private int   currentState  = STATE_IDLE;
    private float breathOffset  = 0f;
    private float blinkProgress = 1f;
    private float handOffset    = 0f;
    private float bubbleAlpha   = 0f;
    private float shakeOffset   = 0f;
    private float jumpOffset    = 0f;
    private float tearDrop      = 0f;

    // ── Animators ──
    private ValueAnimator breathAnimator;
    private ValueAnimator blinkAnimator;
    private ValueAnimator handAnimator;
    private ValueAnimator shakeAnimator;
    private ValueAnimator jumpAnimator;
    private ValueAnimator tearAnimator;

    // ── أدوات الرسم ──
    private final Paint paintSkin    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDark    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintWhite   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGreen   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintShadow  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBubble  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintHair    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintClothes = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTear    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  path         = new Path();

    private String bubbleText = "";

    public NpcView(Context context) {
        super(context);
        init();
    }

    public NpcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintSkin.setColor(0xFFFFCC99);
        paintDark.setColor(0xFF1A1A2E);
        paintWhite.setColor(Color.WHITE);
        paintGreen.setColor(0xFF3FB950);
        paintHair.setColor(0xFF2C1810);
        paintClothes.setColor(0xFF238636);
        paintTear.setColor(0xFF88CCFF);

        paintShadow.setColor(0x33000000);
        paintShadow.setMaskFilter(
            new android.graphics.BlurMaskFilter(
                12f, android.graphics.BlurMaskFilter.Blur.NORMAL));

        paintBubble.setColor(0xFF21262D);
        paintBubble.setStyle(Paint.Style.FILL);

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(18f);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setAntiAlias(true);

        startBaseAnimations();
    }

    // ── حركات أساسية دائمة ──
    private void startBaseAnimations() {
        breathAnimator = ValueAnimator.ofFloat(0f, 1f);
        breathAnimator.setDuration(2000);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimator.setInterpolator(new LinearInterpolator());
        breathAnimator.addUpdateListener(a -> {
            breathOffset = (float) a.getAnimatedValue() * 3f;
            invalidate();
        });
        breathAnimator.start();

        blinkAnimator = ValueAnimator.ofFloat(1f, 0f, 1f);
        blinkAnimator.setDuration(200);
        blinkAnimator.setStartDelay(3500);
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnimator.setRepeatMode(ValueAnimator.RESTART);
        blinkAnimator.addUpdateListener(a -> {
            blinkProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        blinkAnimator.start();
    }

    // ── تغيير الحالة ──
    public void setState(int state) {
        if (currentState == state) return;
        currentState = state;
        stopStateAnimations();

        switch (state) {
            case STATE_THINKING:
                bubbleText = "🤔";
                startBubble();
                startHand(0.5f, 800);
                break;

            case STATE_TYPING:
                bubbleText = "✍️";
                startBubble();
                startHand(4f, 150);
                break;

            case STATE_HAPPY:
                bubbleText = "😊";
                startBubble();
                startJump(3f, 400);
                break;

            case STATE_SUCCESS:
                bubbleText = "✅";
                startBubble();
                startJump(5f, 300);
                break;

            case STATE_SAD:
                bubbleText = "😢";
                startBubble();
                startTear();
                break;

            case STATE_EXCITED:
                bubbleText = "🤩";
                startBubble();
                startJump(8f, 250);
                startHand(6f, 120);
                break;

            case STATE_ANGRY:
                bubbleText = "😠";
                startBubble();
                startShake(5f, 80);
                break;

            case STATE_LAUGHING:
                bubbleText = "😂";
                startBubble();
                startJump(4f, 200);
                startShake(2f, 150);
                break;

            case STATE_CELEBRATING:
                bubbleText = "🎉";
                startBubble();
                startJump(10f, 300);
                startHand(8f, 100);
                break;

            case STATE_IDLE:
            default:
                bubbleText   = "";
                bubbleAlpha  = 0f;
                handOffset   = 0f;
                shakeOffset  = 0f;
                jumpOffset   = 0f;
                tearDrop     = 0f;
                break;
        }
        invalidate();
    }

    private void stopStateAnimations() {
        if (handAnimator  != null) handAnimator.cancel();
        if (shakeAnimator != null) shakeAnimator.cancel();
        if (jumpAnimator  != null) jumpAnimator.cancel();
        if (tearAnimator  != null) tearAnimator.cancel();
    }

    private void startBubble() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(300);
        a.addUpdateListener(v -> {
            bubbleAlpha = (float) v.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    private void startHand(float amp, long dur) {
        handAnimator = ValueAnimator.ofFloat(-amp, amp);
        handAnimator.setDuration(dur);
        handAnimator.setRepeatCount(ValueAnimator.INFINITE);
        handAnimator.setRepeatMode(ValueAnimator.REVERSE);
        handAnimator.addUpdateListener(a -> {
            handOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        handAnimator.start();
    }

    private void startShake(float amp, long dur) {
        shakeAnimator = ValueAnimator.ofFloat(-amp, amp);
        shakeAnimator.setDuration(dur);
        shakeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shakeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        shakeAnimator.addUpdateListener(a -> {
            shakeOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        shakeAnimator.start();
    }

    private void startJump(float amp, long dur) {
        jumpAnimator = ValueAnimator.ofFloat(0f, amp, 0f);
        jumpAnimator.setDuration(dur);
        jumpAnimator.setRepeatCount(ValueAnimator.INFINITE);
        jumpAnimator.setRepeatMode(ValueAnimator.RESTART);
        jumpAnimator.addUpdateListener(a -> {
            jumpOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        jumpAnimator.start();
    }

    private void startTear() {
        tearAnimator = ValueAnimator.ofFloat(0f, 1f);
        tearAnimator.setDuration(1500);
        tearAnimator.setRepeatCount(ValueAnimator.INFINITE);
        tearAnimator.setRepeatMode(ValueAnimator.RESTART);
        tearAnimator.addUpdateListener(a -> {
            tearDrop = (float) a.getAnimatedValue();
            invalidate();
        });
        tearAnimator.start();
    }

    // ── الرسم الرئيسي ──
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w  = getWidth();
        float h  = getHeight();
        float cx = w / 2f + shakeOffset;
        float cy = h / 2f - jumpOffset;

        // ظل
        canvas.drawOval(
            new RectF(cx - 40, h - 15, cx + 40, h - 5),
            paintShadow);

        drawBody(canvas, cx, cy);
        drawNeck(canvas, cx, cy);
        drawHead(canvas, cx, cy);
        drawHair(canvas, cx, cy);
        drawFace(canvas, cx, cy);
        drawHands(canvas, cx, cy);
        drawTears(canvas, cx, cy);

        if (bubbleAlpha > 0 && !bubbleText.isEmpty()) {
            drawBubble(canvas, cx, cy);
        }
    }

    private void drawBody(Canvas canvas, float cx, float cy) {
        // لون الملابس حسب الحالة
        switch (currentState) {
            case STATE_ANGRY:
                paintClothes.setColor(0xFF8B0000);
                break;
            case STATE_SAD:
                paintClothes.setColor(0xFF445566);
                break;
            case STATE_EXCITED:
            case STATE_CELEBRATING:
                paintClothes.setColor(0xFFFF6B00);
                break;
            default:
                paintClothes.setColor(0xFF238636);
                break;
        }

        RectF body = new RectF(
            cx - 35, cy - breathOffset,
            cx + 35, cy + 60 - breathOffset);
        canvas.drawRoundRect(body, 15, 15, paintClothes);

        paintDark.setAlpha(60);
        canvas.drawLine(cx, cy + 5  - breathOffset,
                        cx, cy + 55 - breathOffset, paintDark);
        paintDark.setAlpha(255);

        paintSkin.setColor(0xFFFFCC99);
        path.reset();
        path.moveTo(cx - 12, cy - breathOffset);
        path.lineTo(cx,      cy + 15 - breathOffset);
        path.lineTo(cx + 12, cy - breathOffset);
        canvas.drawPath(path, paintSkin);
    }

    private void drawNeck(Canvas canvas, float cx, float cy) {
        paintSkin.setColor(0xFFFFCC99);
        RectF neck = new RectF(
            cx - 10, cy - 20 - breathOffset,
            cx + 10, cy + 5  - breathOffset);
        canvas.drawRoundRect(neck, 8, 8, paintSkin);
    }

    private void drawHead(Canvas canvas, float cx, float cy) {
        // لون البشرة يتأثر قليلاً بالحالة
        if (currentState == STATE_ANGRY) {
            paintSkin.setColor(0xFFFFAA88); // أحمر خفيف
        } else if (currentState == STATE_SAD) {
            paintSkin.setColor(0xFFDDBB99); // شاحب
        } else {
            paintSkin.setColor(0xFFFFCC99);
        }

        canvas.drawCircle(cx, cy - 50 - breathOffset, 38, paintSkin);

        // احمرار الخدين
        Paint blush = new Paint(Paint.ANTI_ALIAS_FLAG);
        int blushColor;
        switch (currentState) {
            case STATE_EXCITED:
            case STATE_CELEBRATING:
            case STATE_LAUGHING:
                blushColor = 0x66FF6666;
                break;
            case STATE_SAD:
                blushColor = 0x228899AA;
                break;
            case STATE_ANGRY:
                blushColor = 0x88FF2222;
                break;
            default:
                blushColor = 0x44FF9999;
                break;
        }
        blush.setColor(blushColor);
        canvas.drawCircle(cx - 22, cy - 42 - breathOffset, 10, blush);
        canvas.drawCircle(cx + 22, cy - 42 - breathOffset, 10, blush);
    }

    private void drawHair(Canvas canvas, float cx, float cy) {
        paintHair.setColor(0xFF2C1810);
        RectF hairTop = new RectF(
            cx - 38, cy - 90 - breathOffset,
            cx + 38, cy - 40 - breathOffset);
        canvas.drawArc(hairTop, 180, 180, true, paintHair);

        path.reset();
        path.moveTo(cx - 38, cy - 55 - breathOffset);
        path.lineTo(cx - 48, cy - 25 - breathOffset);
        path.lineTo(cx - 30, cy - 20 - breathOffset);
        canvas.drawPath(path, paintHair);

        path.reset();
        path.moveTo(cx + 38, cy - 55 - breathOffset);
        path.lineTo(cx + 48, cy - 25 - breathOffset);
        path.lineTo(cx + 30, cy - 20 - breathOffset);
        canvas.drawPath(path, paintHair);
    }

    private void drawFace(Canvas canvas, float cx, float cy) {
        float eyeY = cy - 52 - breathOffset;

        // ── العيون ──
        canvas.drawCircle(cx - 13, eyeY, 9, paintWhite);
        canvas.drawCircle(cx + 13, eyeY, 9, paintWhite);

        float eyeH = 9f * blinkProgress;
        if (eyeH < 1f) eyeH = 1f;

        // موضع الحدقة حسب الحالة
        float pupilOffsetX = 0f;
        float pupilOffsetY = 0f;
        if (currentState == STATE_SAD) {
            pupilOffsetY = 2f; // ينظر للأسفل
        } else if (currentState == STATE_EXCITED
                || currentState == STATE_CELEBRATING) {
            pupilOffsetY = -1f; // ينظر للأعلى
        }

        paintDark.setColor(0xFF1A1A2E);
        RectF leftEye = new RectF(
            cx - 18 + pupilOffsetX, eyeY - eyeH + pupilOffsetY,
            cx - 8  + pupilOffsetX, eyeY + eyeH + pupilOffsetY);
        canvas.drawOval(leftEye, paintDark);

        RectF rightEye = new RectF(
            cx + 8  + pupilOffsetX, eyeY - eyeH + pupilOffsetY,
            cx + 18 + pupilOffsetX, eyeY + eyeH + pupilOffsetY);
        canvas.drawOval(rightEye, paintDark);

        paintWhite.setAlpha(200);
        canvas.drawCircle(cx - 10, eyeY - 3, 3, paintWhite);
        canvas.drawCircle(cx + 16, eyeY - 3, 3, paintWhite);
        paintWhite.setAlpha(255);

        // ── الحاجبان ──
        Paint browPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        browPaint.setColor(0xFF2C1810);
        browPaint.setStrokeWidth(3f);
        browPaint.setStyle(Paint.Style.STROKE);
        browPaint.setStrokeCap(Paint.Cap.ROUND);

        float browY = eyeY - 13;

        switch (currentState) {
            case STATE_ANGRY:
                // حاجب غاضب منحدر بشدة
                canvas.drawLine(cx - 20, browY - 2,
                               cx - 8,  browY + 5, browPaint);
                canvas.drawLine(cx + 8,  browY + 5,
                               cx + 20, browY - 2, browPaint);
                break;
            case STATE_SAD:
                // حاجب حزين مرتفع في المنتصف
                canvas.drawLine(cx - 20, browY + 4,
                               cx - 8,  browY - 2, browPaint);
                canvas.drawLine(cx + 8,  browY - 2,
                               cx + 20, browY + 4, browPaint);
                break;
            case STATE_THINKING:
                // حاجب متقطب
                canvas.drawLine(cx - 20, browY + 3,
                               cx - 8,  browY, browPaint);
                canvas.drawLine(cx + 8,  browY,
                               cx + 20, browY + 3, browPaint);
                break;
            case STATE_HAPPY:
            case STATE_EXCITED:
            case STATE_CELEBRATING:
            case STATE_LAUGHING:
            case STATE_SUCCESS:
                // حاجب مرفوع للفرح
                canvas.drawLine(cx - 20, browY - 4,
                               cx - 8,  browY - 6, browPaint);
                canvas.drawLine(cx + 8,  browY - 6,
                               cx + 20, browY - 4, browPaint);
                break;
            default:
                canvas.drawLine(cx - 20, browY,
                               cx - 8,  browY, browPaint);
                canvas.drawLine(cx + 8,  browY,
                               cx + 20, browY, browPaint);
                break;
        }

        // ── الفم ──
        Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthPaint.setStrokeWidth(3f);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);

        float mouthY = cy - 30 - breathOffset;

        switch (currentState) {
            case STATE_HAPPY:
            case STATE_SUCCESS:
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.STROKE);
                canvas.drawArc(
                    new RectF(cx - 15, mouthY - 5, cx + 15, mouthY + 10),
                    0, 180, false, mouthPaint);
                // أسنان
                mouthPaint.setColor(Color.WHITE);
                mouthPaint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(
                    new RectF(cx - 12, mouthY, cx + 12, mouthY + 7),
                    3, 3, mouthPaint);
                break;

            case STATE_EXCITED:
            case STATE_CELEBRATING:
                // فم مفتوح كبير
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.FILL);
                canvas.drawOval(
                    new RectF(cx - 14, mouthY - 4, cx + 14, mouthY + 10),
                    mouthPaint);
                mouthPaint.setColor(Color.WHITE);
                canvas.drawRoundRect(
                    new RectF(cx - 10, mouthY, cx + 10, mouthY + 6),
                    2, 2, mouthPaint);
                break;

            case STATE_LAUGHING:
                // فم ضحك عريض
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.FILL);
                canvas.drawOval(
                    new RectF(cx - 16, mouthY - 3, cx + 16, mouthY + 12),
                    mouthPaint);
                mouthPaint.setColor(Color.WHITE);
                canvas.drawRoundRect(
                    new RectF(cx - 12, mouthY, cx + 12, mouthY + 8),
                    3, 3, mouthPaint);
                break;

            case STATE_SAD:
                // فم حزين معكوس
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.STROKE);
                canvas.drawArc(
                    new RectF(cx - 12, mouthY, cx + 12, mouthY + 12),
                    0, -180, false, mouthPaint);
                break;

            case STATE_ANGRY:
                // فم مضغوط غاضب
                mouthPaint.setColor(0xFF993333);
                mouthPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(
                    cx - 12, mouthY + 5,
                    cx + 12, mouthY + 5, mouthPaint);
                break;

            case STATE_THINKING:
                // فم محايد منحني خفيف
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.STROKE);
                path.reset();
                path.moveTo(cx - 10, mouthY + 3);
                path.quadTo(cx + 2, mouthY - 2, cx + 10, mouthY + 5);
                canvas.drawPath(path, mouthPaint);
                break;

            case STATE_TYPING:
                // فم مفتوح قليلاً
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.FILL);
                canvas.drawOval(
                    new RectF(cx - 7, mouthY - 2, cx + 7, mouthY + 5),
                    mouthPaint);
                break;

            default:
                mouthPaint.setColor(0xFFCC6666);
                mouthPaint.setStyle(Paint.Style.STROKE);
                canvas.drawArc(
                    new RectF(cx - 10, mouthY - 3, cx + 10, mouthY + 7),
                    0, 180, false, mouthPaint);
                break;
        }

        // ── الأنف ──
        Paint nosePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nosePaint.setColor(0xFFDDAA88);
        nosePaint.setStyle(Paint.Style.STROKE);
        nosePaint.setStrokeWidth(2f);
        nosePaint.setStrokeCap(Paint.Cap.ROUND);
        path.reset();
        path.moveTo(cx - 4, cy - 42 - breathOffset);
        path.lineTo(cx,     cy - 37 - breathOffset);
        path.lineTo(cx + 4, cy - 42 - breathOffset);
        canvas.drawPath(path, nosePaint);

        // ── علامة الغضب فوق الرأس ──
        if (currentState == STATE_ANGRY) {
            Paint veinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            veinPaint.setColor(0xFFFF4444);
            veinPaint.setStyle(Paint.Style.STROKE);
            veinPaint.setStrokeWidth(2.5f);
            veinPaint.setStrokeCap(Paint.Cap.ROUND);
            float vx = cx + 25;
            float vy = cy - 80 - breathOffset;
            path.reset();
            path.moveTo(vx, vy);
            path.lineTo(vx + 6, vy - 6);
            path.lineTo(vx + 12, vy);
            path.lineTo(vx + 6, vy + 6);
            canvas.drawPath(path, veinPaint);
        }

        // ── نجوم حول الرأس عند الاحتفال ──
        if (currentState == STATE_CELEBRATING
                || currentState == STATE_EXCITED) {
            Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            starPaint.setColor(0xFFFFDD00);
            starPaint.setStyle(Paint.Style.FILL);
            float[] angles = {0, 60, 120, 180, 240, 300};
            for (float angle : angles) {
                float rad = (float) Math.toRadians(angle + jumpOffset * 10);
                float sx  = cx + (float) Math.cos(rad) * 48;
                float sy  = (cy - 50 - breathOffset)
                          + (float) Math.sin(rad) * 48;
                canvas.drawCircle(sx, sy, 4, starPaint);
            }
        }
    }

    private void drawHands(Canvas canvas, float cx, float cy) {
        paintSkin.setColor(0xFFFFCC99);

        Paint armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armPaint.setColor(0xFFFFCC99);
        armPaint.setStyle(Paint.Style.STROKE);
        armPaint.setStrokeWidth(14f);
        armPaint.setStrokeCap(Paint.Cap.ROUND);

        float leftHandX  = cx - 55;
        float leftHandY  = cy + 10 - breathOffset + handOffset;
        float rightHandX = cx + 55;
        float rightHandY = cy + 10 - breathOffset - handOffset;

        // ذراع يسرى
        path.reset();
        path.moveTo(cx - 35, cy + 5 - breathOffset);
        path.quadTo(cx - 45, cy + 10 - breathOffset,
                    leftHandX, leftHandY);
        canvas.drawPath(path, armPaint);
        canvas.drawCircle(leftHandX, leftHandY, 10, paintSkin);

        for (int i = 0; i < 4; i++) {
            float fx = leftHandX - 12 + (i * 8);
            float fy = leftHandY - 8;
            canvas.drawRoundRect(
                new RectF(fx - 3, fy - 10, fx + 3, fy),
                3, 3, paintSkin);
        }

        // ذراع يمنى
        path.reset();
        path.moveTo(cx + 35, cy + 5 - breathOffset);
        path.quadTo(cx + 45, cy + 10 - breathOffset,
                    rightHandX, rightHandY);
        canvas.drawPath(path, armPaint);
        canvas.drawCircle(rightHandX, rightHandY, 10, paintSkin);

        for (int i = 0; i < 4; i++) {
            float fx = rightHandX - 12 + (i * 8);
            float fy = rightHandY - 8;
            canvas.drawRoundRect(
                new RectF(fx - 3, fy - 10, fx + 3, fy),
                3, 3, paintSkin);
        }

        if (currentState == STATE_TYPING) {
            drawKeyboard(canvas, cx, cy);
        }

        // ── يد مرفوعة عند الاحتفال ──
        if (currentState == STATE_CELEBRATING
                || currentState == STATE_EXCITED) {
            Paint celebPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            celebPaint.setColor(0xFFFFDD00);
            celebPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(leftHandX, leftHandY - 15, 6, celebPaint);
            canvas.drawCircle(rightHandX, rightHandY - 15, 6, celebPaint);
        }
    }

    private void drawKeyboard(Canvas canvas, float cx, float cy) {
        Paint kbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kbPaint.setColor(0xFF30363D);
        float kbY = cy + 45 - breathOffset;
        canvas.drawRoundRect(
            new RectF(cx - 45, kbY, cx + 45, kbY + 20),
            4, 4, kbPaint);
        kbPaint.setColor(0xFF484F58);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 8; col++) {
                float kx = cx - 40 + col * 10.5f;
                float ky = kbY + 3 + row * 9;
                canvas.drawRoundRect(
                    new RectF(kx, ky, kx + 8, ky + 6),
                    2, 2, kbPaint);
            }
        }
    }

    // ── رسم الدموع ──
    private void drawTears(Canvas canvas, float cx, float cy) {
        if (currentState != STATE_SAD || tearDrop == 0f) return;

        paintTear.setColor(0xFF88CCFF);
        paintTear.setStyle(Paint.Style.FILL);

        float eyeY = cy - 52 - breathOffset;
        float tearY = eyeY + (tearDrop * 25f);

        // دمعة يسرى
        path.reset();
        path.moveTo(cx - 13, tearY - 5);
        path.quadTo(cx - 17, tearY, cx - 13, tearY + 8);
        path.quadTo(cx - 9,  tearY, cx - 13, tearY - 5);
        canvas.drawPath(path, paintTear);

        // دمعة يمنى
        path.reset();
        path.moveTo(cx + 13, tearY - 5);
        path.quadTo(cx + 17, tearY, cx + 13, tearY + 8);
        path.quadTo(cx + 9,  tearY, cx + 13, tearY - 5);
        canvas.drawPath(path, paintTear);
    }

    private void drawBubble(Canvas canvas, float cx, float cy) {
        paintBubble.setAlpha((int)(bubbleAlpha * 200));

        float bx = cx + 30;
        float by = cy - 90 - breathOffset;

        canvas.drawRoundRect(
            new RectF(bx - 5, by - 20, bx + 35, by + 5),
            10, 10, paintBubble);

        path.reset();
        path.moveTo(bx + 5,  by + 5);
        path.lineTo(bx - 5,  by + 15);
        path.lineTo(bx + 15, by + 5);
        canvas.drawPath(path, paintBubble);

        paintText.setAlpha((int)(bubbleAlpha * 255));
        canvas.drawText(bubbleText, bx + 15, by, paintText);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(120, 160);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (breathAnimator != null) breathAnimator.cancel();
        if (blinkAnimator  != null) blinkAnimator.cancel();
        if (handAnimator   != null) handAnimator.cancel();
        if (shakeAnimator  != null) shakeAnimator.cancel();
        if (jumpAnimator   != null) jumpAnimator.cancel();
        if (tearAnimator   != null) tearAnimator.cancel();
    }
}
