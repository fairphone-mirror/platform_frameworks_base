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
import android.telephony.TelephonyManager;
import android.util.Log;

public class PsensorListener {
    private final String TAG = "PsensorListener";
    private SensorManager mSensorManager;
    private TelephonyManager mTelephonyMgr;
    private Sensor mProximitySensor;
    private Context mContext;
    float mPsensorMaxRange = 0.0F;
    private final int mKey = 2;
    private final int mValueFar = 0;
    private final int mValueNear = 2;
    private int mCurrentState = 0; // 0=far, 2=near
    private boolean mIsActive = false;

    public PsensorListener(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelephonyMgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        update();
    }

    public void update() {
        if (mProximitySensor == null) {
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        if (mProximitySensor != null) {
            mPsensorMaxRange = mProximitySensor.getMaximumRange();
        } else {
            mPsensorMaxRange = 0.0F;
            Log.e(
                    TAG,
                    "[SW_SAR] Psensor abnormal, using default value for mPsensorMaxRange = "
                            + mPsensorMaxRange);
        }
    }

    public void Register() {
        mSensorManager.registerListener(
                sensorEventListener, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        mIsActive = true;
    }

    public void unRegister() {
        mSensorManager.unregisterListener(sensorEventListener, mProximitySensor);
        mIsActive = false;
        resetDefaulState();
    }

    SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}

                @Override
                public void onSensorChanged(SensorEvent event) {
                    float f = event.values[0];
                    int mDetectState;
                    if (f < mPsensorMaxRange) {
                        mDetectState = 2; // near
                    } else {
                        mDetectState = 0; // far
                    }
                    handleDistUpdate(mDetectState);
                }
            };

    private void handleDistUpdate(int value) {
        if (mCurrentState != value) {
            Log.d(
                    TAG,
                    "[SW_SAR] PsensorListener CurrentState = "
                            + mCurrentState
                            + " OldState = "
                            + value);
            mCurrentState = value;
            setTransmitPower(mKey, mCurrentState);
        }
    }

    public void resetDefaulState() {
        if (!getActive() && mCurrentState == mValueNear) {
            setTransmitPower(mKey, mValueFar);
            mCurrentState = mValueFar;
        }
    }

    private void setTransmitPower(int key, int value) {
        Log.d(TAG, "[SW_SAR] PsensorListener setTransmitPower key = " + key + ", value = " + value);
        if (mTelephonyMgr != null) {
            mTelephonyMgr.setTransmitPower(key, value);
        }
    }

    public boolean getActive() {
        return mIsActive;
    }
}
