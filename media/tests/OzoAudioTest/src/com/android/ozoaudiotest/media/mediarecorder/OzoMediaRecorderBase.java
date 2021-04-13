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
import android.media.CamcorderProfile;

// MediaRecorder API based implementation for OZO Audio. Recording uses MediaRecorder API
// and OZO Audio is used either as (pre-processing) effect or as separate media codec.
abstract class OzoMediaRecorderBase extends MediaRecordBase {
    protected MediaRecorder mMediaRecorder;

    OzoMediaRecorderBase(Context context, OzoSettings settings) {
        super(context, settings);
    }

    protected abstract void setAudioSource();
    protected abstract void mediaRecordSetup();

    protected void setup() {
        this.log("setup()");

        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        this.setAudioSource();

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // Video encoding parameters
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        // Audio encoding parameters
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(256000);
        mMediaRecorder.setAudioChannels(2);
        mMediaRecorder.setAudioSamplingRate(SAMPLING_RATE_IN_HZ);

        // Set MP4 output file
        String fileName = this.getMP4MediaFileName();
        this.log("Recording to file: " + fileName);
        this.UILog("Recording " + fileName);

        this.mediaRecordSetup();

        mMediaRecorder.setOutputFile(fileName);

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            this.log("Could not prepare recorder: " + e);
            this.release();
        }
    }

    protected boolean hasRecorder() {
        return this.mMediaRecorder != null ? true : false;
    }

    protected void recorderStart() {
        mMediaRecorder.start();
    }

    protected void recorderStop() {
        mMediaRecorder.stop();
    }

    protected void recorderPause() {
        mMediaRecorder.pause();
    }

    protected void recorderResume() {
        mMediaRecorder.resume();
    }

    protected void recorderRelease() {
        mMediaRecorder.release();
        mMediaRecorder = null;

        mCamera.lock(); // Take camera back from MediaRecorder
    }
}
