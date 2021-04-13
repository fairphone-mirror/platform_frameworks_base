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

// Show level meter data in UI
public class OzoLevelMeterView extends OzoView {
    private boolean mCanShow = false;
    private boolean mDrawing = false;
    private OzoLevel mLevelView;

    public OzoLevelMeterView(OzoLevel view) {
        super(view);
        mLevelView = view;
    }

    // Element can receive level updates
    public void open() {
        mCanShow = true;
    }

    // Element no longer accepts level updates
    public void close() {
        mCanShow = false;
    }

    // Update specified level
    public void setLevel(int level) {
        if (mCanShow && !mDrawing && mLevelView != null) {
            mDrawing = true;

            mLevelView.post(new Runnable() {
                @Override
                public void run() {
                    mLevelView.setLevel(level);
                    mDrawing = false;
                }
            });
        }
    }
}
