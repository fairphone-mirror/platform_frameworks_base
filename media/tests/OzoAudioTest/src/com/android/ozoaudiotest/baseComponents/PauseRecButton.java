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

import android.widget.Button;

// Pause/resume recording when pause button is clicked
public class PauseRecButton extends OzoButton {
    private boolean mPaused = false;
    private OzoView mPausedView;

    public PauseRecButton(Button button, OzoView pausedView) {
        super(button);
        mPausedView = pausedView;
    }

    @Override
    public void clicked() {
        mPaused = !mPaused;

        if (mPaused) {
            if (mListener != null)
                mListener.onRecordPause();

            mPausedView.show();
        } else {
            if (mListener != null)
                mListener.onRecordResume();

            mPausedView.hide();
        }
    }

    @Override
    public void reset() {
        mPausedView.hide();
    }
}
