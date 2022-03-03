package com.android.server.display;

import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.hardware.input.InputManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.view.Display;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.util.Slog;
import com.android.server.UiThread;
import com.android.server.wm.PointerEventDispatcher;


import java.util.ArrayList;
public class AuxiliarySensorController {

    private static final String TAG = "AuxiliarySensorController";

    private static AuxiliarySensorController mInstance;
    private Context mContext;
    private Sensor mGyroSensor;
    private Sensor mGSensor;
    private Sensor mLightSensor;

    private boolean  mFeatureSupport= false;
    private boolean  mEnableGyroState = false;
    private boolean  mGyroChangeState = false;
    private boolean  mGChangeState = false;
    private boolean  mAccelerationState = false;
    private boolean  mLightState = false;
    private boolean  mEnableSensorAuxiliary = false;
    private boolean  mEnableAcceleration = false;
    private boolean  mGsensorState = false;
    private static final double R2D = 180.0f / (double) Math.PI;
    private static final double NS2S = 1.0f / 1000000000.0f;
    private double integratedValues[] = new double[3];
    private double lastValues[] = new double[3];
    private double timestamp = 0;
    private static final int GYRO_THRESHOLD = 23;
    private static final int CLEAR_THRESHOLD = 90;
    private static final int SLOPE_VALUE = 5;
    private static final int ACCELERATION_VALUE = 12;
    private static final int GSENSOR_THRESHOLD = 7;
    private int mCallStatus = 0;
    private int mPendingProximity = PROXIMITY_UNKNOWN;

    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private boolean mAuxiliarySensorEnabled;
    private boolean mDisplayStateOn = true;
    private AuxiliaryDisplayListener displayListener;

    // The sensor manager.
    private SensorManager mSensorManager;

    private Callbacks mCallbacks;
    private TelephonyManager mTelephonyManager;
    private PowerManager.WakeLock mWakeLock;

    public static synchronized AuxiliarySensorController getInstance() {
        if (mInstance == null) {
            mInstance = new AuxiliarySensorController();
        }
        return mInstance;
    }

    protected AuxiliarySensorController() {
    }

