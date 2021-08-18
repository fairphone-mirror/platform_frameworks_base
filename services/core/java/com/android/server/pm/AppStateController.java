package com.android.server.pm;

import java.util.List;
import java.util.ArrayList;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Handler;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.os.Process;
import android.provider.Settings;

import static android.os.UserHandle.USER_SYSTEM;

import com.android.internal.telephony.PhoneConstants;


//import com.android.internal.telephony.SubscriptionController;

/* [TCT-ROM] create by tairan.hao for Task 9884474 on 2020-09-09
    move implenments and do refactor from SystemUI3.0/JoySystemUI/app/src/main/java/com/android/systemui/statusbar/phone/StatusBar.java
*/
public class AppStateController {

    private final String TAG = "AppStateController";

    private static final String SETTINGS_CONFIG_SIM_LOCK_GID = "sim_lock_gid";

    private static final String SETTINGS_CONFIG_SIM_LOCK_SPN = "sim_lock_spn";

    private static final String SETTINGS_CONFIG_SIM_LOCK_MCCMNC = "sim_lock_mccmnc";

    private Context mContext;
    private Handler mHandler;
    SubscriptionManager mSubscriptionManager;

    private String spn0, plmn0, iccid0;
    private String spn1, plmn1, iccid1;
    private String gid1_0, gid1_1;

    private MyCarrierInfo mMyCarrierInfo;

    private int simCount = 0;


    /**
     * @param context
     * @param handler
     * @hide
     */
    public AppStateController(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        registerReceiver();
    }

    /* =============================================================================================================  */
    /**
     * Orange
     */
    //Orange defect 8172436 8670766
    private final String[] orangeApps = {"com.orange.aura.oobe",
            "com.orange.update",
            "com.orange.clock",
            "com.orange.widgets.mostusedapps",
            "com.orange.widget.tips"};
    private final String[] orangeSIMPlmn = {"21403", "21421", "20801", "26003", "22610", "23101",
            "20610", "25901", "27099", "21419", "63203", "65202",
            "61302", "62402", "34001", "62303", "61101", "61203",
            "63086", "60201", "62701", "41677", "64700", "61807",
            "64602", "61002", "61701", "60400", "61404", "60801",
            "61901", "60501"};
    AppUnderControll orange = new AppUnderControll(orangeApps) {
        private boolean hasEnabled = false; //[TCT-ROM]Add by tairan.hao for 10635863 on 2020-01-12

        @Override
        public void setNewState() {
            //if not first boot, don't disable orange apps
            if (isFirstBoot() && !isSimAppropriate()) {
                orange.newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            }
            if (isSimAppropriate()) {
                orange.newState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            }
        }

        @Override
        public boolean isSimAppropriate() {
            String[] simyo = {"simyo"};
            return isSubInfoAppropriate(orangeSIMPlmn, plmn0) && !isSubInfoAppropriate(simyo, spn0) ||
                    isSubInfoAppropriate(orangeSIMPlmn, plmn1) && !isSubInfoAppropriate(simyo, spn1);
        }

        @Override
        public void updateAppState() {
            //1*1 widget
            ComponentName folderWidget = new ComponentName("com.orange.update", "com.orange.update.widget.FolderWidgetProvider");
            //2*1 widget
            ComponentName comboFolderWidget = new ComponentName("com.orange.update", "com.orange.update.widget.ComboFolderWidgetProvider");
            IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            for (String pkg : orange.pkgs) {

                updateInstallState(pkg, isSimAppropriate(), mIPm);

//                if (!isAppInstalled(pkg)) {
//                    continue;
//                }
//                try {
//                    //[TCT-ROM]Begin modify by tairan.hao for 10635863 on 2020-01-12
//                    if ("com.orange.update".equals(pkg)) {
//                        // if(orange.newState != mIPm.getComponentEnabledSetting(folderWidget, mContext.getUserId())){
//                        //     //Log.d(TAG,"set FolderWidgetProvider as " + orange.newState);
//                        // }
//                        Log.d(TAG, "updateAppState comboFolderWidget " + mIPm.getComponentEnabledSetting(comboFolderWidget, mContext.getUserId()));
//                        if (!hasEnabled && orange.newState != mIPm.getComponentEnabledSetting(comboFolderWidget, mContext.getUserId())) {
//                            mIPm.setComponentEnabledSetting(comboFolderWidget, orange.newState, orange.enableFlag, mContext.getUserId());
//                            mIPm.setComponentEnabledSetting(folderWidget, orange.newState, orange.enableFlag, mContext.getUserId());
//                            hasEnabled = orange.newState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
//                            Log.d(TAG, "set orange 1*1 and 2*1 widgets as " + orange.newState);
//                        }
//                        //[TCT-ROM]End modify by tairan.hao for 10635863 on 2020-01-12
//                    } else if (orange.newState != mIPm.getApplicationEnabledSetting(pkg, USER_SYSTEM)) {
//                        mIPm.setApplicationEnabledSetting(pkg, orange.newState, orange.enableFlag, USER_SYSTEM, "System");
//                        Log.d(TAG, "set " + pkg + " enable state as " + orange.newState);
//                    }
//                } catch (Exception e) {
//                    Log.i(TAG, "cannot set " + pkg + " as " + orange.newState);
//                }
            }
        }
    };

