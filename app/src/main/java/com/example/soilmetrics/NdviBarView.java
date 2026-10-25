package com.example.soilmetrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

// plant health color bar
public class NdviBarView extends View {

    private double ndvi = -1;
    private Paint barPaint = new Paint();
    private Paint dotPaint = new Paint();
    private Paint ringPaint = new Paint();
    private Paint textPaint = new Paint();
    private float density;

    public NdviBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        barPaint.setAntiAlias(true);

        dotPaint.setColor(Color.WHITE);
        dotPaint.setAntiAlias(true);

        ringPaint.setColor(Color.parseColor("#415B57"));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3 * density);
        ringPaint.setAntiAlias(true);

        textPaint.setColor(Color.parseColor("#6B807B"));
        textPaint.setTextSize(12 * density);
        textPaint.setAntiAlias(true);
    }

    // set the plant number
    public void setNdvi(double value) {
        ndvi = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = 10 * density;
        float right = getWidth() - 10 * density;
        float top = 8 * density;
        float bottom = top + 14 * density;

        // bare to green bar
        barPaint.setShader(new LinearGradient(left, 0, right, 0,
                new int[]{Color.parseColor("#DCCBA6"), Color.parseColor("#A2C1BB"), Color.parseColor("#415B57")},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(left, top, right, bottom, 7 * density, 7 * density, barPaint);

        // labels under the bar
        canvas.drawText("Bare soil", left, bottom + 18 * density, textPaint);
        String end = "Lush crop";
        canvas.drawText(end, right - textPaint.measureText(end), bottom + 18 * density, textPaint);

        // dot at the value
        if (ndvi >= 0) {
            float x = (float) (left + (right - left) * Math.min(ndvi, 1.0));
            float y = (top + bottom) / 2;
            canvas.drawCircle(x, y, 10 * density, dotPaint);
            canvas.drawCircle(x, y, 10 * density, ringPaint);
        }
    }
}
