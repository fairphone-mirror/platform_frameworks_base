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

import android.content.Context;
import android.widget.Switch;
import android.widget.Toast;

// Handle wind noise enabling and disabling during capture
class WnrSwitch extends OzoSwitch {
    private Context mContext;
    private OzoLevelMeterView mWindLevelView;

    public WnrSwitch(Context context, OzoLevelMeterView windLevelView, Switch switchView) {
        super(switchView);
        mContext = context;
        mWindLevelView = windLevelView;
    }

    @Override
    public void enabled() {
        mWindLevelView.open();
        if (mListener != null) {
            if (mListener.onRecordEnableWindscreen() == true)
                Toast.makeText(mContext.getApplicationContext(), "WNR enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void disabled() {
        if (mListener != null) {
            if (mListener.onRecordDisableWindscreen() == true)
                Toast.makeText(mContext.getApplicationContext(), "WNR disabled", Toast.LENGTH_SHORT).show();
        }

        mWindLevelView.setLevel(0);
        mWindLevelView.close();
    }
}
