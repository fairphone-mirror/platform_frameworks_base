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

import android.view.View;
import android.widget.SeekBar;

abstract class OzoSeekBar extends OzoView {
    protected SeekBar mSeekBar;

    public OzoSeekBar(SeekBar seekBar) {
        super(seekBar);

        mSeekBar = seekBar;

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {
                stopTracking(seekBar, seekBar.getProgress());
            }
        });
    }

    public void reset(int value) {
        mSeekBar.setProgress(value);
    }

    abstract public void stopTracking(SeekBar seekBar, int progress);
}
