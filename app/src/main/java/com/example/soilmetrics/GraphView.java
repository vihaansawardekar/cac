package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

// simple line graph
public class GraphView extends View {

    private ArrayList<Double> values = new ArrayList<>();
    private ArrayList<String> labels = new ArrayList<>();
    private Paint linePaint = new Paint();
    private Paint fillPaint = new Paint();
    private Paint dotPaint = new Paint();
    private Paint holePaint = new Paint();
    private Paint textPaint = new Paint();
    private Paint gridPaint = new Paint();
    private float density;

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        linePaint.setColor(Color.parseColor("#415B57"));
        linePaint.setStrokeWidth(3 * density);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setAntiAlias(true);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        dotPaint.setColor(Color.parseColor("#415B57"));
        dotPaint.setAntiAlias(true);

        holePaint.setColor(Color.parseColor("#FFC3A1"));
        holePaint.setAntiAlias(true);

        textPaint.setColor(Color.parseColor("#6B807B"));
        textPaint.setTextSize(11 * density);
        textPaint.setAntiAlias(true);

        gridPaint.setColor(Color.parseColor("#D4E6E1"));
        gridPaint.setStrokeWidth(density);
    }

    // give graph new numbers
    public void setData(ArrayList<Double> newValues, ArrayList<String> newLabels) {
        values = newValues;
        labels = newLabels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = 40 * density;
        float right = getWidth() - 14 * density;
        float top = 14 * density;
        float bottom = getHeight() - 26 * density;

        if (values.size() == 0) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Your soil tests will show here", getWidth() / 2f, getHeight() / 2f, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        // find low and high
        double low = values.get(0);
        double high = values.get(0);
        for (int i = 0; i < values.size(); i++) {
            low = Math.min(low, values.get(i));
            high = Math.max(high, values.get(i));
        }
        low = low - 0.5;
        high = high + 0.5;
        if (low < 0) {
            low = 0;
        }

        // three grid lines
        for (int i = 0; i < 3; i++) {
            float y = top + (bottom - top) * i / 2;
            canvas.drawLine(left, y, right, y, gridPaint);
            double number = high - (high - low) * i / 2;
            canvas.drawText(String.format("%.1f%%", number), 2 * density, y + 4 * density, textPaint);
        }

        // find each point
        float[] xs = new float[values.size()];
        float[] ys = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            if (values.size() == 1) {
                xs[i] = (left + right) / 2;
            } else {
                xs[i] = left + (right - left) * i / (values.size() - 1);
            }
            ys[i] = (float) (bottom - (bottom - top) * (values.get(i) - low) / (high - low));
        }

        // glow under the line
        Path line = new Path();
        Path fill = new Path();
        line.moveTo(xs[0], ys[0]);
        fill.moveTo(xs[0], bottom);
        fill.lineTo(xs[0], ys[0]);
        for (int i = 1; i < values.size(); i++) {
            line.lineTo(xs[i], ys[i]);
            fill.lineTo(xs[i], ys[i]);
        }
        fill.lineTo(xs[values.size() - 1], bottom);
        fill.close();
        fillPaint.setShader(new LinearGradient(0, top, 0, bottom,
                Color.parseColor("#66A2C1BB"), Color.parseColor("#00A2C1BB"), Shader.TileMode.CLAMP));
        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(line, linePaint);

        // dots with peach centers
        for (int i = 0; i < values.size(); i++) {
            canvas.drawCircle(xs[i], ys[i], 6 * density, dotPaint);
            canvas.drawCircle(xs[i], ys[i], 3 * density, holePaint);
        }

        // first and last label
        if (labels.size() > 0) {
            canvas.drawText(labels.get(0), left, bottom + 18 * density, textPaint);
            if (labels.size() > 1) {
                String lastLabel = labels.get(labels.size() - 1);
                float width = textPaint.measureText(lastLabel);
                canvas.drawText(lastLabel, right - width, bottom + 18 * density, textPaint);
            }
        }
    }
}
