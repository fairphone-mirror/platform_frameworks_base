package com.android.server.policy;

import android.os.Handler;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;

import java.io.PrintWriter;

/**
 * add by t2m yingyubin for FP5-189 20230324
 * Watches for Pick up gesture sensor events then invokes the listener.
 */
public abstract class PickUpGestureListener {
    private static final String TAG = "PickUpGestureListener";

    private final SensorManager mSensorManager;
    private final Handler mHandler;

    private final Object mLock = new Object();

    private boolean mTriggerRequested;
    private Sensor mSensor;

    public PickUpGestureListener(Context context, Handler handler) {
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mHandler = handler;

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PICK_UP_GESTURE);
    }

    public abstract void onWakeUp();

    public boolean isSupported() {
        synchronized (mLock) {
            return mSensor != null;
        }
    }

    public void requestWakeUpTrigger() {
        synchronized (mLock) {
            if (mSensor != null && !mTriggerRequested) {
                mTriggerRequested = true;
                mSensorManager.requestTriggerSensor(mListener, mSensor);
            }
        }
    }

    public void cancelWakeUpTrigger() {
        synchronized (mLock) {
            if (mSensor != null && mTriggerRequested) {
                mTriggerRequested = false;
                mSensorManager.cancelTriggerSensor(mListener, mSensor);
            }
        }
    }

    private final TriggerEventListener mListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent event) {
            synchronized (mLock) {
                mTriggerRequested = false;
                mHandler.post(mWakeUpRunnable);
            }
        }
    };

    private final Runnable mWakeUpRunnable = new Runnable() {
        @Override
        public void run() {
            onWakeUp();
        }
    };
}

