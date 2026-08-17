package com.tyler.weighttracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class WaveBackgroundView extends View {
    private Paint paint;
    private float waveOffset = 0f;

    public WaveBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // shift wave offset for fluid motion
        waveOffset += 0.02f;
        float shift = (float) Math.sin(waveOffset) * 200f;

        Shader shader = new LinearGradient(
                0, 0 + shift, getWidth(), getHeight() + shift,
                new int[]{0xFF11111B, 0xFF313244, 0xFF181825},
                null, Shader.TileMode.MIRROR
        );

        paint.setShader(shader);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        // keep loop running
        invalidate();
    }
}