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

// Stop recording when stop button is clicked
public class StopRecButton extends OzoButton {
    private OzoView mStartView;
    private OzoView mRecordView;

    public StopRecButton(OzoView startView, OzoView recordView, Button button) {
        super(button);
        mStartView = startView;
        mRecordView = recordView;
    }

    @Override
    public void clicked() {
        if (mListener != null)
            mListener.onRecordStop();

        // Show initial view and hide record view
        mRecordView.toggleVisibility();
        mStartView.toggleVisibility();
    }
}
