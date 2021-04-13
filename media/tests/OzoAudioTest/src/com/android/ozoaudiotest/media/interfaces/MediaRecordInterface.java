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

import android.hardware.Camera;

// Record engine interface
public interface MediaRecordInterface {
    public void setCamera(Camera camera);
    public void setListener(MediaRecordListener listener);

    public void start();
    public void stop();
    public void pause();

    // Audio focus changes
    public void switchFocusOff();
    public void setFocusAzimuth(double azimuth);
    public void setFocusZoom(double value);

    // Custom (vendor specific) audio processing modes
    public boolean enableOzoAudioCustomProcessing();
    public boolean disableOzoAudioCustomProcessing();

    // Audio windscreen
    public boolean enableAudioWindNoiseReduction();
    public boolean disableAudioWindNoiseReduction();

    // Audio noise suppression
    public boolean enableAudioNoiseSuppression();
    public boolean disableAudioNoiseSuppression();
}
