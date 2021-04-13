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

// Create media recorder interface that handles OZO Audio in following modes:
// - OZO Audio as media codec using Android MediaRecorder API
// - OZO Audio as pre-processing effect using Android MediaRecorder API
// - OZO Audio as pre-processing effect using Android AudioRecord API (audio only recording in PCM format)
public class OzoRecordFactory implements RecordFactoryInterface {
    private MediaRecordListener mListener;
    private MediaRecordInterface mInterface;

    private Context mContext;
    private OzoSettings mSettings;

    OzoRecordFactory(Context context, OzoSettings settings) {
        mContext = context;
        mSettings = settings;
    }

    public void start() {
        if (mInterface != null)
            mInterface.start();
    }

    public void stop() {
        if (mInterface != null)
            mInterface.stop();
    }

    public void pause() {
        if (mInterface != null)
            mInterface.pause();
    }

    public void resume() {
        this.pause();
    }

    public void setListener(MediaRecordListener listener) {
        mListener = listener;
        if (mInterface != null)
            mInterface.setListener(mListener);
    }

    public void changeInterface(int state) {
        mInterface = null;

        if (state == OzoViewListener.RECORD_STATE.AUDIO_RECORDER_OZO) {
            mListener.onLogEvent("Create OZO sound recorder");
            mInterface = new OzoEffectSoundRecorder(mContext, mSettings, true);
        } else if (state == OzoViewListener.RECORD_STATE.AUDIO_RECORDER) {
            mListener.onLogEvent("Create sound recorder");
            mInterface = new OzoEffectSoundRecorder(mContext, mSettings, false);
        } else if (state == OzoViewListener.RECORD_STATE.MEDIA_EFFECT) {
            mListener.onLogEvent("Create OZO effect recorder");
            mInterface = new OzoEffectMediaRecorder(mContext, mSettings);
        } else if (state == OzoViewListener.RECORD_STATE.MEDIA_RECORDER) {
            mListener.onLogEvent("Create OZO codec recorder");
            mInterface = new OzoCodecMediaRecorder(mContext, mSettings);
        } else if (state == OzoViewListener.RECORD_STATE.MEDIA_CODEC) {
            mListener.onLogEvent("Create OZO sound recorder using media codec");
            mInterface = new OzoCodecSoundRecorder(mContext, mSettings);
        } else {
            mListener.onLogEvent("Create legacy media recorder");
            mInterface = new OzoCodecMediaRecorder(mContext, mSettings);
        }

        this.setListener(mListener);
    }

    public MediaRecordInterface getInterface() {
        return mInterface;
    }
}
