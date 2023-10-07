package com.android.systemui.ripple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import android.content.res.TypedArray;
import com.android.systemui.R;
import android.util.Log;
import android.view.MotionEvent;
import android.provider.Settings;
import android.os.UserHandle;
import android.content.Intent;

/**
 * author : suntianhai
 * e-mail : tianhai.sun@t2mobile.com
 * time   : 2023/09/04
 * desc   : add for FP5-2640 pocket mode
 * version: 1.0
 */
public class SpreadView extends View {

    private Paint centerPaint;
    private int radius = 100;
    private Paint spreadPaint;
    private float centerX;
    private float centerY;
    private int distance = 3;
    private int maxRadius = 50;
    private int delayMilliseconds = 100;
    private List<Integer> spreadRadius = new ArrayList<>();
    private List<Integer> alphas = new ArrayList<>();

    public SpreadView(Context context) {
        this(context,null,0);
    }

    public SpreadView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SpreadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpreadView, defStyleAttr, 0);
        radius = a.getInt(R.styleable.SpreadView_spread_radius, radius);
        maxRadius = a.getInt(R.styleable.SpreadView_spread_max_radius, maxRadius);
        int centerColor = a.getColor(R.styleable.SpreadView_spread_center_color, android.R.attr.colorAccent);
        int spreadColor = a.getColor(R.styleable.SpreadView_spread_spread_color, android.R.attr.colorAccent);
        distance = a.getInt(R.styleable.SpreadView_spread_distance, distance);
        a.recycle();
        centerPaint = new Paint();
        centerPaint.setColor(centerColor);
        centerPaint.setAntiAlias(true);

        alphas.add(255);
        spreadRadius.add(0);
        spreadPaint = new Paint();
        spreadPaint.setAntiAlias(true);
        spreadPaint.setAlpha(255);
        spreadPaint.setColor(spreadColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < spreadRadius.size(); i++) {
        int alpha = alphas.get(i);
        spreadPaint.setAlpha(alpha);

        int width = spreadRadius.get(i);
        canvas.drawCircle(centerX, centerY, radius + width, spreadPaint);

        if (alpha > 0 && width < 200) {
            alpha = (alpha - distance) > 0 ? (alpha - distance) : 1;
            alphas.set(i, alpha);
            spreadRadius.set(i, width + distance);
            }
        }
        if (spreadRadius.get(spreadRadius.size() - 1) > maxRadius) {
            spreadRadius.add(0);
            alphas.add(255);
        }
        if (spreadRadius.size()  > 2) {
            alphas.remove(0);
            spreadRadius.remove(0);
        }
        canvas.drawCircle(centerX, centerY, radius, centerPaint);
        postInvalidateDelayed(delayMilliseconds);
    }

    private int lastX;
    private int lastY;
    private boolean closePS = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
                if(closePS){
                    closePS = false;
                    layout(0 , 2035, 1080 , 2340);
                    Settings.Global.putStringForUser(getContext().getContentResolver(),
                    Settings.Global.UPDATE_POCKET_MODE_UI, System.currentTimeMillis() + "",
                    UserHandle.myUserId());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int offsetX = x - lastX;
                int offsetY = y - lastY;
                layout(getLeft() + offsetX, getTop(), getRight() + offsetX, getBottom());

                if(Math.abs(offsetX) > 20){
                    closePS = true;
                }
            break;
        }
        return true;
    }

}