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
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;

import android.os.Handler;

import java.util.List;


// Media recorder interface implementation using OZO Audio effect + MediaRecorder API
class OzoEffectMediaRecorder extends OzoMediaRecorderBase {
    private AudioManager mAudioManager;
    private OzoEffectRecord mOzoEffect;
    private AudioManager.AudioRecordingCallback mCallback;

    OzoEffectMediaRecorder(Context context, OzoSettings settings) {
        super(context, settings);
    }

    protected void setAudioSource() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
    }

    public void switchFocusOff() {
        mOzoEffect.disableFocus();
    }

    public void setFocusAzimuth(double azimuth) {
        mOzoEffect.setFocusAzimuth(azimuth);
    }

    public void setFocusZoom(double value) {
        mOzoEffect.setFocusZoom(value);
    }

    public boolean enableOzoAudioCustomProcessing() {
        return mOzoEffect.enableOzoAudioCustomProcessing();
    }

    public boolean disableOzoAudioCustomProcessing() {
        return mOzoEffect.disableOzoAudioCustomProcessing();
    }

    public boolean enableAudioWindNoiseReduction() {
        return mOzoEffect.enableAudioWindNoiseReduction();
    }

    public boolean disableAudioWindNoiseReduction() {
        return mOzoEffect.disableAudioWindNoiseReduction();
    }

    public boolean enableAudioNoiseSuppression() {
        return mOzoEffect.enableAudioNoiseSuppression();
    }

    public boolean disableAudioNoiseSuppression() {
        return mOzoEffect.disableAudioNoiseSuppression();
    }

    protected void mediaRecordSetup() {
        OzoMediaRecorderBase self = this;
        mAudioManager = (AudioManager) this.getContext().getSystemService(Context.AUDIO_SERVICE);

        mCallback = new AudioManager.AudioRecordingCallback() {
            public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs)
            {
                log("onRecordingConfigurationChanged");
                for (AudioRecordingConfiguration arc : configs)
                {
                    if (arc.getClientAudioSource() == MediaRecorder.AudioSource.UNPROCESSED)
                    {
                        if (mOzoEffect == null) {
                            log("Creating Ozo effect");

                            int sessionId = arc.getClientAudioSessionId();
                            mOzoEffect = new OzoEffectRecord(sessionId, mSettings, self);
                            mOzoEffect.setListener(mListener);
                            mOzoEffect.init(OZO_DEVICE_UUID);
                        }
                    }
                }
            }
        };

        mAudioManager.registerAudioRecordingCallback(mCallback, null);

        // If the underlying device is a 2-mic device, this call is not needed
        mMediaRecorder.setOzoEffectChannelMask(MEDIARECORDER_CHANNELMASK);
    }

    @Override
    protected void recorderStop() {
        super.recorderStop();
        mOzoEffect.stop();
    }

    @Override
    protected void recorderRelease() {
        super.recorderRelease();

        if (mOzoEffect != null)
            mOzoEffect.release();
        mOzoEffect = null;

        mAudioManager.unregisterAudioRecordingCallback(mCallback);
    }
}