    /* =============================================================================================================  */
    /**
     * ATT
     */
    //ATT defect 8743303 10658538
    private final String[] attApps = {"com.att.miatt"};
    //[TCT-ROM]Begin modify by tairan.hao for 10691772 on 2021-02-04
    private final String[] attSIMPlmn = {"33405", "334050", "334090", "33470", "33450"};
    //[TCT-ROM]End modify by tairan.hao for 10691772 on 2021-02-04
    AppUnderControll att = new AppUnderControll(attApps) {
        @Override
        public boolean isSimAppropriate() {
            if (iccid0 != null && iccid0.length() >= 9) {
                String sub = iccid0.substring(8, 9);
                Log.d(TAG, "iccid0[8]= " + sub);
                //[TCT-ROM]Begin add by tairan.hao for 10658538 on 2021-01-18
                if ("0".equals(sub) || "2".equals(sub) ||
                        "4".equals(sub) || "5".equals(sub) ||
                        "6".equals(sub) || "7".equals(sub) ||
                        "8".equals(sub) || "9".equals(sub)) {
                    //[TCT-ROM]End add by tairan.hao for 10658538 on 2021-01-18
                    if (isSubInfoAppropriate(attSIMPlmn, plmn0)) {
                        return true;
                    }
                }
            }
            if (iccid1 != null && iccid1.length() >= 9) {
                String sub = iccid1.substring(8, 9);
                Log.d(TAG, "iccid1[8] =" + sub);
                //[TCT-ROM]Begin add by tairan.hao for 10658538 on 2021-01-18
                if ("0".equals(sub) || "2".equals(sub) ||
                        "4".equals(sub) || "5".equals(sub) ||
                        "6".equals(sub) || "7".equals(sub) ||
                        "8".equals(sub) || "9".equals(sub)) {
                    //[TCT-ROM]End add by tairan.hao for 10658538 on 2021-01-18
                    if (isSubInfoAppropriate(attSIMPlmn, plmn1)) {
                        return true;
                    }
                }
            }
            return false;
        }


    };

    /* =============================================================================================================  */
    /**
     * UNEFON
     */
    //UNEFON defect 8910971 8743303
    private final String[] UNEFON_APPS = {"com.att.miunefonmx"};
    private final String[] unefonSIMPlmn = {"33405", "334050"};
    AppUnderControll unefon = new AppUnderControll(UNEFON_APPS) {
        @Override
        public boolean isSimAppropriate() {
            if (iccid0 != null && iccid0.length() >= 9) {
                Log.d(TAG, "iccid0[8]= " + iccid0.substring(8, 9));
                if ("3".equals(iccid0.substring(8, 9)) || "1".equals(iccid0.substring(8, 9))) {
                    if (isSubInfoAppropriate(attSIMPlmn, plmn0)) {
                        return true;
                    }
                }
            }
            if (iccid1 != null && iccid1.length() >= 9) {
                Log.d(TAG, "iccid1[8] =" + iccid1.substring(8, 9));
                if ("3".equals(iccid1.substring(8, 9)) || "1".equals(iccid1.substring(8, 9))) {
                    if (isSubInfoAppropriate(attSIMPlmn, plmn1)) {
                        return true;
                    }
                }
            }
            return false;

        }
    };
    /* =============================================================================================================  */


