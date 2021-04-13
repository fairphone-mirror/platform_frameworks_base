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

import android.widget.Button;

// Handle view visibility via click event
public class Click2ViewButton extends OzoButton {
    private OzoView mOptionsView;

    public Click2ViewButton(OzoView optionsView, Button button) {
        super(button);
        mOptionsView = optionsView;
    }

    @Override
    public void clicked() {
        // When clicked show the view
        mOptionsView.toggleVisibility();
    }
}
