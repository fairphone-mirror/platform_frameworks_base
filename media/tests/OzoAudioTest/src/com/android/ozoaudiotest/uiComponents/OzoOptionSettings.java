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

import android.widget.Switch;
import android.widget.RadioGroup;

// Record option settings
public class OzoOptionSettings implements OzoSettings {
    // Options view
    OzoView mOptionsView;
    // Button that opens options view
    OzoButton mOptionsButton;

    // Enable/disable OZO Tune
    private OptionsSwitch mOzoTuneSwitch;
    // Enable/disable wind level at the start of capture
    private OptionsSwitch mWnrEnabledSwitch;
    // Enable/disable wind level notifications
    private OptionsSwitch mWnrNotificationSwitch;
    // Enable/disable sound source localization notifications
    private OptionsSwitch mSSLocNotificationSwitch;
    // Enable/disable audio level notifications
    private OptionsSwitch mAudioLevelsNotificationSwitch;
    // Enable/disable noise suppression at the start of capture
    private OptionsSwitch mNsEnabledSwitch;
    // Recording mode selection
    private OptionsRadio mRecModeGroup;

    private class OptionsSwitch extends OzoSwitch {
        public OptionsSwitch(Switch switchEl) {
            super(switchEl);
        }

        @Override
        public void enabled() {}

        @Override
        public void disabled() {}

        public boolean isChecked() {
            return ((Switch) this.getView()).isChecked();
        }
    }

    private class OptionsRadio extends OzoRadio {
        private RecordMode mRecMode;

        public OptionsRadio(RecordMode recMode) {
            super(recMode.getGroupElement());
            mRecMode = recMode;
        }

        @Override
        public void checked(int id) {
            if (mListener != null) {
                mListener.onRecordStateChanged(mRecMode.setRecordState(id));
            }
        }

        public boolean isOzoAudio() {
            return mRecMode.isOzoAudio();
        }

        public int getRecordState() {
            return mRecMode.getRecordState();
        }
    }

    public OzoOptionSettings(OzoView view, OzoButton button, Switch ozoTune, Switch wnrNotifications,
        Switch wnrEnabled, Switch sslocNotifications, Switch nsEnabled, Switch levelNotifications,
        RecordMode recMode) {
        mOptionsView = view;
        mOptionsButton = button;

        mOzoTuneSwitch = new OptionsSwitch(ozoTune);
        mWnrEnabledSwitch = new OptionsSwitch(wnrEnabled);
        mWnrNotificationSwitch = new OptionsSwitch(wnrNotifications);
        mSSLocNotificationSwitch = new OptionsSwitch(sslocNotifications);
        mNsEnabledSwitch = new OptionsSwitch(nsEnabled);
        mRecModeGroup = new OptionsRadio(recMode);
        mAudioLevelsNotificationSwitch = new OptionsSwitch(levelNotifications);
    }

    public void setListener(OzoViewListener listener) {
        mRecModeGroup.setListener(listener);
    }

    public boolean isOzoAudio() {
        return mRecModeGroup.isOzoAudio();
    }

    public int getRecordState() {
        return mRecModeGroup.getRecordState();
    }

    public boolean isOzoTune() {
        return mOzoTuneSwitch.isChecked();
    }

    public boolean isWnrEnabled() {
        return mWnrEnabledSwitch.isChecked();
    }

    public boolean isNsEnabled() {
        return mNsEnabledSwitch.isChecked();
    }

    public boolean isSSLocNotificationsEnabled() {
        return mSSLocNotificationSwitch.isChecked();
    }

    public boolean isWnrNotificationsEnabled() {
        return mWnrNotificationSwitch.isChecked();
    }

    public boolean isAudioLevelsNotificationsEnabled() {
        return mAudioLevelsNotificationSwitch.isChecked();
    }
}
