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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

// Sound recorder using OZO Audio pre-processing effect. Output is stored as PCM format
public class OzoEffectSoundRecorder extends MediaRecordBase {
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Size of the buffer where the audio data is stored by Android (3 x 20ms x 3ch x 2 => 60ms worth of audio in bytes)
     */
    private static final int BUFFER_SIZE = 3 * 960 * OZO_INPUTMIC_CHANNELS * 2;

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean mRecordingInProgress = new AtomicBoolean(false);

    private AudioRecord mAudioRecorder;
    private Thread mRecordingThread;
    private OzoEffectRecord mOzoEffect;

    private boolean mIsOzoAudio;

    OzoEffectSoundRecorder(Context context, OzoSettings settings, boolean isOzoAudio) {
        super(context, settings);
        mIsOzoAudio = isOzoAudio;
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

    protected void setup() {
        this.log("setup()");

        // Set PCM output file
        String filename = this.getPCMAudioFileName();
        this.log("Recording to file: " + filename);
        this.UILog("Recording " + filename);

        // Open all mics but return stereo output
        int channelIndexMask = (mIsOzoAudio) ? 6 : 3;

        // Open all mics and return without downmix
        //int channelIndexMask = (1 << OZO_INPUTMIC_CHANNELS) - 1;

        AudioFormat audioInputFormat = new AudioFormat.Builder()
            .setEncoding(AUDIO_FORMAT)
            .setSampleRate(SAMPLING_RATE_IN_HZ)
            .setChannelIndexMask(channelIndexMask)
            .build();

        mAudioRecorder = new AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            .setAudioFormat(audioInputFormat)
            .setBufferSizeInBytes(BUFFER_SIZE)
            .build();

        if (mIsOzoAudio) {
            mOzoEffect = new OzoEffectRecord(mAudioRecorder.getAudioSessionId(), mSettings, this);
            mOzoEffect.setListener(mListener);
            mOzoEffect.init(OZO_DEVICE_UUID);
        }
    }

    protected boolean hasRecorder() {
        return this.mAudioRecorder != null ? true : false;
    }

    protected void recorderStart() {
        mAudioRecorder.startRecording();

        mRecordingInProgress.set(true);
        mRecordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        mRecordingThread.start();
    }

    protected void recorderStop() {
        mRecordingInProgress.set(false);
        mAudioRecorder.stop();
    }

    protected void recorderPause() {
        mRecordingInProgress.set(false);
    }

    protected void recorderResume() {
        mRecordingInProgress.set(true);
    }

    protected void recorderRelease() {
        mAudioRecorder.release();
        mAudioRecorder = null;

        if (mOzoEffect != null)
            mOzoEffect.release();
        mOzoEffect = null;
    }

    private class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            final File file = getPCMAudioFile();
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {

                while (mRecordingInProgress.get()) {
                    int result = mAudioRecorder.read(buffer, BUFFER_SIZE);
                    if (result < 0)
                        throw new RuntimeException("Reading of audio buffer failed: " + getBufferReadFailureReason(result));

                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }
}
