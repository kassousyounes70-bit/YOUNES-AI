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
import android.view.animation.SinInterpolator;

public class NpcView extends View {

    // ── حالات الشخصية ──
    public static final int STATE_IDLE     = 0; // استراحة
    public static final int STATE_THINKING = 1; // تفكير
    public static final int STATE_TYPING   = 2; // كتابة
    public static final int STATE_HAPPY    = 3; // فرح
    public static final int STATE_SUCCESS  = 4; // نجاح

    private int   currentState   = STATE_IDLE;
    private float breathOffset   = 0f; // حركة التنفس
    private float blinkProgress  = 1f; // تطرف العيون
    private float handOffset     = 0f; // حركة اليدين
    private float bubbleAlpha    = 0f; // فقاعة الكلام
    private float moodProgress   = 0f; // تعبير الوجه

    // ── Animators ──
    private ValueAnimator breathAnimator;
    private ValueAnimator blinkAnimator;
    private ValueAnimator handAnimator;
    private ValueAnimator moodAnimator;

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
        // ── ألوان الشخصية ──
        paintSkin.setColor(0xFFFFCC99);
        paintDark.setColor(0xFF1A1A2E);
        paintWhite.setColor(Color.WHITE);
        paintGreen.setColor(0xFF3FB950);
        paintHair.setColor(0xFF2C1810);
        paintClothes.setColor(0xFF238636);

