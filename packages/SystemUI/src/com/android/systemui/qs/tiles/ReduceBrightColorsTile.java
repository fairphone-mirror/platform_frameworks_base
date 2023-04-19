/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import static com.android.systemui.qs.dagger.QSFlagsModule.RBC_AVAILABLE;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R.drawable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.ReduceBrightColorsController;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;

import javax.inject.Inject;
import javax.inject.Named;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.content.Context;
import android.provider.Settings.Secure;
import android.hardware.Sensor;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

/** Quick settings tile: Reduce Bright Colors **/
public class ReduceBrightColorsTile extends QSTileImpl<QSTile.BooleanState>
        implements ReduceBrightColorsController.Listener{

    public static final String TILE_SPEC = "reduce_brightness";
    private final boolean mIsAvailable;
    private final ReduceBrightColorsController mReduceBrightColorsController;
    private boolean mIsListening;
    private final SensorManager mSensorManager;
    private int mSmallLuxCounter = 0;
    private int mLageLuxCounter = 0 ;
    private final float CAN_ENTRY_EXTRA_DIM_VALUE = 80;
    private boolean mRegistLightsensor = false;

    @Inject
    public ReduceBrightColorsTile(
            @Named(RBC_AVAILABLE) boolean isAvailable,
            ReduceBrightColorsController reduceBrightColorsController,
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mReduceBrightColorsController = reduceBrightColorsController;
        mReduceBrightColorsController.observe(getLifecycle(), this);
        mIsAvailable = isAvailable;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mLightSensorListener,mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT_BACK),
                  SensorManager.SENSOR_DELAY_NORMAL);
        mRegistLightsensor = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(mReceiver, filter);

    }

    @Override
    public boolean isAvailable() {
        return mIsAvailable;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                  if(!mRegistLightsensor){
                      mSensorManager.registerListener(mLightSensorListener,mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT_BACK),
                          SensorManager.SENSOR_DELAY_NORMAL);
                      mRegistLightsensor=true;
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    mSensorManager.unregisterListener(mLightSensorListener);
                    mRegistLightsensor=false;
                    break;
            }
        }
    };

    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            final float lux = event.values[0];
            if(lux <= CAN_ENTRY_EXTRA_DIM_VALUE){
                mSmallLuxCounter++;
                mLageLuxCounter = 0;
            }else {
                mSmallLuxCounter = 0;
                mLageLuxCounter++;
            }
            if(mLageLuxCounter == 10){
                mReduceBrightColorsController.setReduceBrightColorsActivated(false);
                Secure.putInt(mContext.getContentResolver(),Secure.ENABLE_REDUCE_BRIGHT_COLORS,0);
                Secure.putString(mContext.getContentResolver(),Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,"");
                Secure.putString(mContext.getContentResolver(),Secure.ACCESSIBILITY_BUTTON_TARGETS,"");
            }
            if(mSmallLuxCounter == 10){
                Secure.putInt(mContext.getContentResolver(),Secure.ENABLE_REDUCE_BRIGHT_COLORS,1);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used.
        }
    };

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_REDUCE_BRIGHT_COLORS_SETTINGS);
    }

    @Override
    protected void handleClick(@Nullable View view) {
        boolean isEnableExtraDim = Secure.getInt(mContext.getContentResolver(),Secure.ENABLE_REDUCE_BRIGHT_COLORS,0) == 1;
        if(isEnableExtraDim){
            mReduceBrightColorsController.setReduceBrightColorsActivated(!mState.value);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.reduce_bright_colors_feature_name);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = mReduceBrightColorsController.isReduceBrightColorsActivated();
        boolean isEnableExtraDim = Secure.getInt(mContext.getContentResolver(),Secure.ENABLE_REDUCE_BRIGHT_COLORS,0) == 1;
        if(isEnableExtraDim){
            state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        } else {
            state.state = Tile.STATE_UNAVAILABLE;
        }
        state.label = mContext.getString(R.string.reduce_bright_colors_feature_name);
        state.expandedAccessibilityClassName = Switch.class.getName();
        state.contentDescription = state.label;
        state.icon = ResourceIcon.get(state.value
                ? drawable.qs_extra_dim_icon_on
                : drawable.qs_extra_dim_icon_off);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void onActivated(boolean activated) {
        refreshState();
    }
}
