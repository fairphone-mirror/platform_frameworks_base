package com.android.server.pm;

import java.util.Arrays;
import java.util.ArrayList;

import android.os.UserHandle;
import android.os.PersistableBundle;
import android.os.ServiceManager;
import android.os.Handler;
import android.content.Context;

import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.Process;
import android.os.SystemProperties;


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
    private static final String DSDLOCKED_CARRIERID = "persist.radio.dsd.locked";
    private static final String DSDLOCKED_HASENABLEAPP = "persist.sys.dsd.enableapp";
    private static final String DSDLOCKED_REBOOT = "persist.radio.dsd.locked.reboot";

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
        boolean islock = SystemProperties.getBoolean(DSDLOCKED_CARRIERID, false);
        if(!islock){
            setPreInstallCarrierApkState();
        }
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
            Log.d(TAG, "config != null");
            String[] preInstallApps = config.getStringArray(KEY_CARRIER_PREINSTALL);
            if (preInstallApps != null && preInstallApps.length > 0) { //dsd lock carrier_install is not null
                Log.d(TAG, "get carrier config preintall count = " + preInstallApps.length);
                for (String app : preInstallApps) {
                    Log.d(TAG, "get carrier config preintall " + app);
                    for (AppState appState : mAppStateArrayList) {
                        if (appState.pkgName.equals(app)) {
                            appState.installState = true;
                            if (!mIsCarrierConfigLoaded) {
                                Log.d(TAG, "mIsCarrierConfigLoaded = true");
                                mIsCarrierConfigLoaded = true;
                            }
                        }
                    }
                }
            } else {  //dsd lock carrier_install is null

                config = configManager.getConfig();
                preInstallApps = config.getStringArray(KEY_CARRIER_PREINSTALL);

                if (preInstallApps != null && preInstallApps.length > 0) { //dsd lock carrier_install is not null
                    Log.d(TAG, "else get carrier config preintall count = " + preInstallApps.length);
                    
                    if("com.gohappy.mobileapp".equals(preInstallApps[0])){
                        for (String app : preInstallApps) {
                            Log.d(TAG, "else get carrier config preintall " + app);
                            for (AppState appState : mAppStateArrayList) {
                                if (appState.pkgName.equals(app)) {
                                    appState.installState = true;
                                    if (!mIsCarrierConfigLoaded) {
                                        Log.d(TAG, "mIsCarrierConfigLoaded = true");
                                        mIsCarrierConfigLoaded = true;
                                    }
                                }
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
        mIntentFilter.addAction(Intent.ACTION_USER_ADDED);
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
                if (Intent.ACTION_USER_ADDED.equals(intent.getAction())) {
                    int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0);
                    if (userId != mUserId) {
                        Log.d(TAG, "ACTION_USER_ADDED" + " old user id = " + userId + " new user id = " + mUserId);
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
        mHasSetAppState = SystemProperties.getBoolean(DSDLOCKED_HASENABLEAPP, false);
        boolean rebootafterfirstdsdlock  = SystemProperties.getBoolean(DSDLOCKED_REBOOT, false);
        if (!mIsCarrierConfigLoaded || mHasSetAppState || !rebootafterfirstdsdlock) {
            Log.d(TAG, " judgeAndFireSetAppState mIsCarrierConfigLoaded  " + mIsCarrierConfigLoaded + " mHasSetAppState = " + mHasSetAppState);
            return;
        }
        setPreInstallCarrierApkState();
        mHasSetAppState = true;
        SystemProperties.set(DSDLOCKED_HASENABLEAPP, "1");
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
            boolean success = ipm.setSystemAppInstallState(pkg, isSimAppropriate, mUserId);
            Log.d(TAG, pkg +" updateInstallState:" + success);
            if (!success && !isSimAppropriate){
                int[] userIds = UserManagerService.getInstance().getUserIdsIncludingPreCreated();
                for (int uid : userIds){
                    if (uid != UserHandle.USER_SYSTEM){
                        ipm.setApplicationHiddenSettingAsUser(pkg,true,uid);
                        ipm.setApplicationEnabledSetting(pkg,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.UNINSTALL_REASON_UNKNOWN,uid,mContext.getBasePackageName());
                    }
               }

            }
        } catch (Exception e) {
            Log.d(TAG, pkg + " updateInstallState error " + e.getMessage());
        }

    }

    private class AppState {
        public String pkgName;
        public boolean installState;
    }

}
