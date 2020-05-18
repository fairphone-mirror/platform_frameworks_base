/*
 * Copyright 2020-2022 Fairphone B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.power;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;

public class GsensorListener {
    private final String TAG = "GsensorListener";
    private SensorManager mSensorManager;
    private TelephonyManager mTelephonyMgr;
    private Sensor mAccelerometerSensor;
    private Context mContext;
    private LinkedList<float[]> mAccSensorEventList;
    private float mAccXSum = 0;
    private float mAccYSum = 0;
    private float mAccZSum = 0;
    private final float mThreshold = 0.1f;
    private final int mKey = 1;
    private final int mValueFixed = 0;
    private final int mValueMoving = 1;
    private int mDetectEventNumer = 3;
    private int mCurrentState = 0; // 0=Fixed, 1=Moving
    private long mStartMotionLessTime = -1L;
    private boolean mIsActive;

    public GsensorListener(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelephonyMgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        update();
    }

    public void update() {
        if (mAccelerometerSensor == null) {
            mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public void Register() {
        mSensorManager.registerListener(sensorEventListener, mAccelerometerSensor, 100000);
        mAccSensorEventList = new LinkedList();
        mIsActive = true;
        setTransmitPower(mKey, mValueFixed);
    }

    public void unRegister() {
        mSensorManager.unregisterListener(sensorEventListener, mAccelerometerSensor);
        clearAccSensorData();
        // mCurrentState = 0;
        mIsActive = false;
        mStartMotionLessTime = -1L;
        resetDefaulState();
    }

    SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}

                @Override
                public void onSensorChanged(SensorEvent event) {
                    addAccSensorData(event);
                    if (mAccSensorEventList.size() < mDetectEventNumer) return;
                    try {
                        handleMotionDetection();
                        removeFirstAccSensorData();
                        return;
                    } catch (java.util.NoSuchElementException paramSensorEvent) {
                        Log.e(
                                TAG,
                                "[SW_SAR] onSensorChanged: Acc-sensor was already disabled. Ignore:"
                                        + " "
                                        + paramSensorEvent);
                    }
                }
            };

    private void addAccSensorData(SensorEvent Event) {
        mAccXSum += Event.values[0];
        mAccYSum += Event.values[1];
        mAccZSum += Event.values[2];
        mAccSensorEventList.add(Arrays.copyOf(Event.values, Event.values.length));
    }

    private void handleMotionDetection() {

        if (isMotionDetectedByAcc() && mCurrentState != mValueMoving) {
            mCurrentState = 1;
            setTransmitPower(mKey, mCurrentState);
        } else if (!isMotionDetectedByAcc() && mCurrentState != mValueFixed) {
            mCurrentState = 0;
            setTransmitPower(mKey, mCurrentState);
        }
    }

    private boolean isMotionDetectedByAcc() {
        float[] firstEvent = (float[]) mAccSensorEventList.getFirst();
        float[] lastEvent = (float[]) mAccSensorEventList.getLast();
        float mAccXSecond = mAccXSum - (firstEvent[0] + lastEvent[0]);
        float mAccYSecond = mAccYSum - (firstEvent[1] + lastEvent[1]);
        float mAccZSecond = mAccZSum - (firstEvent[2] + lastEvent[2]);
        int detectMotion = 0;
        if (Math.abs(mAccXSecond - firstEvent[0]) > mThreshold
                || Math.abs(mAccYSecond - firstEvent[1]) > mThreshold
                || Math.abs(mAccZSecond - firstEvent[2]) > mThreshold) {
            detectMotion += 1;
        }
        if (Math.abs(lastEvent[0] - mAccXSecond) > mThreshold
                || Math.abs(lastEvent[1] - mAccYSecond) > mThreshold
                || Math.abs(lastEvent[2] - mAccZSecond) > mThreshold) {
            detectMotion += 1;
        }
        if (detectMotion == mDetectEventNumer - 1) {
            mStartMotionLessTime = -1L;
            return true;
        } else {
            long mCurrentTime = SystemClock.elapsedRealtime();
            if (mStartMotionLessTime == -1L) mStartMotionLessTime = mCurrentTime;
            if ((mCurrentTime - mStartMotionLessTime < 500)) {
                return true;
            }
            return false;
        }
    }

    private void removeFirstAccSensorData() {
        if (mAccSensorEventList.size() == 0) return;
        float[] arrayOfFloat = (float[]) mAccSensorEventList.removeFirst();
        mAccXSum -= arrayOfFloat[0];
        mAccYSum -= arrayOfFloat[1];
        mAccZSum -= arrayOfFloat[2];
    }

    private void clearAccSensorData() {
        mAccSensorEventList.clear();
        mAccXSum = 0;
        mAccYSum = 0;
        mAccZSum = 0;
    }

    public void resetDefaulState() {
        if (!getActive() && mCurrentState == mValueMoving) {
            setTransmitPower(mKey, mValueFixed);
            mCurrentState = mValueFixed;
        }
    }

    private void setTransmitPower(int key, int value) {
        Log.d(TAG, "[SW_SAR] GsensorListener setTransmitPower key = " + key + ", value = " + value);
        if (mTelephonyMgr != null) {
            mTelephonyMgr.setTransmitPower(key, value);
        }
    }

    public boolean getActive() {
        return mIsActive;
    }
}
