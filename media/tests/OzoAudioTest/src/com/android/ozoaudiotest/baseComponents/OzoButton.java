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
import android.widget.Button;

abstract class OzoButton extends OzoView {
    protected Button mButton;

    public OzoButton(Button button) {
        super(button);

        mButton = button;
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                clicked();
            }
        });
    }

    public void setText(String text) {
        mButton.setText(text);
    }

    public void reset() {}

    abstract public void clicked();
}
