package com.floattime.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HsvColorPickerView extends View {

    private float hue = 0f;      // 0..360
    private float sat = 1f;      // 0..1
    private float val = 1f;      // 0..1

    private final Paint huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint satValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float hueBarTop, hueBarBottom, hueBarLeft, hueBarRight;
    private float svLeft, svTop, svRight, svBottom;

    private static final float HUE_BAR_H = 36f;
    private static final float GAP = 14f;

    public HsvColorPickerView(Context c) { this(c, null); }
    public HsvColorPickerView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setColor(int argb) {
        float[] hsv = new float[3];
        Color.colorToHSV(argb, hsv);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2];
        invalidate();
    }

    public int getColor() {
        float[] hsv = {hue, sat, val};
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = dp(6);
        float width = w - pad * 2;
        hueBarLeft = pad;
        hueBarRight = pad + width;
        hueBarTop = pad;
        hueBarBottom = pad + dp(HUE_BAR_H);
        svLeft = pad;
        svRight = pad + width;
        svTop = hueBarBottom + dp(GAP);
        svBottom = h - pad;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();

        // 色相条：横向彩虹渐变
        huePaint.setShader(new LinearGradient(hueBarLeft, 0, hueBarRight, 0,
                new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(hueBarLeft, hueBarTop, hueBarRight, hueBarBottom, huePaint);

        // 色相指示器
        float hueX = hueBarLeft + (hue / 360f) * (hueBarRight - hueBarLeft);
        thumbPaint.setStyle(android.graphics.Paint.Style.STROKE);
        thumbPaint.setStrokeWidth(dp(2.5f));
        thumbPaint.setColor(0xFFFFFFFF);
        thumbPaint.setShadowLayer(4, 0, 0, 0x88000000);
        canvas.drawRect(hueX - dp(3), hueBarTop - dp(2), hueX + dp(3), hueBarBottom + dp(2), thumbPaint);

        // 饱和度-明度方块
        // 底色：横向饱和度渐变（左白右纯色），纵向明度渐变（上透明下黑）
        int pureHue = Color.HSVToColor(new float[]{hue, 1f, 1f});
        satValPaint.setShader(new LinearGradient(svLeft, svTop, svRight, svTop,
                Color.WHITE, pureHue, Shader.TileMode.CLAMP));
        canvas.drawRect(svLeft, svTop, svRight, svBottom, satValPaint);

        Paint valPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valPaint.setShader(new LinearGradient(svLeft, svTop, svLeft, svBottom,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
        canvas.drawRect(svLeft, svTop, svRight, svBottom, valPaint);

        // 选点指示器
        float px = svLeft + sat * (svRight - svLeft);
        float py = svTop + (1f - val) * (svBottom - svTop);
        thumbPaint.setStyle(android.graphics.Paint.Style.STROKE);
        thumbPaint.setStrokeWidth(dp(2f));
        thumbPaint.setColor(0xFFFFFFFF);
        thumbPaint.setShadowLayer(6, 0, 0, 0xAA000000);
        canvas.drawCircle(px, py, dp(9), thumbPaint);

        // 当前色预览圆
        circlePaint.setStyle(android.graphics.Paint.Style.FILL);
        circlePaint.setColor(getColor());
        float cvR = dp(12);
        float cvX = svRight - cvR - dp(6);
        float cvY = svTop + cvR + dp(6);
        canvas.drawCircle(cvX, cvY, cvR, circlePaint);
        thumbPaint.setStyle(android.graphics.Paint.Style.STROKE);
        thumbPaint.setStrokeWidth(dp(1.2f));
        thumbPaint.setColor(0x66000000);
        thumbPaint.clearShadowLayer();
        canvas.drawCircle(cvX, cvY, cvR, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {
            if (y >= hueBarTop - dp(8) && y <= hueBarBottom + dp(8)) {
                hue = clamp01((x - hueBarLeft) / (hueBarRight - hueBarLeft)) * 360f;
                invalidate();
                return true;
            } else if (y >= svTop && y <= svBottom && x >= svLeft && x <= svRight) {
                sat = clamp01((x - svLeft) / (svRight - svLeft));
                val = 1f - clamp01((y - svTop) / (svBottom - svTop));
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
