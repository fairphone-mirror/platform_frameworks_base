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

// Handle custom processing enabling and disabling during capture
class CustomProcSwitch extends OzoSwitch {
    private Context mContext;

    public CustomProcSwitch(Context context, Switch switchView) {
        super(switchView);
        mContext = context;
    }

    @Override
    public void enabled() {
        if (mListener != null) {
            if (mListener.onRecordEnableCustomProcessing() == true)
                Toast.makeText(mContext.getApplicationContext(), "Custom processing enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void disabled() {
        if (mListener != null) {
            if (mListener.onRecordDisableCustomProcessing() == true)
                Toast.makeText(mContext.getApplicationContext(), "Custom processing disabled", Toast.LENGTH_SHORT).show();
        }
    }
}
