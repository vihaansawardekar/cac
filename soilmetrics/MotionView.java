package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

// back hoof motion graph
public class MotionView extends View {

    private double[] leftLine = new double[0];
    private double[] rightLine = new double[0];
    private Paint leftPaint = new Paint();
    private Paint rightPaint = new Paint();
    private Paint gridPaint = new Paint();
    private Paint textPaint = new Paint();
    private float density;

    public MotionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        leftPaint.setColor(Color.parseColor("#415B57"));
        leftPaint.setStrokeWidth(3 * density);
        leftPaint.setStyle(Paint.Style.STROKE);
        leftPaint.setStrokeCap(Paint.Cap.ROUND);
        leftPaint.setAntiAlias(true);

        rightPaint.setColor(Color.parseColor("#E9946B"));
        rightPaint.setStrokeWidth(3 * density);
        rightPaint.setStyle(Paint.Style.STROKE);
        rightPaint.setStrokeCap(Paint.Cap.ROUND);
        rightPaint.setAntiAlias(true);

        gridPaint.setColor(Color.parseColor("#D4E6E1"));
        gridPaint.setStrokeWidth(density);

        textPaint.setColor(Color.parseColor("#6B807B"));
        textPaint.setTextSize(12 * density);
        textPaint.setAntiAlias(true);
    }

    // give graph hoof paths
    public void setData(double[] left, double[] right) {
        leftLine = left;
        rightLine = right;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = 8 * density;
        float right = getWidth() - 8 * density;
        float top = 22 * density;
        float bottom = getHeight() - 22 * density;

        // key at the top
        canvas.drawLine(left, 9 * density, left + 18 * density, 9 * density, leftPaint);
        canvas.drawText("Left back hoof", left + 24 * density, 13 * density, textPaint);
        float spot = getWidth() / 2f;
        canvas.drawLine(spot, 9 * density, spot + 18 * density, 9 * density, rightPaint);
        canvas.drawText("Right back hoof", spot + 24 * density, 13 * density, textPaint);

        canvas.drawLine(left, (top + bottom) / 2, right, (top + bottom) / 2, gridPaint);
        canvas.drawText("Time", left, getHeight() - 6 * density, textPaint);
        if (leftLine.length < 2) {
            return;
        }

        // find low and high
        double low = leftLine[0];
        double high = leftLine[0];
        for (int i = 0; i < leftLine.length; i++) {
            low = Math.min(low, Math.min(leftLine[i], rightLine[i]));
            high = Math.max(high, Math.max(leftLine[i], rightLine[i]));
        }
        if (high - low < 0.01) {
            high = low + 0.01;
        }

        drawLine(canvas, leftLine, low, high, left, right, top, bottom, leftPaint);
        drawLine(canvas, rightLine, low, high, left, right, top, bottom, rightPaint);
    }

    // draw one hoof path
    private void drawLine(Canvas canvas, double[] values, double low, double high,
                          float left, float right, float top, float bottom, Paint paint) {
        Path path = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = left + (right - left) * i / (values.length - 1);
            float y = (float) (bottom - (bottom - top) * (values[i] - low) / (high - low));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, paint);
    }
}
