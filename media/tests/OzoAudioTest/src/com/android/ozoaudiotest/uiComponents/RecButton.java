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

// Start recording when record button is clicked
public class RecButton extends OzoButton {
    private RecordFactoryInterface mMediaFactory;
    private OzoView mStartView;
    private OzoView mRecordView;
    private OzoView mOzoRecView;
    private OzoOptionSettings mSettings;

    public RecButton(RecordFactoryInterface recordInterface, OzoView startView, OzoView recordView,
        OzoView ozoRecView, OzoOptionSettings settings, Button button) {
        super(button);
        mMediaFactory = recordInterface;
        mStartView = startView;
        mRecordView = recordView;
        mOzoRecView = ozoRecView;
        mSettings = settings;
    }

    @Override
    public void clicked() {
        // Show/hide OZO Audio related controls
        if (!mSettings.isOzoAudio())
            mOzoRecView.hide();
        else
            mOzoRecView.show();

        // Show record view and hide start view
        mRecordView.toggleVisibility();
        mStartView.toggleVisibility();

        mMediaFactory.start();
    }
}
