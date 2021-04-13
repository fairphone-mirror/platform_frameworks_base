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

class OzoView {
    protected View mView;
    protected OzoViewListener mListener;

    public OzoView(View view) {
        mView = view;
    }

    public void setListener(OzoViewListener listener) {
        mListener = listener;
    }

    public View getView() {
        return mView;
    }

    public void toggleVisibility() {
        if(mView.getVisibility() == View.GONE)
            show();
        else
            hide();
    }

    public void hide() {
        mView.setVisibility(View.GONE);
    }

    public void show() {
        mView.setVisibility(View.VISIBLE);
    }
}
