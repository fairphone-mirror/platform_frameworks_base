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

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.Context;

import android.hardware.Camera;
import android.os.Bundle;

import android.widget.Button;
import android.widget.Switch;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import android.view.View;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.MotionEvent;

import android.graphics.PixelFormat;
import android.graphics.Rect;

import java.io.IOException;


public class OzoAudioTest extends Activity implements SurfaceHolder.Callback {

    final static String TAG = "OzoAudioTest";

    private Camera mCamera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean previewing = false;

    private OzoButton mRecButton;
    private OzoOptionSettings mRecOptions;
    private RecordViewComponents mComponents;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ozoaudiotest);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());

        // Start view
        OzoView startView = new OzoView(controlInflater.inflate(R.layout.initial, null));
        MarginLayoutParams layoutParamsCtrl = new MarginLayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        this.addContentView(startView.getView(), layoutParamsCtrl);

        // Record control view
        OzoView recView = new OzoView(controlInflater.inflate(R.layout.control, null));
        LayoutParams layoutParamsCtrl2 = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        this.addContentView(recView.getView(), layoutParamsCtrl2);

        // Record options view
        OzoView optionsView = new OzoView(controlInflater.inflate(R.layout.options, null));
        layoutParamsCtrl = new MarginLayoutParams(1500, 800);
        layoutParamsCtrl.setMargins(50, 50, 0, 0);
        this.addContentView(optionsView.getView(), layoutParamsCtrl);

        // Record paused view
        OzoView pausedView = new OzoView(controlInflater.inflate(R.layout.paused, null));
        layoutParamsCtrl = new MarginLayoutParams(700, 400);
        layoutParamsCtrl.setMargins(50, 50, 0, 0);
        this.addContentView(pausedView.getView(), layoutParamsCtrl);

        // Options button, show the record options view when clicked
        OzoButton optionsButton = new Click2ViewButton(optionsView, findViewById(R.id.recOptions));

        final RadioGroup rg = (RadioGroup) findViewById(R.id.recordMode);
        RecordMode recMode = new RecordMode(rg, R.id.soundRecMode, R.id.ozoSoundRecMode, R.id.ozoDefaultRecMode,
            R.id.ozoEffectRecMode, R.id.legacyRecMode, R.id.ozoMediaCodecRecMode);

        // Default recording mode is Ozo audio as media codec
        rg.check(R.id.ozoDefaultRecMode);
        recMode.setRecordState(R.id.ozoDefaultRecMode);

        mRecOptions = new OzoOptionSettings(
            optionsView, optionsButton,
            (Switch) findViewById(R.id.ozoTuneSwitch),
            (Switch) findViewById(R.id.wnrNotifications),
            (Switch) findViewById(R.id.wnrSwitch),
            (Switch) findViewById(R.id.sslocNotifications),
            (Switch) findViewById(R.id.nsSwitch),
            (Switch) findViewById(R.id.levelsSwitch),
            recMode
        );

        OzoView recCtrl = new OzoView(findViewById(R.id.ozoRectCtrl));

        mComponents = new RecordViewComponents(
            this, TAG, mRecOptions,
            (Button) findViewById(R.id.recStop),
            (Button) findViewById(R.id.recPause),
            (Switch) findViewById(R.id.wnr),
            (Switch) findViewById(R.id.ns),
            (Switch) findViewById(R.id.customproc),
            (OzoLevel) findViewById(R.id.windLevelView),
            (SeekBar) findViewById(R.id.zoomBar),
            startView, recView, pausedView,
            (OzoCircle) findViewById(R.id.azimuthView),
            recCtrl,
            (OzoLevel) findViewById(R.id.levelLeft),
            (OzoLevel) findViewById(R.id.levelRight)
        );

        mRecOptions.setListener(mComponents);

        // Record button, start capture when clicked
        mRecButton = new RecButton(
            mComponents.getRecordInstance(), startView, recView,
            recCtrl, mRecOptions, findViewById(R.id.recStart)
        );
        mRecButton.getView().requestFocus();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Rect viewRect = new Rect();
        mRecOptions.mOptionsView.getView().getGlobalVisibleRect(viewRect);
        if (!viewRect.contains((int) event.getRawX(), (int) event.getRawY())) {
            mRecOptions.mOptionsView.hide();
            mRecOptions.mOptionsButton.show();
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            mCamera.stopPreview();
            previewing = false;
        }

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        mComponents.setCamera(mCamera);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        previewing = false;
        mComponents.setCamera(null);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mComponents.getRecordInstance() != null)
            mComponents.getRecordInstance().pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mComponents.getRecordInstance() != null)
            mComponents.getRecordInstance().resume();
    }
}
