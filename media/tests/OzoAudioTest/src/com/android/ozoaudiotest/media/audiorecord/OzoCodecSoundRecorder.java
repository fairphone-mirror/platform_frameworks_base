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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

// Sound recorder using OZO Audio media codec via MediaCodec API. Output is stored as MP4 format.
public class OzoCodecSoundRecorder extends MediaRecordBase {
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Size of the buffer where the audio data is stored by Android (26.6ms x 3ch x 2)
     */
    private static final int BUFFER_SIZE = 7680;

    // Buffer size in microseconds (OZO_INPUTMIC_CHANNELS channels, 16-bit width, 48kHz)
    private static final long FRAME_BUFFER_DUR_MS = (BUFFER_SIZE * 1000000L) / (OZO_INPUTMIC_CHANNELS * 2 * 48000);

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean mRecordingInProgress = new AtomicBoolean(false);

    private AudioRecord mAudioRecorder;
    private Thread mRecordingThread;

    private MediaCodec mCodec;
    private MediaFormat mFormat;
    private MediaMuxer mMuxer;
    private static final int TIMEOUT_US = 10000;
    private int noOutputCounter = 0;
    private int mAudioTrackIndex;
    private long ts = 0;

    OzoCodecSoundRecorder(Context context, OzoSettings settings) {
        super(context, settings);
    }

    public void switchFocusOff() {}

    public void setFocusAzimuth(double azimuth) {}

    public void setFocusZoom(double value) {}

    public boolean enableOzoAudioCustomProcessing() {
        return false;
    }

    public boolean disableOzoAudioCustomProcessing() {
        return false;
    }

    public boolean enableAudioWindNoiseReduction() {
        return false;
    }

    public boolean disableAudioWindNoiseReduction() {
        return false;
    }

    public boolean enableAudioNoiseSuppression() {
        return false;
    }

    public boolean disableAudioNoiseSuppression() {
        return false;
    }

    protected void setup() {
        this.log("setup()");

        // Set MP4 output file
        String filename = this.getMP4MediaFileName();
        this.log("Recording to file: " + filename);
        this.UILog("Recording " + filename);

        // Open all mics and return without downmix
        int channelIndexMask = (1 << OZO_INPUTMIC_CHANNELS) - 1;

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

        // Set ouput mime type
        final String outputMimeType = "audio/ozoaudio";

        // Set output format
        mFormat = new MediaFormat();
        mFormat.setString(MediaFormat.KEY_MIME, outputMimeType);
        mFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
        mFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, 256000);

        try {
            // Get and configure encoding codec
            mCodec = MediaCodec.createEncoderByType(outputMimeType);
            mCodec.configure(mFormat, null /* surface */, null /* crypto */, MediaCodec.CONFIGURE_FLAG_ENCODE);

            // Create MP4 writer
            mMuxer = new MediaMuxer(filename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        } catch (Exception e) {
            this.log("Media codec creation failed");
            mCodec.release();
            mCodec = null;
        }
    }

    protected boolean hasRecorder() {
        return this.mAudioRecorder != null ? true : false;
    }

    protected void recorderStart() {
        mCodec.start();
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

        if (mCodec != null) {
            mCodec.flush();
            mCodec.stop();
            mCodec.release();
        }

        mMuxer.stop();
        mMuxer.release();
    }

    private void sendToMediaMuxer() {
        int outputBufferIndex = 0;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (outputBufferIndex >= 0) {
            // See if codec has encoded data in a new output buffer
            outputBufferIndex = mCodec.dequeueOutputBuffer(info, TIMEOUT_US);

            if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mAudioTrackIndex = mMuxer.addTrack(mCodec.getOutputFormat());
                mMuxer.start();
            }

            if (outputBufferIndex >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
                    info.size = 0;

                // Output buffer
                ByteBuffer outputBuffer = mCodec.getOutputBuffer(outputBufferIndex);
                if (outputBuffer != null)
                    this.muxAudio(outputBuffer, info);

                mCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    private void muxAudio(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        try{
            mMuxer.writeSampleData(mAudioTrackIndex, buffer, bufferInfo);
        } catch(IllegalArgumentException e) {
            log("Muxer received illegal agrument");
        } catch(IllegalStateException e) {
            log("Muxer in illegal state");
        }
    }

    private boolean isEndOfStream(MediaCodec.BufferInfo info) {
        return (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    private boolean isCodecInfo(MediaCodec.BufferInfo info) {
        return (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
    }

    private class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            while (mRecordingInProgress.get()) {
                int result = mAudioRecorder.read(buffer, BUFFER_SIZE);
                if (result < 0)
                    throw new RuntimeException("Reading of audio buffer failed: " + getBufferReadFailureReason(result));

                this.encode(buffer, BUFFER_SIZE, ts);
                buffer.clear();

                // Audio data size in microseconds
                ts += FRAME_BUFFER_DUR_MS;
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

        private void encode(ByteBuffer rawBuffer, int length, long presentationTimeUs) {
            // Get index of free input buffer from codec
            int inputBufferIndex = mCodec.dequeueInputBuffer(TIMEOUT_US);

            if (inputBufferIndex >= 0) {
                // Get free input buffer from codec
                ByteBuffer inputBuffer = mCodec.getInputBuffer(inputBufferIndex);

                if (inputBuffer != null) {
                    inputBuffer.put(rawBuffer.array(), 0, length);

                    // Queue new input buffer to encode it
                    mCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
                }
            }

            sendToMediaMuxer();
        }
    }
}
