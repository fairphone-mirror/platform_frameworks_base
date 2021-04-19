package com.android.server;

import android.content.Intent;
import android.hardware.health.V1_0.HealthInfo;
import android.os.BatteryManager;
import android.os.UserHandle;
import android.util.Log;
import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <pre>
 *     author : zhangtianwen
 *     e-mail : tianwen.zhang@t2mobile.com
 *     time   : 2021/04/16
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class Fp4BatteryFuncImpl implements ICustomerBatteryFunc {

    private static final String TAG = Fp4BatteryFuncImpl.class.getSimpleName();

    private int mLastBatteryHealth = 0;

    @Override
    public void notifyBatteryTempWarnChanged(Context context, CustomBatteryInfo info) {
        String usbPresent = getUsbPresent();
        //if (usbPresent.equals("1")) {
            HealthInfo currentHealthInfo = info.getHealthInfo();
            int currentBatteryHealth = currentHealthInfo.batteryHealth;
            if (mLastBatteryHealth != currentBatteryHealth) {
                Intent intent = new Intent(Intent.ACTION_BATTERY_WARM_TEMP_CHANGED);
                Log.i(TAG, "send ACTION_BATTERY_WARM_TEMP_CHANGED CustomBatteryInfo = " + info);
                intent.putExtra(Intent.EXTRA_BATTERY_HEALTH, currentBatteryHealth);
                context.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
            mLastBatteryHealth = currentHealthInfo.batteryHealth;
        //}


    }

    private String getUsbPresent() {
        String version = null;
        try {
            InputStream is = new FileInputStream("/sys/class/power_supply/usb/present");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            version = reader.readLine();
            reader.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getVersion fail" + e);
        }
        return version;
    }

}
