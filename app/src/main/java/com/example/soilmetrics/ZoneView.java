package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

// field map of compaction
public class ZoneView extends View {

    public static final int ROWS = 4;
    public static final int COLS = 5;

    // risk for each box
    private double[][] risk = new double[ROWS][COLS];
    private boolean hasData = false;
    private Paint boxPaint = new Paint();
    private Paint textPaint = new Paint();
    private float density;

    public ZoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        boxPaint.setAntiAlias(true);
        textPaint.setColor(Color.parseColor("#25332F"));
        textPaint.setTextSize(13 * density);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    // fill in the boxes
    public void setBaseRisk(double baseRisk) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                double value = baseRisk;
                // edges are turn rows
                if (r == 0 || r == ROWS - 1 || c == 0 || c == COLS - 1) {
                    value = value + 10;
                }
                // gate gets most traffic
                if (r == ROWS - 1 && c == 0) {
                    value = value + 10;
                }
                risk[r][c] = SoilSimulator.limit(value, 0, 100);
            }
        }
        hasData = true;
        invalidate();
    }

    // count the red boxes
    public int countHigh() {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (risk[r][c] >= 60) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = 4 * density;
        float boxW = getWidth() / (float) COLS;
        float boxH = getHeight() / (float) ROWS;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                double value = risk[r][c];
                if (!hasData) {
                    boxPaint.setColor(Color.parseColor("#E2EFEC"));
                } else if (value < 35) {
                    boxPaint.setColor(Color.parseColor("#A2C1BB"));
                } else if (value < 60) {
                    boxPaint.setColor(Color.parseColor("#FFC3A1"));
                } else {
                    boxPaint.setColor(Color.parseColor("#E07A64"));
                }
                float x = c * boxW;
                float y = r * boxH;
                canvas.drawRoundRect(x + gap, y + gap, x + boxW - gap, y + boxH - gap, 10 * density, 10 * density, boxPaint);

                String label = "" + Math.round(value);
                if (r == ROWS - 1 && c == 0) {
                    label = "Gate";
                }
                if (hasData) {
                    canvas.drawText(label, x + boxW / 2, y + boxH / 2 + 5 * density, textPaint);
                }
            }
        }
    }
}