    public void init(Context context, SensorManager sensorManager, Callbacks callbacks) {
        mSensorManager = sensorManager;
        mContext = context;
        final PackageManager pm = mContext.getPackageManager();
        mFeatureSupport = (pm != null) /*&& pm.hasSystemFeature("vendor.tct.sensor.auxiliary")*/;
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        displayListener =
            new AuxiliaryDisplayListener(
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE));
        mCallbacks = callbacks;
    }

    public boolean setAuxiliarySensorState(boolean positive,int pendingProximity) {
        if (!mFeatureSupport) {
            return false;
        }
        if (pendingProximity != PROXIMITY_POSITIVE && positive
                && mGyroChangeState && mGsensorState) {
            mDisplayStateOn = false;
            initSensorAuxiliaryState();
            mEnableSensorAuxiliary = true;
            Slog.d(TAG, "Sensor auxiliary start " );
        }

        if (pendingProximity == PROXIMITY_POSITIVE) {
            if (positive) {
                resetSensorAuxiliaryState();
                Slog.d(TAG, "Sensor Auxiliary reset" );
            } else {
                if (mCallStatus == TelephonyManager.CALL_STATE_OFFHOOK && mEnableSensorAuxiliary) {
                    mEnableGyroState = true;
                    acquireWakeLock();
                    Slog.d(TAG, "Sensor auxiliary enable" );
                    return true;
                }
            }
        }
        return false;
    }

    public void setPendingProximity(int pendingProximity) {
        mPendingProximity = pendingProximity;
    }

    public void setAuxiliarySensorEnabled(boolean enable) {
        if (!mFeatureSupport) {
            return;
        }
        if (enable) {
            if (!mAuxiliarySensorEnabled) {
                mAuxiliarySensorEnabled = true;
                mSensorManager.registerListener(mAuxiliarySensorListener, mGyroSensor,
                        SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(mAuxiliarySensorListener, mGSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mAuxiliarySensorListener, mLightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
                mCallStatus = mTelephonyManager.getCallState();
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                displayListener.register();
            }
        } else {
            if (mAuxiliarySensorEnabled) {
                mAuxiliarySensorEnabled = false;
                mPendingProximity = PROXIMITY_UNKNOWN;
                mSensorManager.unregisterListener(mAuxiliarySensorListener);
                mTelephonyManager.listen(mPhoneStateListener, 0);
                initSensorAuxiliaryState();
                displayListener.unregister();
            }
        }
    }

    public boolean getAuxiliarySensorState() {
        if (!mFeatureSupport) {
            return false;
        }
        return mEnableGyroState;
    }

    public interface Callbacks {
        void powerOnByAuxiliarySensor();
    }

    private final SensorEventListener mAuxiliarySensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if (mDisplayStateOn) {
                    mGyroChangeState = checkGyroStatus(event);
                } else  if ( mEnableGyroState && (mCallStatus != TelephonyManager.CALL_STATE_OFFHOOK
                        || (checkGyroStatus(event) && mGChangeState) || mAccelerationState || mLightState)) {
                    mCallbacks.powerOnByAuxiliarySensor();
                    Slog.d(TAG, "AuxiliarySensor set wakeup");
                    initSensorAuxiliaryState();
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                mGChangeState = (Math.abs(x) < SLOPE_VALUE || Math.abs(y) <SLOPE_VALUE);
                mGsensorState =  Math.abs(z) < GSENSOR_THRESHOLD;
                mAccelerationState = ((float) Math.sqrt(x*x + y*y +z*z) > ACCELERATION_VALUE );
            }

            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                int value = (int) event.values[0];
                android.util.Log.e(TAG,"onSensorChanged TYPE_LIGHT value = "+value);
                if (value > 20) {
                    mLightState = true;
                }else{
                    mLightState = false;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used.
        }
    };

    private void onDisplayStateChanged(boolean isDisplayOn) {
        mDisplayStateOn = isDisplayOn;
        if (isDisplayOn && mPendingProximity != PROXIMITY_POSITIVE) {
            initSensorAuxiliaryState();
        }
    }

    private void initSensorAuxiliaryState() {
        resetSensorAuxiliaryState();
        mEnableSensorAuxiliary = false;
    }

    private void resetSensorAuxiliaryState() {
        timestamp = 0;
        integratedValues[0] = 0;
        integratedValues[1] = 0;
        integratedValues[2] = 0;
        mGyroChangeState = false;
        mGChangeState = false;
        mAccelerationState = false;
        mLightState = false;
        mGsensorState = false;
        mEnableGyroState = false;
        releaseWakeLock();
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager powerManager =
                    (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
    }

    private boolean checkGyroStatus(SensorEvent event) {
        if (timestamp == 0) {
            timestamp = event.timestamp;
            lastValues[0] = event.values[0];
            lastValues[1] = event.values[1];
            lastValues[2] = event.values[2];
        } else {
            final double dT = (event.timestamp - timestamp) * NS2S;
            integratedValues[0] += (event.values[0] + lastValues[0]) / 2.0f * dT;
            integratedValues[1] += (event.values[1] + lastValues[1]) / 2.0f * dT;
            integratedValues[2] += (event.values[2] + lastValues[2]) / 2.0f * dT;
            double x = integratedValues[0] * R2D;
            double y = integratedValues[1] * R2D;
            double z = integratedValues[2] * R2D;
            timestamp = event.timestamp;
            lastValues[0] = event.values[0];
            lastValues[1] = event.values[1];
            lastValues[2] = event.values[2];
            return getStateEnable(Math.abs(x), Math.abs(y), Math.abs(z));
        }
        return false;
    }

    private boolean getStateEnable(double x, double y, double z) {
        boolean stateEnable = (x >= GYRO_THRESHOLD && y >= GYRO_THRESHOLD)
                || (y >= GYRO_THRESHOLD && z >= GYRO_THRESHOLD)
                || (x >= GYRO_THRESHOLD && z >= GYRO_THRESHOLD);
        if (!stateEnable) {
            if (x >= CLEAR_THRESHOLD) {
                integratedValues[0] = 0;
            } else if (y >= CLEAR_THRESHOLD) {
                integratedValues[1] = 0;
            } else if (z >= CLEAR_THRESHOLD) {
                integratedValues[2] = 0;
            }
        }
        return stateEnable;
    }

   private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
        mCallStatus = state;
      }
    };

    private final class AuxiliaryDisplayListener implements DisplayListener {

    private DisplayManager displayManager;
    private boolean isDisplayOn = true;

    AuxiliaryDisplayListener(DisplayManager displayManager) {
      this.displayManager = displayManager;
    }

    public void register() {
      displayManager.registerDisplayListener(this, null);
    }

    public void unregister() {
      displayManager.unregisterDisplayListener(this);
    }

    @Override
    public void onDisplayRemoved(int displayId) {}

    @Override
    public void onDisplayChanged(int displayId) {
      if (displayId == Display.DEFAULT_DISPLAY) {
        final Display display = displayManager.getDisplay(displayId);

        final boolean isDisplayOn = display.getState() != Display.STATE_OFF;
        // For call purposes, we assume that as long as the screen is not truly off, it is
        // considered on, even if it is in an unknown or low power idle state.
        if (isDisplayOn != this.isDisplayOn) {
          this.isDisplayOn = isDisplayOn;
          onDisplayStateChanged(this.isDisplayOn);
        }
      }
    }

    @Override
    public void onDisplayAdded(int displayId) {}
  }
}
