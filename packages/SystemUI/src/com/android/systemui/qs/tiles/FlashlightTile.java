/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.service.quicksettings.Tile;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.FlashlightController;

import javax.inject.Inject;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.os.SystemProperties;

/**
 * Quick settings tile: Control flashlight
 **/
public class FlashlightTile extends QSTileImpl<BooleanState> implements
        FlashlightController.FlashlightListener {

    public static final String TILE_SPEC = "flashlight";
    private final FlashlightController mFlashlightController;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private AlarmManager alarmManager;
    private PendingIntent updateIntent_reduce;
    private PendingIntent updateIntent_close;
    private int mAlarmState = 0;

    private static final String ACTION_REDUCE_FLASHLIGHT = "action.t2m.reduce.flashlight";
    private static final String ACTION_CLOSE_FLASHLIGHT = "action.t2m.close.flashlight";
    private static final long FIRST_DELAY_TIME = 3*60*1000;
    private static final long CLOSE_DELAY_TIME = 1*60*1000;


    @Inject
    public FlashlightTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            FlashlightController flashlightController
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mFlashlightController = flashlightController;
        mFlashlightController.observe(getLifecycle(), this);
        alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        mFilter = new IntentFilter(ACTION_REDUCE_FLASHLIGHT);
        mFilter.addAction(ACTION_CLOSE_FLASHLIGHT);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!mFlashlightController.isAvailable() || !mFlashlightController.isEnabled()) {
                    mAlarmState = 0;
                    SystemProperties.set("persist.sys.setflashlight","0");
                    return;
                }
                if(ACTION_REDUCE_FLASHLIGHT.equals(action)){
                    mAlarmState = 2;
                    SystemProperties.set("persist.sys.setflashlight","1");
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CLOSE_DELAY_TIME, updateIntent_close);
                }else if(ACTION_CLOSE_FLASHLIGHT.equals(action)){
                    mAlarmState = 0;
                    refreshState(false);
                    mFlashlightController.setFlashlight(false);
                    SystemProperties.set("persist.sys.setflashlight","0");
                }
            }
        };
        mContext.registerReceiver(mReceiver, mFilter);
        updateIntent_reduce = PendingIntent.getBroadcast(mContext, 0,new Intent(ACTION_REDUCE_FLASHLIGHT), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        updateIntent_close = PendingIntent.getBroadcast(mContext, 0,new Intent(ACTION_CLOSE_FLASHLIGHT), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
    }

    @Override
    public boolean isAvailable() {
        return mFlashlightController.hasFlashlight();
    }

    @Override
    protected void handleClick(@Nullable View view) {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
        boolean newState = !mState.value;
        if(newState){
            mAlarmState = 1;
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + FIRST_DELAY_TIME,updateIntent_reduce);
        }else{
            if(mAlarmState == 1){
                alarmManager.cancel(updateIntent_reduce);
                mAlarmState = 0;
            }else if(mAlarmState == 2){
                alarmManager.cancel(updateIntent_close);
                mAlarmState = 0;
            }
        }
        refreshState(newState);
        mFlashlightController.setFlashlight(newState);
        SystemProperties.set("persist.sys.setflashlight","0");
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_flashlight_label);
    }

    @Override
    protected void handleLongClick(@Nullable View view) {
        handleClick(view);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        state.secondaryLabel = "";
        state.stateDescription = "";
        if (!mFlashlightController.isAvailable()) {
            state.secondaryLabel = mContext.getString(
                    R.string.quick_settings_flashlight_camera_in_use);
            state.stateDescription = state.secondaryLabel;
            state.state = Tile.STATE_UNAVAILABLE;
            state.icon = ResourceIcon.get(R.drawable.qs_flashlight_icon_off);
            return;
        }
        if (arg instanceof Boolean) {
            boolean value = (Boolean) arg;
            if (value == state.value) {
                return;
            }
            state.value = value;
        } else {
            state.value = mFlashlightController.isEnabled();
        }
        state.contentDescription = mContext.getString(R.string.quick_settings_flashlight_label);
        state.expandedAccessibilityClassName = Switch.class.getName();
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.icon = ResourceIcon.get(state.value
                ? R.drawable.qs_flashlight_icon_on : R.drawable.qs_flashlight_icon_off);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_FLASHLIGHT;
    }

    @Override
    public void onFlashlightChanged(boolean enabled) {
        if(!enabled){
            if(mAlarmState == 1){
                alarmManager.cancel(updateIntent_reduce);
                mAlarmState = 0;
            }else if(mAlarmState == 2){
                alarmManager.cancel(updateIntent_close);
                mAlarmState = 0;
                SystemProperties.set("persist.sys.setflashlight","0");
            }
        }
        refreshState(enabled);
    }

    @Override
    public void onFlashlightError() {
        refreshState(false);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        if(!available){
            if(mAlarmState == 1){
                alarmManager.cancel(updateIntent_reduce);
                mAlarmState = 0;
            }else if(mAlarmState == 2){
                alarmManager.cancel(updateIntent_close);
                mAlarmState = 0;
                SystemProperties.set("persist.sys.setflashlight","0");
            }
        }
        refreshState();
    }
}
