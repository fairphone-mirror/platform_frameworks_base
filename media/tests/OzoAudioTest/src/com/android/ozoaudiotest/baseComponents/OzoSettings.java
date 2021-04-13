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

// Record option settings
public interface OzoSettings {
    public int getRecordState();
    public boolean isOzoAudio();
    public boolean isOzoTune();
    public boolean isWnrEnabled();
    public boolean isSSLocNotificationsEnabled();
    public boolean isWnrNotificationsEnabled();
    public boolean isNsEnabled();
    public boolean isAudioLevelsNotificationsEnabled();
}
