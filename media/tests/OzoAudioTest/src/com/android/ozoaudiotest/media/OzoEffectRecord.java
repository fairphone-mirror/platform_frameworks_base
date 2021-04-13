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

import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.OzoAudioEffect;

import android.os.Handler;

// Ozo effect wrapper for media recordings
class OzoEffectRecord {
    private OzoAudioEffect mOzoEffect;
    private OzoSettings mSettings;
    private MediaRecordListener mListener;
    private MediaLoggerInterface mLogger;

    // Wind level updates are handled here
    private Handler mWnrHandler = new Handler();
    private Runnable updateWnrThread = new Runnable() {
        public void run() {
            if (mOzoEffect != null && mListener != null) {
                int windLevel = mOzoEffect.getWnrLevel();
                if (windLevel < 0)
                    windLevel = 0;

                mListener.onRecordSetWindLevel(windLevel);
            }

            // Update every 75ms
            mWnrHandler.postDelayed(this, 75);
        }
    };

    // Audio level updates are handled here
    private Handler mLevelHandler = new Handler();
    private Runnable updateLevelThread = new Runnable() {
        public void run() {
            if (mOzoEffect != null && mListener != null) {
                int[] audioLevel = mOzoEffect.getAudioLevel();

                if (mListener != null)
                    mListener.onRecordSetAudioLevels(audioLevel[0], audioLevel[1]);
            }

            // Update every 75ms
            mLevelHandler.postDelayed(this, 75);
        }
    };

    OzoEffectRecord(int sessionId, OzoSettings settings, MediaLoggerInterface logger) {
        mSettings = settings;
        mLogger = logger;
        mOzoEffect = OzoAudioEffect.create(sessionId);
    }

    public void setListener(MediaRecordListener listener) {
        mListener = listener;
    }

    public void disableFocus() {
        mOzoEffect.disableFocus();
        mLogger.UILog("Focus disabled");
    }

    public void setFocusAzimuth(double azimuth) {
        mOzoEffect.setFocusAzimuth(azimuth);
    }

    public void setFocusZoom(double value) {
        mOzoEffect.setFocusGain(value);
    }

    public boolean enableOzoAudioCustomProcessing() {
        mOzoEffect.setOzoParameter(OzoAudioEffect.OzoParameters.FEAT_CUSTOM, OzoAudioEffect.OzoParameters.ENABLED);
        return true;
    }

    public boolean disableOzoAudioCustomProcessing() {
        mOzoEffect.setOzoParameter(OzoAudioEffect.OzoParameters.FEAT_CUSTOM, OzoAudioEffect.OzoParameters.DISABLED);
        return true;
    }

    public boolean enableAudioWindNoiseReduction() {
        return mOzoEffect.enableWnr() == AudioEffect.SUCCESS ? true : false;
    }

    public boolean disableAudioWindNoiseReduction() {
        return mOzoEffect.disableWnr() == AudioEffect.SUCCESS ? true : false;
    }

    public boolean enableAudioNoiseSuppression() {
        return mOzoEffect.enableNs() == AudioEffect.SUCCESS ? true : false;
    }

    public boolean disableAudioNoiseSuppression() {
        return mOzoEffect.disableNs() == AudioEffect.SUCCESS ? true : false;
    }

    public void init(String deviceID) {
        mOzoEffect.setDevice(deviceID);
        mLogger.log("Device UUID: " + mOzoEffect.getDevice());

        // Wind noise reduction status at startup
        if (mSettings.isWnrEnabled())
            mOzoEffect.enableWnr(); // WNR enabled

        // Noise suppression status at startup
        if (mSettings.isNsEnabled()) {
            mOzoEffect.enableNs();
        }

        // Focus
        mOzoEffect.enableFocus();
        mOzoEffect.setFocusGain(MediaRecorder.OzoAudioParameters.ZERO_GAIN);
        mOzoEffect.setFocusAzimuth(MediaRecorder.OzoAudioParameters.DEFAULT_AZIMUTH);
        mOzoEffect.setFocusElevation(MediaRecorder.OzoAudioParameters.DEFAULT_ELEVATION);

        int ret = mOzoEffect.setEnabled(true);
        if (ret != AudioEffect.SUCCESS) {
            mLogger.log("Error when creating Ozo effect: " + ret);
            return;
        }

        this.start();

        if (mListener != null)
            mListener.onRecordSetup(true);
    }

    private void start() {
        if (mSettings.isWnrNotificationsEnabled())
            this.startWnrTimer();

        if (mSettings.isAudioLevelsNotificationsEnabled())
            this.startLevelTimer();
    }

    public void stop() {
        if (mSettings.isWnrNotificationsEnabled())
            this.stopWnrTimer();

        if (mSettings.isAudioLevelsNotificationsEnabled())
            this.stopLevelTimer();
    }

    public void release() {
        if (mOzoEffect != null)
            mOzoEffect.setEnabled(false);

        if (mOzoEffect != null) {
            mOzoEffect.release();
            mOzoEffect = null;
            mLogger.log("Ozo effect released()");
        }
    }

    // Stop wind level notifications
    private void stopWnrTimer() {
        mWnrHandler.removeCallbacks(updateWnrThread);
    }

    // Start wind level notifications
    private void startWnrTimer() {
        mWnrHandler.postDelayed(updateWnrThread, 0);
    }

    // Stop audio level notifications
    private void stopLevelTimer() {
        mLevelHandler.removeCallbacks(updateLevelThread);
    }

    // Start audio level notifications
    private void startLevelTimer() {
        mLevelHandler.postDelayed(updateLevelThread, 0);
    }
}
