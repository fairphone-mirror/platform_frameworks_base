/*
Copyright (C) 2020 Nokia Corporation.
This material, including documentation and any related
computer programs, is protected by copyright controlled by
Nokia Corporation. All rights are reserved. Copying,
including reproducing, storing, adapting or translating, any
or all of this material requires the prior written consent of
Nokia Corporation. This material also contains confidential
information which may not be disclosed to others without the
prior written consent of Nokia Corporation.
*/

package com.android.ozoaudiotest;

import android.content.Context;
import android.util.AttributeSet;

import android.view.View;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class OzoCircle extends View implements OnGestureListener {

    interface OzoCircleListener {
        // New azimuth angle has been set
        public void onAngle(double angle);
    }

    private GestureDetector mGestureDetector;
    private OzoCircleListener mListener;

    private float mUserPicCenterX = 200;
    private float mUserPicCenterY = 200;
    private int mBorderRadius = 150;

    private float mUserPicBorderCenterX;
    private float mUserPicBorderCenterY;
    private Paint mVisiblePaint;
    private Paint mVisibleMessageCountPaint;
    private int mVisibleMessageCountRadius;

    private double mOutAngle;

    public OzoCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setUp();
    }

    public OzoCircle(Context context) {
        super(context);
        this.setUp();
    }

    private void setUp() {
        createVisiblePaint();
        updatePosition(270); // Initial angle
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    public void setListener(OzoCircleListener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(mUserPicCenterX, mUserPicCenterY, mBorderRadius, mVisiblePaint);
        canvas.drawCircle(mUserPicBorderCenterX, mUserPicBorderCenterY, mVisibleMessageCountRadius, mVisibleMessageCountPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // When user stops moving the angle send event to any listener(s) regarding the selected angle
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mListener != null)
                mListener.onAngle(mOutAngle);
        }

        if (mGestureDetector.onTouchEvent(event))
            return true;
        else
            return super.onTouchEvent(event);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float x = e2.getX() / ((float) getWidth());
        float y = e2.getY() / ((float) getHeight());
        float rotDegrees = cartesianToPolar(1 - x, 1 - y); // 1- to correct our custom axis direction

        if (! Float.isNaN(rotDegrees)) {
            float posDegrees = rotDegrees;
            if (rotDegrees < 0) posDegrees = 360 + rotDegrees;

            posDegrees = (posDegrees - 90) % 360;
            setPosAngle(posDegrees);
            return true;
        }

        return false;
    }

    /**
     * Set circle angle.
     *
     * @param angle New angle
     */
    public void setAngle(int angle) {
        angle = (angle + 270) % 360;
        updatePosition(angle);
    }

    private void updatePosition(double angle) {
        angle = Math.toRadians(angle);
        mUserPicBorderCenterX = (float) (mUserPicCenterX + Math.cos(angle) * mBorderRadius);
        mUserPicBorderCenterY = (float) (mUserPicCenterY + Math.sin(angle) * mBorderRadius);
        invalidate();
    }

    private void createVisiblePaint() {
        mVisiblePaint = new Paint();
        mVisiblePaint.setAntiAlias(true);
        mVisiblePaint.setFilterBitmap(true);
        mVisiblePaint.setDither(true);
        mVisiblePaint.setColor(Color.parseColor("#F85A74"));
        mVisiblePaint.setStyle(Paint.Style.STROKE);
        mVisiblePaint.setStrokeWidth(14f);

        mVisibleMessageCountPaint = new Paint();
        mVisibleMessageCountPaint.setAntiAlias(true);
        mVisibleMessageCountPaint.setFilterBitmap(true);
        mVisibleMessageCountPaint.setDither(true);
        mVisibleMessageCountPaint.setColor(Color.parseColor("#F85A74"));

        mVisibleMessageCountRadius = mBorderRadius / 6;
    }

    private float cartesianToPolar(float x, float y) {
        return (float) -Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
    }

    private void setPosAngle(float deg) {
        // Convert to external angle representation
        //
        // Angle 0 is north
        // Clockwise angles are negative (0 -> -180)
        // Anti-clockwise angles are positive (0 -> 180)
        float deg2 = deg;
        if (deg2 >= -90 && deg2 <= 90)
            deg2 = -(deg2 + 90);
        else {
            deg2 = 270 - deg2;
        }

        mOutAngle = deg2;
        updatePosition(deg);
    }

    public boolean onDown(MotionEvent event) { return true; }

    public boolean onSingleTapUp(MotionEvent e) { return true; }

    public void onShowPress(MotionEvent e) { }

    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) { return false; }

    public void onLongPress(MotionEvent e) { }
}
