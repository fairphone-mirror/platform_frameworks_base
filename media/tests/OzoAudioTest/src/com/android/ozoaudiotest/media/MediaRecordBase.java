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

import android.util.Log;
import android.content.Context;

import android.hardware.Camera;

import android.os.SystemClock;
import android.os.Handler;

import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Locale;


// Base class for OZO Audio related recordings
abstract class MediaRecordBase implements MediaRecordInterface, MediaLoggerInterface {
    final static String FILEPREFIX = "ozo_record_";

    final static int SAMPLING_RATE_IN_HZ = 48000;

    // OZO Audio related parameters, these need to be adapted to target device!!
    // Pixel 3 device ID and input mics
    // final static String OZO_DEVICE_UUID = "4238B2D5-0234-41C0-8E49-BB5D3FD474DC";
    // Pixel 4 device ID and input mics
    final static String OZO_DEVICE_UUID = "D3AE30E4-203E-400F-9CC2-B565EF964924";
    final static int OZO_INPUTMIC_CHANNELS = 3;
    final static int MEDIARECORDER_CHANNELMASK = 0x80000006;

    private Context mContext;

    protected Camera mCamera;
    protected OzoSettings mSettings;

    protected MediaRecordListener mListener;

    private String mFileNameBase;

    private int mState = 0;
    private boolean mPaused = false;

    // Record time update
    private long mStartHTime = 0L;
    private long mPauseTime = 0L;
    private Handler mTimerHandler = new Handler();
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long updatedTime = mPauseTime + (SystemClock.uptimeMillis() - mStartHTime);

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;

            String text = "" + String.format("%02d", mins) + ":" + String.format("%02d", secs);
            mListener.onRecordPositionUpdate(text);

            mTimerHandler.postDelayed(this, 500);
        }
    };

    MediaRecordBase(Context context, OzoSettings settings) {
        mContext = context;
        mSettings = settings;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    public void setListener(MediaRecordListener listener) {
        mListener = listener;
    }

    protected abstract void setup();
    protected abstract boolean hasRecorder();
    protected abstract void recorderStart();
    protected abstract void recorderStop();
    protected abstract void recorderPause();
    protected abstract void recorderResume();
    protected abstract void recorderRelease();

    public void start() {
        if (mState == 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String date = sdf.format(new Date());
            mFileNameBase = FILEPREFIX + date;

            this.setup();

            try {
                this.recorderStart();
                this.startTimer();
                mState = 1;
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Could not start recorder: ", e);
                this.release();
                mState = 0;
            }
        }
    }

    public void stop() {
        this.log("stop()");

        if (this.hasRecorder() == true) {
            try {
                mPaused = false;
                this.recorderStop();
                this.log("stopped()");
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Could not stop recorder: ", e);
            } finally {
                this.stopTimer(false);
                this.release();
                mState = 0;
                this.UILog("Recording stopped");
            }
        }
    }

    public void pause() {
        this.log("pause()");

        if (this.hasRecorder() == true) {
            try {
                if (mPaused) {
                    mPaused = false;
                    this.recorderResume();
                    this.startTimer();
                    this.log("resuming");
                }
                else {
                    mPaused = true;
                    this.recorderPause();
                    this.stopTimer(true);
                    this.log("pausing");
                }
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Could not pause/resume recorder: ", e);
            }
        }
    }

    protected void release() {
        this.log("release()");
        this.recorderRelease();
        mState = 0;
        if (mListener != null)
            mListener.onRecordReleased();
    }

    // Stop record time timer
    private void stopTimer(boolean paused) {
        mPauseTime = (paused == true) ? SystemClock.uptimeMillis() - mStartHTime : 0L;
        mTimerHandler.removeCallbacks(updateTimerThread);
    }

    // Start record time timer
    private void startTimer() {
        this.log("Start timer");
        mStartHTime = SystemClock.uptimeMillis();
        mTimerHandler.postDelayed(updateTimerThread, 0);
    }

    public void log(String string) {
        if (mListener != null)
            mListener.onLogEvent(string);
    }

    public void UILog(String string) {
        if (mListener != null)
            mListener.onUILogEvent(string);
    }

    public Context getContext() {
        return mContext;
    }

    public String getMP4MediaFileName() {
        String basePath = mContext.getExternalFilesDir(null).getPath() + "/";
        return basePath + mFileNameBase + ".mp4";
    }

    public String getOzoTuneFileName() {
        String basePath = mContext.getExternalFilesDir(null).getPath() + "/";
        return basePath + mFileNameBase + ".ozotune";
    }

    private String getPCMAudioName() {
        return mFileNameBase + ".pcm";
    }

    public String getPCMAudioFileName() {
        String basePath = mContext.getExternalFilesDir(null).getPath() + "/";
        return basePath + this.getPCMAudioName();
    }

    public File getPCMAudioFile() {
        return new File(mContext.getExternalFilesDir(null), this.getPCMAudioName());
    }
}
