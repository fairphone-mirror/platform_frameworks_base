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

import android.widget.SeekBar;

// Handle audio zooming
public class ZoomBar extends OzoSeekBar {
    public ZoomBar(SeekBar seekBar) {
        super(seekBar);
    }

    public void stopTracking(SeekBar seekBar, int progress) {
        mListener.onRecordSetFocusZoom(progress / 10.0);
    }
}
