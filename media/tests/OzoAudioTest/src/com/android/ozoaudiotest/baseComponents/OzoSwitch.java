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

import android.view.View;
import android.widget.Switch;
import android.widget.CompoundButton;

abstract class OzoSwitch extends OzoView {
    protected Switch mSwitch;

    public OzoSwitch(Switch switchView) {
        super(switchView);

        mSwitch = switchView;
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bChecked) {
                if (bChecked) {
                    enabled();
                } else {
                    disabled();
                }
            }
        });
    }

    public void setChecked(boolean state) {
        mSwitch.setChecked(state);
    }

    abstract public void enabled();

    abstract public void disabled();
}
