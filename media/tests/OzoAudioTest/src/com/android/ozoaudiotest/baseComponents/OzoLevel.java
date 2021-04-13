/*
Copyright (C) 2019 Nokia Corporation.
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
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.View;


public final class OzoLevel extends View {
    private ShapeDrawable mDrawable;
    private double mLevel = 0;

    private final int[] segmentColors = {
        0xff5555ff,
        0xff5555ff,
        0xff00ff00,
        0xff00ff00,
        0xff00ff00,
        0xff00ff00,
        0xffffff00,
        0xffffff00,
        0xffff0000,
        0xffff0000
    };
    private final int segmentOffColor = 0xff555555;
    private final int PADDING = 2;
    private final int HEIGHT = 50;
    private int mScale = 100;

    public OzoLevel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OzoLevel(Context context) {
        super(context);
    }

    public void setLevel(double level) {
        mLevel = level / mScale; // Normalize
        invalidate();
    }

    public void setScale(int scale) {
        mScale = scale;
    }

    private void drawLevel(Canvas canvas) {
        int x = 0;
        double level = mLevel * segmentColors.length;
        int width = (int) (Math.floor(getWidth() / segmentColors.length)) - (2 * PADDING);

        mDrawable = new ShapeDrawable(new RectShape());
        for (int i = 0; i < segmentColors.length; i++) {
            x += PADDING;
            int color = (level > (i + 0.5)) ? segmentColors[i] : segmentOffColor;

            mDrawable.getPaint().setColor(color);
            mDrawable.setBounds(x, 0, x + width, HEIGHT);
            mDrawable.draw(canvas);
            x = x + width + PADDING;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawLevel(canvas);
    }
}
