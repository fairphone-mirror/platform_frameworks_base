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

public abstract interface OzoViewListener {

    // Record states
    interface RECORD_STATE {
        // Ozo effect with MediaRecoder API
        int MEDIA_EFFECT = 0;
        // Ozo media codec with MediaRecorder API
        int MEDIA_RECORDER = 1;
        // Ozo effect with AudioRecord API
        int AUDIO_RECORDER_OZO = 2;
        // AAC media codec with MediaRecorder API
        int LEGACY_RECORDER = 3;
        // Sound record with AudioRecord API
        int AUDIO_RECORDER = 4;
        // Sound recording with MediaCodec API
        int MEDIA_CODEC = 5;
    }

    void onRecordStateChanged(int state);

    void onRecordStop();
    void onRecordPause();
    void onRecordResume();

    void onRecordSetFocusZoom(double value);

    boolean onRecordEnableWindscreen();
    boolean onRecordDisableWindscreen();
    boolean onRecordEnableCustomProcessing();
    boolean onRecordDisableCustomProcessing();
    boolean onRecordEnableNoiseSuppression();
    boolean onRecordDisableNoiseSuppression();
}
