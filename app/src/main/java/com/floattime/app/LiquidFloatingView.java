package com.floattime.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class LiquidFloatingView extends View {

    // 桃色 (251,204,199) — 默认值，可被 Prefs 覆盖
    private static final int PEACH_C = 0xFFFBFCC7;
    private static final int PEACH_D = 0xFFF6C8C0;
    private static final int PEACH_G = 0xFFF0A89F;
    // 薄荷绿 (149,197,172)
    private static final int MINT_C  = 0xFF95C5AC;
    private static final int MINT_D  = 0xFF7BB496;
    private static final int MINT_G  = 0xFF5A9A7C;

    private int idleC = PEACH_C, idleD = PEACH_D, idleG = PEACH_G;
    private int actC  = MINT_C,  actD  = MINT_D,  actG  = MINT_G;

    private int curC = PEACH_C, curD = PEACH_D, curG = PEACH_G;
    private int tgtC = PEACH_C, tgtD = PEACH_D, tgtG = PEACH_G;
    private float colorProgress = 0f;

    private float phase = 0f;
    private ValueAnimator waveAnim;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bottomReflectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glassCoverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String topText = "";
    private String bottomText = "";
    private boolean showText = false;

    // 风浪闪动
    private boolean storming = false;
    private float stormAmplitude = 0f;
    private float stormRotation = 0f;
    private float stormScale = 1f;
    private ValueAnimator stormAnim;

    private final Path wavePath = new Path();
    private final Path topArcPath = new Path();
    private final ArgbEvaluator argb = new ArgbEvaluator();

    public LiquidFloatingView(Context c) { this(c, null); }
    public LiquidFloatingView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // 圆形 outline，避免 elevation 产生方形阴影
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                int w = view.getWidth(), h = view.getHeight();
                int r = Math.min(w, h) / 2;
                outline.setOval(w / 2 - r, h / 2 - r, w / 2 + r, h / 2 + r);
            }
        });

        wavePaint1.setColor(0x33FFFFFF);
        wavePaint2.setColor(0x1AFFFFFF);

        textPaint.setColor(0xFF1F3D32);
        textPaint.setTextSize(sp(22));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setShadowLayer(4, 0, 1, 0x77FFFFFF);

        dividerPaint.setColor(0x331F3D32);
        dividerPaint.setStrokeWidth(dp(0.6f));

        waveAnim = ValueAnimator.ofFloat(0f, 1f);
        waveAnim.setDuration(5000);
        waveAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveAnim.setInterpolator(new LinearInterpolator());
        waveAnim.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
        waveAnim.start();
    }

    public void startStorm() {
        if (stormAnim != null) stormAnim.cancel();
        storming = true;
        stormAnim = ValueAnimator.ofFloat(0f, 1f);
        stormAnim.setDuration(2000);
        stormAnim.setInterpolator(new LinearInterpolator());
        stormAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            // 2 秒内做 3 次风浪摆动，振幅先增后减
            float envelope = (float) Math.sin(t * Math.PI);
            stormAmplitude = envelope * 18f;
            stormRotation = (float) Math.sin(t * Math.PI * 6) * envelope * 20f;
            stormScale = 1f + (float) Math.sin(t * Math.PI * 8) * envelope * 0.08f;
            invalidate();
            if (t >= 1f) {
                storming = false;
                stormAmplitude = 0f;
                stormRotation = 0f;
                stormScale = 1f;
                invalidate();
            }
        });
        stormAnim.start();
    }

    public void setActive(boolean active) {
        isActiveState = active;
        if (active) animateTo(actC, actD, actG);
        else        animateTo(idleC, idleD, idleG);
    }

    private boolean isActiveState = false;

    public void setIdleColors(int c, int d, int g) {
        idleC = c; idleD = d; idleG = g;
        if (!isActiveState) animateTo(idleC, idleD, idleG);
    }

    public void setActiveColors(int c, int d, int g) {
        actC = c; actD = d; actG = g;
        if (isActiveState) animateTo(actC, actD, actG);
    }

    private void animateTo(int c, int d, int g) {
        curC = ev(curC, tgtC); curD = ev(curD, tgtD); curG = ev(curG, tgtG);
        tgtC = c; tgtD = d; tgtG = g;
        colorProgress = 0f;
        ValueAnimator ca = ValueAnimator.ofFloat(0f, 1f);
        ca.setDuration(500);
        ca.addUpdateListener(a -> { colorProgress = (float) a.getAnimatedValue(); invalidate(); });
        ca.start();
    }

    public void setTopText(String t) { this.topText = t == null ? "" : t; invalidate(); }
    public void setBottomText(String t) { this.bottomText = t == null ? "" : t; invalidate(); }
    public void setShowText(boolean show) { this.showText = show; invalidate(); }

    public void setTextSizeSp(int sp) {
        textPaint.setTextSize(sp(sp));
        invalidate();
    }

    private int ev(int from, int to) { return (int) argb.evaluate(colorProgress, from, to); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float R = Math.min(w, h) / 2f - dp(6);

        // 风浪闪动：旋转 + 缩放
        if (storming) {
            canvas.save();
            canvas.rotate(stormRotation, cx, cy);
            canvas.scale(stormScale, stormScale, cx, cy);
        }

        int cc = ev(curC, tgtC);
        int cd = ev(curD, tgtD);
        int cg = ev(curG, tgtG);

        // === 1) 外阴影（径向渐变，严格收在 View 内，边缘 alpha 归零）===
        // View 半宽 halfW，圆形 R，阴影渐变半径严格 <= halfW
        float halfW = Math.min(w, h) / 2f;
        float shadowR = Math.min(halfW, R + dp(14));
        shadowPaint.setShader(new RadialGradient(cx, cy, shadowR,
                new int[]{0x00000000, 0x00000000, 0x12000000, 0x18000000, 0x0C000000, 0x00000000},
                new float[]{0.6f, 0.74f, 0.84f, 0.9f, 0.95f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, shadowR, shadowPaint);
        // 边缘淡白光晕
        float glowR = Math.min(halfW, R + dp(6));
        shadowPaint.setShader(new RadialGradient(cx, cy, glowR,
                new int[]{0x00FFFFFF, 0x00FFFFFF, 0x16FFFFFF, 0x00000000},
                new float[]{0.78f, 0.9f, 0.96f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, glowR, shadowPaint);

        // === 2) 主体（径向渐变，中心亮边缘深，像液体充盈）===
        bodyPaint.setShader(new RadialGradient(
                cx, cy - R * 0.2f, R * 1.1f,
                new int[]{cc, cd, cg},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, R, bodyPaint);

        // === 3) 液态波浪（剪切到圆内）===
        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, R, Path.Direction.CW);
        canvas.clipPath(clip);
        float extraAmp = storming ? stormAmplitude : 0f;
        drawWave(canvas, cx, cy, R, phase, 0.08f, 1.3f, R * 0.13f + extraAmp, wavePaint1);
        drawWave(canvas, cx, cy, R, phase + 0.5f, 0.28f, 2.0f, R * 0.09f + extraAmp * 0.7f, wavePaint2);
        canvas.restore();

        // ====== 玻璃罩子层（全部 SRC_ATOP 叠加在圆内）======
        canvas.save();
        Path glassClip = new Path();
        glassClip.addCircle(cx, cy, R, Path.Direction.CW);
        canvas.clipPath(glassClip);

        // === 4) 顶部双弧形高光（珠宝泛光，一大一小对应两块）===
        topArcPaint.setStyle(Paint.Style.STROKE);
        topArcPaint.setStrokeCap(Paint.Cap.ROUND);
        topArcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        // 大高光：左上弧，粗，亮（主反光），内缩不贴边
        Path bigArc = new Path();
        float bigInset = R * 0.26f;
        android.graphics.RectF bigRect = new android.graphics.RectF(
                cx - R + bigInset, cy - R + bigInset * 0.7f,
                cx + R - bigInset, cy + R - bigInset);
        bigArc.addArc(bigRect, 205, 130);
        topArcPaint.setStrokeWidth(R * 0.14f);
        topArcPaint.setShader(new LinearGradient(
                cx - R * 0.3f, cy - R, cx + R * 0.2f, cy,
                new int[]{0xDDFFFFFF, 0x88FFFFFF, 0x11FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(bigArc, topArcPaint);

        // 小高光：右下弧，细，稍暗（次反光，与大的对称呼应）
        Path smallArc = new Path();
        float smallInset = R * 0.24f;
        android.graphics.RectF smallRect = new android.graphics.RectF(
                cx - R + smallInset, cy - R + smallInset,
                cx + R - smallInset, cy + R - smallInset * 0.5f);
        smallArc.addArc(smallRect, 25, 80);
        topArcPaint.setStrokeWidth(R * 0.08f);
        topArcPaint.setShader(new LinearGradient(
                cx + R * 0.2f, cy, cx - R * 0.2f, cy + R,
                new int[]{0x00FFFFFF, 0x66FFFFFF, 0xBBFFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(smallArc, topArcPaint);

        topArcPaint.setXfermode(null);

        // === 5) 顶部大块柔光（玻璃罩整体通透感）===
        glassCoverPaint.setShader(new RadialGradient(
                cx, cy - R * 0.4f, R * 0.95f,
                new int[]{0x55FFFFFF, 0x22FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        glassCoverPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawCircle(cx, cy, R, glassCoverPaint);
        glassCoverPaint.setXfermode(null);

        // === 6) 底部反射光（玻璃罩底部环境反光）===
        bottomReflectPaint.setShader(new RadialGradient(
                cx, cy + R * 0.65f, R * 0.7f,
                new int[]{0x44FFFFFF, 0x11FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        bottomReflectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawCircle(cx, cy, R, bottomReflectPaint);
        bottomReflectPaint.setXfermode(null);

        // === 7) 边缘全反射包边（环形扫光，模拟玻璃球边缘反光）===
        rimShinePaint.setStyle(Paint.Style.STROKE);
        rimShinePaint.setStrokeWidth(dp(2.5f));
        // 用渐变让边缘上半亮、下半暗
        rimShinePaint.setShader(new LinearGradient(cx, cy - R, cx, cy + R,
                new int[]{0xBBFFFFFF, 0x66FFFFFF, 0x22FFFFFF, 0x55FFFFFF},
                new float[]{0f, 0.3f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        rimShinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawCircle(cx, cy, R - dp(1.5f), rimShinePaint);
        rimShinePaint.setXfermode(null);

        canvas.restore();
        // ====== 玻璃罩子层结束 ======

        // === 8) 文字与分割线 ===
        if (showText) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            if (!topText.isEmpty()) {
                canvas.drawText(topText, cx, cy - R * 0.28f
                        - (textPaint.descent() + textPaint.ascent()) / 2, textPaint);
            }
            if (!bottomText.isEmpty()) {
                canvas.drawText(bottomText, cx, cy + R * 0.28f
                        - (textPaint.descent() + textPaint.ascent()) / 2, textPaint);
            }
            float dl = R * 0.34f;
            canvas.drawLine(cx - dl, cy, cx + dl, cy, dividerPaint);
        }

        if (storming) {
            canvas.restore();
        }
    }

    private void drawWave(Canvas canvas, float cx, float cy, float R,
                          float ph, float yOff, float freq, float amp, Paint p) {
        wavePath.reset();
        float left = cx - R, right = cx + R;
        float baseY = cy + R * yOff;
        wavePath.moveTo(left, cy + R);
        int steps = 60;
        for (int i = 0; i <= steps; i++) {
            float x = left + (right - left) * i / steps;
            float t = (i / (float) steps) * (float) Math.PI * 2f * freq + ph * (float) Math.PI * 2f;
            wavePath.lineTo(x, baseY + amp * (float) Math.sin(t));
        }
        wavePath.lineTo(right, cy + R);
        wavePath.close();
        canvas.drawPath(wavePath, p);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
