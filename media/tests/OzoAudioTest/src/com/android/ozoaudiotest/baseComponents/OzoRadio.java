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
import android.widget.RadioGroup;

abstract class OzoRadio extends OzoView {
    protected RadioGroup mGroup;

    public OzoRadio(RadioGroup group) {
        super(group);

        mGroup = group;
        mGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                checked(checkedId);
            }
        });
    }

    abstract public void checked(int id);
}
