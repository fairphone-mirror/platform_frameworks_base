/*
Copyright (C) 2019 Nokia Corporation.
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

import android.widget.RadioGroup;

class RecordMode {
    protected RadioGroup mGroup;

    protected int mSoundRecId;
    protected int mOzoSoundRecId;
    protected int mOzoCodecRecId;
    protected int mOzoEffectRecId;
    protected int mLegacyRecId;
    protected int mOzoMediaCodecRecId;

    protected int mRecState = OzoViewListener.RECORD_STATE.MEDIA_RECORDER;

    public RecordMode(RadioGroup group, int soundRecId, int ozoSoundRecId, int ozoCodecRecId,
        int ozoEffectRecId, int legacyRecId, int mediacodecRecId) {
        mGroup = group;
        mSoundRecId = soundRecId;
        mOzoSoundRecId = ozoSoundRecId;
        mOzoCodecRecId = ozoCodecRecId;
        mOzoEffectRecId = ozoEffectRecId;
        mLegacyRecId = legacyRecId;
        mOzoMediaCodecRecId = mediacodecRecId;
    }

    public RadioGroup getGroupElement() {
        return mGroup;
    }

    public int setRecordState(int id) {
        if (id == mOzoSoundRecId)
            mRecState = OzoViewListener.RECORD_STATE.AUDIO_RECORDER_OZO;
        else if (id == mSoundRecId)
        mRecState = OzoViewListener.RECORD_STATE.AUDIO_RECORDER;
        else if (id == mOzoCodecRecId)
            mRecState = OzoViewListener.RECORD_STATE.MEDIA_RECORDER;
        else if (id == mOzoEffectRecId)
            mRecState = OzoViewListener.RECORD_STATE.MEDIA_EFFECT;
        else if (id == mLegacyRecId)
            mRecState = OzoViewListener.RECORD_STATE.LEGACY_RECORDER;
        else if (id == mOzoMediaCodecRecId) {
            mRecState = OzoViewListener.RECORD_STATE.MEDIA_CODEC;
        }

        return mRecState;
    }

    public int getRecordState() {
        return mRecState;
    }

    public boolean isOzoAudio() {
        return (mRecState == OzoViewListener.RECORD_STATE.AUDIO_RECORDER ||
                mRecState == OzoViewListener.RECORD_STATE.LEGACY_RECORDER ||
                mRecState == OzoViewListener.RECORD_STATE.MEDIA_CODEC) ? false : true;
    }
}
