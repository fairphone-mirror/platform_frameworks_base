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

import android.content.Context;
import android.media.MediaRecorder;

// Media recorder interface implementation using either OZO Audio or plain AAC codec
class OzoCodecMediaRecorder extends OzoMediaRecorderBase implements MediaRecorder.OnEventListener {

    OzoCodecMediaRecorder(Context context, OzoSettings settings) {
        super(context, settings);
    }

    // Receive wind noise level notifications
    public void onWindEvent(MediaRecorder mr, int ts, boolean status, int level) {
        String text = "Wind level (timestamp + level): " + ts + ", " + level;
        this.log(text);
        if (mListener != null)
            mListener.onRecordSetWindLevel(level);
    }

    public void onSourceLocalizationEvent(MediaRecorder mr, int azimuth, int elevation, int strength, int id) {
        String text = "Sound source localization: " + azimuth + ", " + elevation + ", " + strength + ", " + id;
        this.log(text);
    }

    public void onSourceSectorAzimuthEvent(MediaRecorder mr, int azimuth, int elevation, int id) {
        String text = "Sound source sector (azimuth): " + azimuth + ", " + elevation + ", " + id;
        this.log(text);
    }

    public void onSourceSectorWidthEvent(MediaRecorder mr, int width, int height, int id) {
        String text = "Sound source sector (width): " + width + ", " + height + ", " + id;
        this.log(text);
    }

    public void onAudioLevelsEvent(MediaRecorder mr, int levelL, int levelR) {
        String text = "Audio levels (L + R): " + levelL + ", " + levelR;
        this.log(text);
        if (mListener != null)
            mListener.onRecordSetAudioLevels(levelL, levelR);
    }

    public void switchFocusOff() {
        mMediaRecorder.disableAudioFocus();
        this.UILog("Focus disabled");
    }

    public void setFocusAzimuth(double azimuth) {
        mMediaRecorder.enableAudioFocus();
        mMediaRecorder.setAudioFocusAzimuth(azimuth);
    }

    public void setFocusZoom(double value) {
        mMediaRecorder.enableAudioFocus();
        mMediaRecorder.setAudioFocusGain(value);
    }

    public boolean enableOzoAudioCustomProcessing() {
        if (mMediaRecorder != null) {
            mMediaRecorder.enableOzoAudioCustomProcessing();
            return true;
        }

        return false;
    }

    public boolean disableOzoAudioCustomProcessing() {
        if (mMediaRecorder != null) {
            mMediaRecorder.disableOzoAudioCustomProcessing();
            return true;
        }

        return false;
    }

    public boolean enableAudioWindNoiseReduction() {
        if (mMediaRecorder != null) {
            mMediaRecorder.enableAudioWindNoiseReduction();
            return true;
        }

        return false;
    }

    public boolean disableAudioWindNoiseReduction() {
        if (mMediaRecorder != null) {
            mMediaRecorder.disableAudioWindNoiseReduction();
            return true;
        }

        return false;
    }

    public boolean enableAudioNoiseSuppression() {
        if (mMediaRecorder != null) {
            mMediaRecorder.enableAudioNoiseSuppression();
            return true;
        }

        return false;
    }

    public boolean disableAudioNoiseSuppression() {
        if (mMediaRecorder != null) {
            mMediaRecorder.disableAudioNoiseSuppression();
            return true;
        }

        return false;
    }

    protected void setAudioSource() {
        boolean isOzoAudio = mSettings.isOzoAudio();

        // Video + audio in MP4
        if (isOzoAudio)
            // OZO Audio requires unprocessed audio as input
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
        else
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC
        );
    }

    protected void mediaRecordSetup() {
        boolean isOzoAudio = mSettings.isOzoAudio();

        mMediaRecorder.setOnEventListener(this);

        if (mListener != null)
            mListener.onRecordSetup(isOzoAudio);

        // OZO Audio specific initialization parameters
        if (isOzoAudio) {
            // Request wind noise level notifications
            if (mSettings.isWnrNotificationsEnabled())
                mMediaRecorder.enableAudioWindNoiseNotification();

            // Request audio level notifications (left and right channel levels)
            if (mSettings.isAudioLevelsNotificationsEnabled())
                mMediaRecorder.enableAudioLevelsNotification();

            mMediaRecorder.setOzoAudioParameters(OZO_DEVICE_UUID, OZO_INPUTMIC_CHANNELS);

            // Wind noise reduction status at startup
            if (mSettings.isWnrEnabled())
                mMediaRecorder.setInitialWindNoise(true); // WNR enabled

            // Noise suppression status at startup
            if (mSettings.isNsEnabled()) {
                mMediaRecorder.setInitialNoiseSuppression();
            }

            // Sound source localization notifications status at startup
            if (mSettings.isSSLocNotificationsEnabled())
                mMediaRecorder.enableSoundSourceLocalization();

            // Assign initial focus settings
            mMediaRecorder.setInitialFocusParameters(
                MediaRecorder.OzoAudioParameters.ZERO_GAIN,
                MediaRecorder.OzoAudioParameters.DEFAULT_AZIMUTH,
                MediaRecorder.OzoAudioParameters.DEFAULT_ELEVATION
            );
        }

        // Set OZO Tune file (note: this requires that Ozo SDK license file supports Tune)
        if (isOzoAudio && mSettings.isOzoTune()) {
            String postFile = this.getOzoTuneFileName();
            this.log("OZO Tune file: " + postFile);
            mMediaRecorder.setOzoTuneFile(postFile);
        }
    }
}