    /**
     * TWO DEGREES
     */
    //TWO_DEGREES Task 9463498
    private final String[] TWO_DEGREES_APPS = {"com.twodegreesmobile.twodegrees"};
    private final String[] twoDegreesSIMPlmn = {"53024"};
    AppUnderControll twoDegrees = new AppUnderControll(TWO_DEGREES_APPS) {
        @Override
        public void setNewState() {
            //if not first boot, don't disable 2Degrees apps
            if (isFirstBoot() && !isSimAppropriate()) {
                twoDegrees.newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            }
            if (isSimAppropriate()) {
                twoDegrees.newState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            }
        }

        @Override
        public boolean isSimAppropriate() {
            return "2degrees".equals(spn0) && isSubInfoAppropriate(twoDegreesSIMPlmn, plmn0) ||
                    "2degrees".equals(spn1) && isSubInfoAppropriate(twoDegreesSIMPlmn, plmn1);
        }
    };

    /* =============================================================================================================  */


    /**
     * DT telekom
     */
    private final String[] DT_APPS = {"de.telekom.tsc"};
    private final String[] dtSIMPlmn = {"20416", "21630", "21901", "21920", "23203", "23207", "26201", "26206"};
    private final String[] dtSIMGidPlmn = {
            "20416FFFF", "204168FFF", "204164E4C",
            "2190101FF", "2190102FF", "2190103FF", "2190199FF", "21901FFFF", "2190110FF", "2190111FF", "2190112FF",
            "2192001FF", "2192002FF", "2192003FF", "2192099FF", "21920FFFF", "2192010FF", "2192011FF", "2192012FF",
            "2320301FF", "2320331FF", "2320332FF",
            "2320707FF", "2320730FF", "23207FFFF",
            "2620101FF", "2620102FF", "2620103FF", "2620104FF", "2620199FF", "262014BFF", "2620144FF",
            "2620601FF", "2620602FF", "2620603FF", "2620604FF", "2620699FF", "262064BFF", "2620644FF"};
    AppUnderControll dt = new AppUnderControll(DT_APPS) {
        @Override
        public void setNewState() {
            if (isFirstBoot() && !isSimAppropriate()) {
                dt.newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            }
            if (isSimAppropriate()) {
                dt.newState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            }
        }

        @Override
        public boolean isSimAppropriate() {
//            CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
//            SubscriptionManager subscriptionManager = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//            boolean isDtConfigEnable = false;
//            int[] list = subscriptionManager.getActiveSubscriptionIdList();
//            if (list != null && list.length > 0) {
//                try {
//                    isDtConfigEnable = configManager.getConfigForSubId(list[0]).getBoolean("preinstall_app_enabler");
//                } catch (Exception e) {
//                    Log.e(TAG, "dt config load error " + e.getMessage());
//                }
//
//            }
//            Log.e(TAG, "dt config enable = " + isDtConfigEnable);
//            return isDtConfigEnable;
            if (plmn0 != null && plmn0.equals("21630")) {
                if (spn0.equals("Telekom HU")) {
                    return true;
                }
                if (spn0.equals("T-Mobile H")) {
                    return true;
                }
            }
            if (isSubInfoAppropriate(dtSIMPlmn, plmn0)) {
                TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(TelephonyManager.class);
                String subGid1 = "";
                String gid1 = telephonyManager.getGroupIdLevel1();
                if (gid1.length() > 2) {
                    subGid1 = gid1.substring(0, 4);
                    String mccMncGid = plmn0 + subGid1;
                    if (isSubInfoAppropriate(dtSIMGidPlmn, mccMncGid)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void updateAppState() {
            IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            for (String pkg : dt.pkgs) {
                updateInstallState(pkg, isSimAppropriate(), mIPm);
            }
        }
    };

    /* =============================================================================================================  */

    private void updateAppState() {
        if (mHandler == null) return;
        getSubscriptionInfo(mContext);
        mHandler.post(() ->
                //updateAppState(orange,att,unefon,twoDegrees)
                updateAppState(orange, dt)
        );
    }

    private void updateAppState(AppUnderControll... appUnderControll) {
        int count = 1;
        for (AppUnderControll apps : appUnderControll) {
            Log.d(TAG, "---Now update Group [" + count++ + "] ---");
            apps.setEnableFlag();
            apps.setNewState();
            apps.updateAppState();
        }
    }

    private boolean isSubInfoAppropriate(String[] targetSim, String currentSim) {
        if (targetSim == null || targetSim.length == 0 || currentSim == null) return false;
        for (String sim : targetSim) {
            if (sim.equals(currentSim)) return true;
        }
        return false;
    }

    private boolean isFirstBoot() {
        try {
            IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            return mIPm.isFirstBoot();
        } catch (RemoteException e) {
            Log.e(TAG, "FirstBoot? no", e);
            return false;
        }
    }

    private void getSubscriptionInfo(Context context) {
        spn0 = "";
        plmn0 = "";
        iccid0 = "";
        spn1 = "";
        plmn1 = "";
        iccid1 = "";
        gid1_0 = "";
        gid1_1 = "";
        if (context == null) return;
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) {
            Log.i(TAG, "subscriptionManager == null!!!");
            return;
        }
        final List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList(true);

        TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();
            Log.d(TAG, "subInfoLength = " + subInfoLength);
            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (mTelephonyManager != null && sir != null && isSubIDValid(sir.getSubscriptionId())) {
                    simCount += 1;
                    int subid = sir.getSubscriptionId();
                    if (i == 0) {
                        spn0 = mTelephonyManager.getSimOperatorName(subid);
                        plmn0 = mTelephonyManager.getSimOperatorNumeric(subid);
                        iccid0 = mTelephonyManager.getSimSerialNumber(subid);
                        gid1_0 = mTelephonyManager.getGroupIdLevel1(subid);
                        Log.d(TAG, "spn0= " + spn0 + " plmn0 = " + plmn0 + " iccid0= " + iccid0 + " gid1 = " + gid1_0);
                    } else if (i == 1) {
                        spn1 = mTelephonyManager.getSimOperatorName(subid);
                        plmn1 = mTelephonyManager.getSimOperatorNumeric(subid);
                        iccid1 = mTelephonyManager.getSimSerialNumber(subid);
                        Log.d(TAG, "spn1= " + spn1 + " plmn1= " + plmn1 + " iccid1= " + iccid1);
                    } else {
                        Log.i(TAG, "so many sim cards!!!");
                    }
                }
            }
        } else {
            Log.i(TAG, "subInfoList == null!!!");
        }


        if (getSimLockState() == 1) {
            if (simCount > 0) {
                MyCarrierInfo info = new MyCarrierInfo();
                info.setMccMnc(plmn0);
                info.setSpn(spn0);
                info.setGid(gid1_0);
                saveLockCarrierInfo(info);
            } else {
                spn0 = getLockSpn();
                plmn0 = getLockMccMnc();
                gid1_0 = getLockGid();
            }
        }
    }

    private boolean hasSimStateChanged = false;

    private void registerReceiver() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        //mIntentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mIntentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(intent.getAction())) {
                    hasSimStateChanged = true;
                    int simStatus = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE, -99);
                    Log.d(TAG, "ACTION_SIM_APPLICATION_STATE_CHANGED" + " simStatus= " + simStatus);
                    updateAppState();
                }
                //if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !hasSimStateChanged){
                if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()) && !hasSimStateChanged) {
                    Log.d(TAG, "LOCKED_BOOT_COMPLETED");
                    updateAppState();
                }
            }
        }, mIntentFilter);
    }

    private boolean isAppInstalled(String pkg) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(pkg, 0);
            if (appInfo != null) {
                Log.d(TAG, pkg + " is installed");
                return true;
            } else {
                Log.d(TAG, pkg + " is not installed");
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, pkg + " is not installed,Exception happed.");
            return false;
        }
    }

    private boolean isSubIDValid(int subid) {
        int slotID = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        if (subid <= SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        slotID = SubscriptionManager.getSlotIndex(subid);
        if (slotID == PhoneConstants.SIM_ID_1 || slotID == PhoneConstants.SIM_ID_2) {
            return true;
        }
        return false;
    }

    private void updateInstallState(String pkg, boolean isSimAppropriate, IPackageManager ipm) {
        Log.d(TAG, pkg + " updateInstallState sim Appropriate  " + isSimAppropriate);
        try {
            ipm.setSystemAppInstallState(pkg, isSimAppropriate, mContext.getUserId());
        } catch (Exception e) {
            Log.d(TAG, pkg + " updateInstallState error " + e.getMessage());
        }

    }

    class AppUnderControll {
        private String[] pkgs;
        private int newState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;// 0
        private int enableFlag = PackageManager.DONT_KILL_APP;

        public AppUnderControll(String[] pkgs) {
            this.pkgs = pkgs;
        }

        public void setNewState() {
            this.newState = isSimAppropriate() ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            Log.d(TAG, "newState= " + this.newState);
        }

        public void setEnableFlag() {

        }

        public boolean isSimAppropriate() {
            Log.i(TAG, "override isSimAppropriate()!!!");
            return true;
        }

        public void updateAppState() {
            IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            for (String pkg : pkgs) {
                try {
                    if (!isAppInstalled(pkg) || newState == mIPm.getApplicationEnabledSetting(pkg, USER_SYSTEM))
                        continue;
                    mIPm.setApplicationEnabledSetting(pkg,
                            newState, enableFlag, USER_SYSTEM, "System");
                    Log.d(TAG, "set " + pkg + " enable state as " + newState);
                } catch (Exception e) {
                    Log.i(TAG, "cannot set " + pkg + " as " + newState);
                }
            }
        }

    }


    private int getSimLockState() {
        int lockState = SystemProperties.getInt("persist.radio.dsd.locked", 0);
        Log.d(TAG, " getSimLockState >> " + lockState);
        return lockState;
    }

    private String getLockGid() {
        String lockMccGid = Settings.Global.getString(mContext.getContentResolver(), SETTINGS_CONFIG_SIM_LOCK_GID);
        return lockMccGid;
    }

    private String getLockSpn() {
        String lockMccGid = Settings.Global.getString(mContext.getContentResolver(), SETTINGS_CONFIG_SIM_LOCK_SPN);
        return lockMccGid;
    }

    private String getLockMccMnc() {
        String mccMnc = Settings.Global.getString(mContext.getContentResolver(), SETTINGS_CONFIG_SIM_LOCK_MCCMNC);
        return mccMnc;
    }

    private void saveLockCarrierInfo(MyCarrierInfo info) {
        Settings.Global.putString(mContext.getContentResolver(), SETTINGS_CONFIG_SIM_LOCK_GID, info.getGid());
        Settings.Global.putString(mContext.getContentResolver(), SETTINGS_CONFIG_SIM_LOCK_MCCMNC, info.getMccMnc());
        Settings.Global.putString(mContext.getContentResolver(), SETTINGS_CONFIG_SIM_LOCK_SPN, info.getSpn());
    }

    private class MyCarrierInfo {
        private String mccMnc;
        private String gid;
        private String spn;

        public void setMccMnc(String mccMnc) {
            this.mccMnc = mccMnc;
        }

        public void setGid(String gid) {
            this.gid = gid;
        }

        public void setSpn(String spn) {
            this.spn = spn;
        }

        public String getMccMnc() {
            return mccMnc;
        }

        public String getGid() {
            return gid;
        }

        public String getSpn() {
            return spn;
        }
    }


}