package com.android.server.pm;

import java.util.ArrayList;

import android.os.PersistableBundle;
import android.os.ServiceManager;
import android.os.Handler;
import android.content.Context;

import android.content.pm.IPackageManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.Process;


public class AppStateController {

    private final String TAG = "AppStateController";

    private static final String KEY_CARRIER_PREINSTALL = "carrier_preinstall";

    private static final String CARRIER_PREINSTALL_ARRAY[] = {"de.telekom.tsc","com.aura.oobe.deutsche", "com.orange.aura.oobe", "com.orange.update","com.gohappy.mobileapp","com.fetself","com.omusic.gPhone","net.fetnet.fetvod","com.fet.fridaywallet"};

    private Context mContext;

    private Handler mHandler;

    private ArrayList<AppState> mAppStateArrayList = new ArrayList();

    private boolean mHasSetAppState;

    private boolean hasSimStateChanged = false;

    private boolean mIsCarrierConfigLoaded = false;

    private boolean mIsCarrierConfigReveiver = false;


    private int mUserId;

    /**
     * @param context
     * @param handler
     * @hide
     */
    public AppStateController(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        mUserId = Process.myUserHandle().myUserId();
        registerReceiver();
        initCarrierAppList();
        setPreInstallCarrierApkState();
        Log.d(TAG, "AppStateController init");
    }

    private void initCarrierAppList() {
        for (String pkg : CARRIER_PREINSTALL_ARRAY) {
            AppState appState = new AppState();
            appState.installState = false;
            appState.pkgName = pkg;
            mAppStateArrayList.add(appState);
        }
    }

    private void updateCarrierAppState() {
        CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        //PersistableBundle config = configManager.getConfig();
        PersistableBundle config = configManager.getConfigLocked();
        if (config != null) {
            String[] preInstallApps = config.getStringArray(KEY_CARRIER_PREINSTALL);
            if (preInstallApps != null && preInstallApps.length > 0) {
                Log.d(TAG, "get carrier config preintall count = " + preInstallApps.length);
                for (String app : preInstallApps) {
                    Log.d(TAG, "get carrier config preintall " + app);
                    for (AppState appState : mAppStateArrayList) {
                        if (appState.pkgName.equals(app)) {
                            appState.installState = true;
                            if (!mIsCarrierConfigLoaded) {
                                mIsCarrierConfigLoaded = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void registerReceiver() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        //mIntentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mIntentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
        mIntentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mIntentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_USER_SWITCHED);
        mIntentFilter.addAction(Intent.ACTION_USER_REMOVED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
                    int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0);
                    if (userId != mUserId) {
                        Log.d(TAG, "ACTION_USER_SWITCHED" + " old user id = " + userId + " new user id = " + mUserId);
                        mUserId = userId;
                        updateCarrierAppState();
                        setPreInstallCarrierApkState();
                    }

                }
                if (TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(intent.getAction())) {
                    hasSimStateChanged = true;
                    int simStatus = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE, -99);
                    Log.d(TAG, "ACTION_SIM_APPLICATION_STATE_CHANGED" + " simStatus= " + simStatus);
                }
                //if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !hasSimStateChanged){
                if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
                    Log.d(TAG, "LOCKED_BOOT_COMPLETED");
//                    if (!mHasSetAppState && mIsCarrierConfigReveiver) {
//                        setPreInstallCarrierApkState();
//                        mHasSetAppState = true;
//                    }
                }

                if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                    Log.d(TAG, "ACTION_BOOT_COMPLETED");
//                    if (!mHasSetAppState && mIsCarrierConfigReveiver) {
//                        setPreInstallCarrierApkState();
//                        mHasSetAppState = true;
//                    }
                }

                if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "ACTION_CARRIER_CONFIG_CHANGED");
                    updateCarrierAppState();
                    mHandler.postDelayed(() -> judgeAndFireSetAppState(),200);

                }
            }
        }, mIntentFilter);
    }

    private void judgeAndFireSetAppState() {
        if (!mIsCarrierConfigLoaded || mHasSetAppState) {
            return;
        }
        setPreInstallCarrierApkState();
        mHasSetAppState = true;
    }

    private void setPreInstallCarrierApkState() {
        IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        for (AppState appState : mAppStateArrayList) {
            updateInstallState(appState.pkgName, appState.installState, mIPm);
        }
    }

    private void updateInstallState(String pkg, boolean isSimAppropriate, IPackageManager ipm) {
        Log.d(TAG, pkg + " updateInstallState sim Appropriate  " + isSimAppropriate + " mUserId = " + mUserId);
        try {
            ipm.setSystemAppInstallState(pkg, isSimAppropriate, mUserId);
        } catch (Exception e) {
            Log.d(TAG, pkg + " updateInstallState error " + e.getMessage());
        }

    }

    private class AppState {
        public String pkgName;
        public boolean installState;
    }

}
