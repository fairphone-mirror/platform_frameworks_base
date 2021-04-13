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

import android.widget.Button;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.Toast;

import java.text.DecimalFormat;

public class RecordViewComponents implements MediaRecordListener, OzoViewListener, OzoCircle.OzoCircleListener {
    Context mContext;
    String mLogTag;
    OzoOptionSettings mSettings;
    OzoView mStartView;
    OzoView mRecView;
    OzoView mPausedView;

    private OzoLevelMeterView mLevelLeft;
    private OzoLevelMeterView mLevelRight;

    private OzoView mRecCtrl;

    private OzoSwitch mWnrSwitch;
    private OzoLevelMeterView mWindLevelView;

    private OzoSwitch mNsSwitch;
    private OzoSwitch mCustomProcSwitch;

    private OzoCircle mAzimuth;
    private OzoSeekBar mZoomBar;

    private OzoButton mStopButton;
    private OzoButton mPauseButton;

    private Camera mCamera;
    private OzoRecordFactory mRecordFactory;

    private DecimalFormat mPrecision = new DecimalFormat("0.00");

    RecordViewComponents(Context context, String logTag, OzoOptionSettings settings,
        Button stopButton, Button pauseButton, Switch wnrSwitch, Switch nsSwitch, Switch customProcSwitch, OzoLevel windLevelView,
        SeekBar zoom, OzoView startView, OzoView recView, OzoView pausedView, OzoCircle azimuth, OzoView recCtrl,
        OzoLevel levelLeft, OzoLevel levelRight) {

        mContext = context;
        mLogTag = logTag;
        mSettings = settings;
        mStartView = startView;
        mRecView = recView;
        mPausedView = pausedView;
        mRecCtrl = recCtrl;

        mLevelLeft = new OzoLevelMeterView(levelLeft);
        mLevelRight = new OzoLevelMeterView(levelRight);
        levelLeft.setScale(32768);
        levelRight.setScale(32768);

        mAzimuth = azimuth;
        mAzimuth.setListener(this);

        mRecordFactory = new OzoRecordFactory(mContext, mSettings);
        mRecordFactory.setListener(this);
        mRecordFactory.changeInterface(mSettings.getRecordState());

        mStopButton = new StopRecButton(mStartView, mRecView, stopButton);
        mStopButton.setListener(this);

        mPauseButton = new PauseRecButton(pauseButton, mPausedView);
        mPauseButton.setListener(this);

        // Current wind level is shown in this view
        mWindLevelView = new OzoLevelMeterView(windLevelView);
        mWnrSwitch = new WnrSwitch(mContext, mWindLevelView, wnrSwitch);
        mWnrSwitch.setListener(this);

        mNsSwitch = new NoiseSuppressionSwitch(mContext, nsSwitch);
        mNsSwitch.setListener(this);

        mCustomProcSwitch = new CustomProcSwitch(mContext, customProcSwitch);
        mCustomProcSwitch.setListener(this);

        mZoomBar = new ZoomBar(zoom);
        mZoomBar.setListener(this);
    }

    public OzoRecordFactory getRecordInstance() {
        return mRecordFactory;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        mRecordFactory.getInterface().setCamera(camera);
    }

    //
    // Event from media recorder
    //

    public void onRecordPositionUpdate(String position) {
        mStopButton.setText(position);
    }

    public void onRecordReleased() {
        mPauseButton.reset();
    }

    public void onRecordSetup(boolean ozoAudio) {
        if (ozoAudio) {
            mAzimuth.setAngle(0);                // Front focus
            mZoomBar.reset(0);                   // Zoom 0
            mCustomProcSwitch.setChecked(false); // No custom processing by default

            // Wind noise reduction status at startup
            if (!mSettings.isWnrEnabled()) {     // WNR disabled
                mWnrSwitch.setChecked(false);
            } else {                             // WNR enabled
                mWnrSwitch.setChecked(true);
            }

            // Noise suppression status at startup
            if (!mSettings.isNsEnabled()) {      // Noise suppression disabled
                mNsSwitch.setChecked(false);
            } else {                             // Noise suppression enabled
                mNsSwitch.setChecked(true);
            }

            // Show or hide audio levels meter
            if (mSettings.isAudioLevelsNotificationsEnabled()) {
                mLevelLeft.open();
                mLevelLeft.show();
                mLevelRight.open();
                mLevelRight.show();
            }
            else {
                mLevelLeft.hide();
                mLevelRight.hide();
            }

            // Show or hide wind level meter
            if (mSettings.isWnrNotificationsEnabled())
                mWindLevelView.show();
            else
                mWindLevelView.hide();
        }
    }

    public void onRecordSetWindLevel(int level) {
        mWindLevelView.setLevel(level);
    }

    public void onRecordSetAudioLevels(int left, int right) {
        mLevelLeft.setLevel(left);
        mLevelRight.setLevel(right);
    }

    public void onLogEvent(String string) {
        Log.e(mLogTag, string);
    }

    public void onUILogEvent(String string) {
        Toast.makeText(mContext.getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }

    //
    // Events from UI components
    //

    public void onRecordStateChanged(int state) {
        mRecordFactory.changeInterface(state);
        mRecordFactory.getInterface().setCamera(mCamera);
    }

    public void onRecordStop() {
        mRecordFactory.getInterface().stop();
    }

    public void onRecordPause() {
        mRecCtrl.hide();
        mRecordFactory.getInterface().pause();
    }

    public void onRecordResume() {
        if (mSettings.isOzoAudio())
            mRecCtrl.show();

        mRecordFactory.getInterface().pause();
    }

    public void onRecordSetFocusZoom(double value) {
        this.onUILogEvent("Focus zoom " + mPrecision.format(value));
        mRecordFactory.getInterface().setFocusZoom(value);
    }

    public boolean onRecordEnableWindscreen() {
        return mRecordFactory.getInterface().enableAudioWindNoiseReduction();
    }

    public boolean onRecordDisableWindscreen() {
        return mRecordFactory.getInterface().disableAudioWindNoiseReduction();
    }

    public boolean onRecordEnableCustomProcessing() {
        return mRecordFactory.getInterface().enableOzoAudioCustomProcessing();
    }

    public boolean onRecordDisableCustomProcessing() {
        return mRecordFactory.getInterface().disableOzoAudioCustomProcessing();
    }

    public boolean onRecordEnableNoiseSuppression() {
        return mRecordFactory.getInterface().enableAudioNoiseSuppression();
    }

    public boolean onRecordDisableNoiseSuppression() {
        return mRecordFactory.getInterface().disableAudioNoiseSuppression();
    }

    //
    // Events from OzoCircle component
    //

    public void onAngle(double azimuth) {
        this.onUILogEvent("Focus azimuth " + mPrecision.format(azimuth));
        mRecordFactory.getInterface().setFocusAzimuth(azimuth);
    }
}
