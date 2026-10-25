package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

// round number meter
public class GaugeView extends View {

    // meter range and words
    private double low = 0;
    private double high = 8.0;
    private String lowText = "0%";
    private String highText = "8%";
    private String unit = "%";
    private int startColor = Color.parseColor("#A2C1BB");
    private int endColor = Color.parseColor("#FFC3A1");

    private double value = 0;
    private boolean hasValue = false;
    private String label = "No tests yet";
    private Paint trackPaint = new Paint();
    private Paint valuePaint = new Paint();
    private Paint bigPaint = new Paint();
    private Paint smallPaint = new Paint();
    private float density;

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        trackPaint.setColor(Color.parseColor("#33FFFFFF"));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(16 * density);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setAntiAlias(true);

        valuePaint.setStyle(Paint.Style.STROKE);
        valuePaint.setStrokeWidth(16 * density);
        valuePaint.setStrokeCap(Paint.Cap.ROUND);
        valuePaint.setAntiAlias(true);

        bigPaint.setColor(Color.WHITE);
        bigPaint.setTextSize(44 * density);
        bigPaint.setFakeBoldText(true);
        bigPaint.setTextAlign(Paint.Align.CENTER);
        bigPaint.setAntiAlias(true);

        smallPaint.setColor(Color.parseColor("#FFC3A1"));
        smallPaint.setTextSize(15 * density);
        smallPaint.setTextAlign(Paint.Align.CENTER);
        smallPaint.setAntiAlias(true);
    }

    // change meter range
    public void setRange(double lowNumber, double highNumber, String unitText) {
        low = lowNumber;
        high = highNumber;
        unit = unitText;
        lowText = Math.round(lowNumber) + unitText;
        highText = Math.round(highNumber) + unitText;
        invalidate();
    }

    // change meter colors
    public void setColors(int start, int end) {
        startColor = start;
        endColor = end;
        invalidate();
    }

    // show a new number
    public void setValue(double om, String words) {
        value = om;
        label = words;
        hasValue = true;
        invalidate();
    }

    // show empty meter
    public void clear(String words) {
        hasValue = false;
        label = words;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f + 8 * density;
        float radius = Math.min(getWidth(), getHeight()) / 2f - 14 * density;
        RectF box = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // gray track first
        canvas.drawArc(box, 135, 270, false, trackPaint);

        // colored part on top
        if (hasValue) {
            valuePaint.setShader(new LinearGradient(box.left, 0, box.right, 0,
                    startColor, endColor, Shader.TileMode.CLAMP));
            double part = (Math.min(value, high) - low) / (high - low);
            float sweep = (float) (270 * Math.max(part, 0));
            if (sweep < 2) {
                sweep = 2;
            }
            canvas.drawArc(box, 135, sweep, false, valuePaint);
        }

        // numbers at both ends
        float endY = centerY + radius * 0.71f + 30 * density;
        canvas.drawText(lowText, centerX - radius * 0.71f, endY, smallPaint);
        canvas.drawText(highText, centerX + radius * 0.71f, endY, smallPaint);

        // words in the middle
        String big = "--";
        if (hasValue) {
            big = String.format("%.1f", value) + unit;
        }
        canvas.drawText(big, centerX, centerY + 10 * density, bigPaint);
        canvas.drawText(label, centerX, centerY + 36 * density, smallPaint);
    }
}
