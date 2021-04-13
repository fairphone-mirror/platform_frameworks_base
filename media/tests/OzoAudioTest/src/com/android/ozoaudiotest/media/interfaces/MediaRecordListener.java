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

// Record events listener interface
public interface MediaRecordListener {
    // Recording position update
    void onRecordPositionUpdate(String position);

    // Recording handle released
    void onRecordReleased();

    // Recording setup is in-progress
    void onRecordSetup(boolean ozoAudio);

    // New wind level update
    void onRecordSetWindLevel(int level);

    // New audio level update
    void onRecordSetAudioLevels(int left, int right);

    // Log event
    void onLogEvent(String string);

    // UI log event
    void onUILogEvent(String string);
}
