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

// High level recording interface
public interface RecordFactoryInterface {

    // Recording is started
    public void start();

    // Recording is stopped
    public void stop();

    // Recording is paused
    public void pause();

    // Recording is resumed
    public void resume();

    // Set record event listener
    public void setListener(MediaRecordListener listener);

    // Change record engine, supported states are listed in OzoViewListener.RECORD_STATE
    public void changeInterface(int state);

    // Return recording engine interface
    public MediaRecordInterface getInterface();
}