        // ── ظل ──
        paintShadow.setColor(0x33000000);
        paintShadow.setMaskFilter(
            new android.graphics.BlurMaskFilter(
                12f, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // ── فقاعة الكلام ──
        paintBubble.setColor(0xFF21262D);
        paintBubble.setStyle(Paint.Style.FILL);

        // ── نص الفقاعة ──
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(18f);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setAntiAlias(true);

        startAnimations();
    }

    // ── بدء الحركات الأساسية ──
    private void startAnimations() {
        // حركة التنفس
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

        // تطرف العيون
        blinkAnimator = ValueAnimator.ofFloat(1f, 0f, 1f);
        blinkAnimator.setDuration(200);
        blinkAnimator.setStartDelay(3000);
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnimator.setRepeatMode(ValueAnimator.RESTART);
        blinkAnimator.addUpdateListener(a -> {
            blinkProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        blinkAnimator.start();
    }

    // ── تغيير حالة الشخصية ──
    public void setState(int state) {
        if (currentState == state) return;
        currentState = state;

        stopStateAnimations();

        switch (state) {
            case STATE_THINKING:
                bubbleText = "🤔";
                startBubbleAnimation();
                startHandAnimation(0.5f, 800);
                break;

            case STATE_TYPING:
                bubbleText = "✍️";
                startBubbleAnimation();
                startHandAnimation(4f, 150);
                break;

            case STATE_HAPPY:
                bubbleText = "😊";
                startBubbleAnimation();
                startMoodAnimation();
                break;

            case STATE_SUCCESS:
                bubbleText = "✅";
                startBubbleAnimation();
                startMoodAnimation();
                break;

            case STATE_IDLE:
            default:
                bubbleText = "";
                bubbleAlpha = 0f;
                handOffset  = 0f;
                moodProgress = 0f;
                break;
        }
        invalidate();
    }

    private void stopStateAnimations() {
        if (handAnimator != null) handAnimator.cancel();
        if (moodAnimator != null) moodAnimator.cancel();
    }

    private void startBubbleAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(300);
        anim.addUpdateListener(a -> {
            bubbleAlpha = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    private void startHandAnimation(float amplitude, long duration) {
        handAnimator = ValueAnimator.ofFloat(-amplitude, amplitude);
        handAnimator.setDuration(duration);
        handAnimator.setRepeatCount(ValueAnimator.INFINITE);
        handAnimator.setRepeatMode(ValueAnimator.REVERSE);
        handAnimator.addUpdateListener(a -> {
            handOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        handAnimator.start();
    }

    private void startMoodAnimation() {
        moodAnimator = ValueAnimator.ofFloat(0f, 1f);
        moodAnimator.setDuration(500);
        moodAnimator.addUpdateListener(a -> {
            moodProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        moodAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w  = getWidth();
        float h  = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        // ── ظل تحت الشخصية ──
        canvas.drawOval(
            new RectF(cx - 40, h - 15, cx + 40, h - 5),
            paintShadow);

        // ── الجسم (من الصدر للأعلى فقط) ──
        drawBody(canvas, cx, cy);

        // ── الرقبة ──
        drawNeck(canvas, cx, cy);

        // ── الرأس ──
        drawHead(canvas, cx, cy);

        // ── الشعر ──
        drawHair(canvas, cx, cy);

        // ── الوجه ──
        drawFace(canvas, cx, cy);

        // ── اليدان ──
        drawHands(canvas, cx, cy);

        // ── فقاعة الكلام ──
        if (bubbleAlpha > 0 && !bubbleText.isEmpty()) {
            drawBubble(canvas, cx, cy);
        }
    }

    private void drawBody(Canvas canvas, float cx, float cy) {
        // الجسم — مستطيل مدوّر
        paintClothes.setColor(0xFF238636);
        RectF body = new RectF(
            cx - 35, cy - breathOffset,
            cx + 35, cy + 60 - breathOffset);
        canvas.drawRoundRect(body, 15, 15, paintClothes);

        // تفاصيل الملابس — خط عمودي
        paintDark.setAlpha(60);
        canvas.drawLine(cx, cy + 5 - breathOffset,
                        cx, cy + 55 - breathOffset, paintDark);
        paintDark.setAlpha(255);

        // ياقة
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
        paintSkin.setColor(0xFFFFCC99);
        // الرأس
        canvas.drawCircle(
            cx, cy - 50 - breathOffset, 38, paintSkin);

        // احمرار الخدين
        Paint blush = new Paint(Paint.ANTI_ALIAS_FLAG);
        blush.setColor(0x44FF9999);
        canvas.drawCircle(cx - 22, cy - 42 - breathOffset, 10, blush);
        canvas.drawCircle(cx + 22, cy - 42 - breathOffset, 10, blush);
    }

    private void drawHair(Canvas canvas, float cx, float cy) {
        paintHair.setColor(0xFF2C1810);

        // شعر علوي
        RectF hairTop = new RectF(
            cx - 38, cy - 90 - breathOffset,
            cx + 38, cy - 40 - breathOffset);
        canvas.drawArc(hairTop, 180, 180, true, paintHair);

        // شعر جانبي أيسر
        path.reset();
        path.moveTo(cx - 38, cy - 55 - breathOffset);
        path.lineTo(cx - 48, cy - 25 - breathOffset);
        path.lineTo(cx - 30, cy - 20 - breathOffset);
        canvas.drawPath(path, paintHair);

        // شعر جانبي أيمن
        path.reset();
        path.moveTo(cx + 38, cy - 55 - breathOffset);
        path.lineTo(cx + 48, cy - 25 - breathOffset);
        path.lineTo(cx + 30, cy - 20 - breathOffset);
        canvas.drawPath(path, paintHair);
    }

    private void drawFace(Canvas canvas, float cx, float cy) {
        float eyeY = cy - 52 - breathOffset;

        // ── العيون ──
        paintDark.setColor(0xFF1A1A2E);
        paintDark.setAlpha(255);

        // بياض العين اليسرى
        canvas.drawCircle(cx - 13, eyeY, 9, paintWhite);
        // بياض العين اليمنى
        canvas.drawCircle(cx + 13, eyeY, 9, paintWhite);

        // حدقة العين (مع تأثير التطرف)
        float eyeH = 9f * blinkProgress;
        if (eyeH < 1f) eyeH = 1f;

        // عين يسرى
        RectF leftEye = new RectF(
            cx - 18, eyeY - eyeH,
            cx - 8,  eyeY + eyeH);
        canvas.drawOval(leftEye, paintDark);

        // عين يمنى
        RectF rightEye = new RectF(
            cx + 8,  eyeY - eyeH,
            cx + 18, eyeY + eyeH);
        canvas.drawOval(rightEye, paintDark);

        // بريق العين
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
        if (currentState == STATE_THINKING) {
            // حاجب متقطب للتفكير
            canvas.drawLine(cx - 20, browY + 3,
                           cx - 8,  browY, browPaint);
            canvas.drawLine(cx + 8,  browY,
                           cx + 20, browY + 3, browPaint);
        } else if (currentState == STATE_HAPPY
                || currentState == STATE_SUCCESS) {
            // حاجب مرفوع للفرح
            canvas.drawLine(cx - 20, browY - 3,
                           cx - 8,  browY - 5, browPaint);
            canvas.drawLine(cx + 8,  browY - 5,
                           cx + 20, browY - 3, browPaint);
        } else {
            // حاجب عادي
            canvas.drawLine(cx - 20, browY,
                           cx - 8,  browY, browPaint);
            canvas.drawLine(cx + 8,  browY,
                           cx + 20, browY, browPaint);
        }

        // ── الفم ──
        Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthPaint.setColor(0xFFCC6666);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(3f);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);

        float mouthY = cy - 30 - breathOffset;

        if (currentState == STATE_HAPPY
                || currentState == STATE_SUCCESS) {
            // ابتسامة كبيرة
            RectF smile = new RectF(
                cx - 15, mouthY - 5,
                cx + 15, mouthY + 10);
            canvas.drawArc(smile, 0, 180, false, mouthPaint);

            // أسنان
            mouthPaint.setColor(Color.WHITE);
            mouthPaint.setStyle(Paint.Style.FILL);
            RectF teeth = new RectF(
                cx - 12, mouthY,
                cx + 12, mouthY + 7);
            canvas.drawRoundRect(teeth, 3, 3, mouthPaint);

        } else if (currentState == STATE_THINKING) {
            // فم محايد مع انحناء خفيف
            path.reset();
            path.moveTo(cx - 12, mouthY + 3);
            path.quadTo(cx, mouthY - 2, cx + 12, mouthY + 3);
            canvas.drawPath(path, mouthPaint);

        } else if (currentState == STATE_TYPING) {
            // فم مفتوح قليلاً
            mouthPaint.setStyle(Paint.Style.FILL);
            mouthPaint.setColor(0xFFCC6666);
            canvas.drawOval(new RectF(
                cx - 8, mouthY - 3,
                cx + 8, mouthY + 5), mouthPaint);

        } else {
            // ابتسامة هادئة
            RectF smile = new RectF(
                cx - 10, mouthY - 3,
                cx + 10, mouthY + 7);
            canvas.drawArc(smile, 0, 180, false, mouthPaint);
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
    }

    private void drawHands(Canvas canvas, float cx, float cy) {
        paintSkin.setColor(0xFFFFCC99);
        paintSkin.setStyle(Paint.Style.FILL);

        // ── اليد اليسرى ──
        float leftHandX = cx - 55;
        float leftHandY = cy + 10 - breathOffset + handOffset;

        // ذراع يسرى
        path.reset();
        path.moveTo(cx - 35, cy + 5 - breathOffset);
        path.quadTo(cx - 45, cy + 10 - breathOffset,
                    leftHandX, leftHandY);
        Paint armPaint = new Paint(paintSkin);
        armPaint.setStyle(Paint.Style.STROKE);
        armPaint.setStrokeWidth(14f);
        armPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(path, armPaint);

        // كف يسرى
        canvas.drawCircle(leftHandX, leftHandY, 10, paintSkin);

        // أصابع يسرى
        for (int i = 0; i < 4; i++) {
            float fx = leftHandX - 12 + (i * 8);
            float fy = leftHandY - 8;
            canvas.drawRoundRect(
                new RectF(fx - 3, fy - 10, fx + 3, fy),
                3, 3, paintSkin);
        }

        // ── اليد اليمنى ──
        float rightHandX = cx + 55;
        float rightHandY = cy + 10 - breathOffset - handOffset;

        // ذراع يمنى
        path.reset();
        path.moveTo(cx + 35, cy + 5 - breathOffset);
        path.quadTo(cx + 45, cy + 10 - breathOffset,
                    rightHandX, rightHandY);
        canvas.drawPath(path, armPaint);

        // كف يمنى
        canvas.drawCircle(rightHandX, rightHandY, 10, paintSkin);

        // أصابع يمنى
        for (int i = 0; i < 4; i++) {
            float fx = rightHandX - 12 + (i * 8);
            float fy = rightHandY - 8;
            canvas.drawRoundRect(
                new RectF(fx - 3, fy - 10, fx + 3, fy),
                3, 3, paintSkin);
        }

        // ── إضافة لوحة مفاتيح عند الكتابة ──
        if (currentState == STATE_TYPING) {
            drawKeyboard(canvas, cx, cy);
        }
    }

    private void drawKeyboard(Canvas canvas, float cx, float cy) {
        Paint kbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kbPaint.setColor(0xFF30363D);

        float kbY = cy + 45 - breathOffset;
        RectF kb = new RectF(cx - 45, kbY, cx + 45, kbY + 20);
        canvas.drawRoundRect(kb, 4, 4, kbPaint);

        // مفاتيح صغيرة
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

    private void drawBubble(Canvas canvas, float cx, float cy) {
        paintBubble.setAlpha((int)(bubbleAlpha * 200));

        float bx = cx + 30;
        float by = cy - 90 - breathOffset;

        // فقاعة
        RectF bubble = new RectF(bx - 5, by - 20, bx + 35, by + 5);
        canvas.drawRoundRect(bubble, 10, 10, paintBubble);

        // ذيل الفقاعة
        path.reset();
        path.moveTo(bx + 5,  by + 5);
        path.lineTo(bx - 5,  by + 15);
        path.lineTo(bx + 15, by + 5);
        canvas.drawPath(path, paintBubble);

        // نص الفقاعة
        paintText.setAlpha((int)(bubbleAlpha * 255));
        canvas.drawText(bubbleText, bx + 15, by, paintText);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // حجم ثابت 120x160
        setMeasuredDimension(120, 160);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (breathAnimator != null) breathAnimator.cancel();
        if (blinkAnimator  != null) blinkAnimator.cancel();
        if (handAnimator   != null) handAnimator.cancel();
        if (moodAnimator   != null) moodAnimator.cancel();
    }
}
