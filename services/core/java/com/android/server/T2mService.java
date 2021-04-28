package com.android.server;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class T2mService extends SystemService {
    static final String TAG = "T2mService";

    private Context context = getContext();


    public T2mService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        resetSettingsforIms();
    }

    private void resetSettingsforIms() {
        Settings.Global.putInt(context.getContentResolver(), "nv_ims_enable", 0);
        Settings.Global.putInt(context.getContentResolver(), "flag_ims_nv_bootup_check", 0);
        Log.i(TAG,
                "BackupImsProfile->onReceive: sim card may be pulled out when UE is shutdown,so we set nv_ims_enable to 0 for rechecking.");
    }

}