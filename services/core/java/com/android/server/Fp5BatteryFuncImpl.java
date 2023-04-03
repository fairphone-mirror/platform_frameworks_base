package com.android.server;

import android.content.Intent;
import android.hardware.health.HealthInfo;
import android.os.BatteryManager;
import android.os.UserHandle;
import android.util.Log;
import android.content.Context;
import java.io.File;
import java.io.FileDescriptor;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import android.util.Slog;

/**
 * <pre>
 *     author : zhangtianwen
 *     e-mail : tianwen.zhang@t2mobile.com
 *     time   : 2021/04/16
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class Fp5BatteryFuncImpl implements ICustomerBatteryFunc {

    private static final String TAG = Fp5BatteryFuncImpl.class.getSimpleName();

    private int mLastBatteryHealth = 0;
    private int low_temp_20 = -200;
    private int low_temp_5 = 50;
    private int high_temp_55 = 550;
    private int high_temp_60 = 600;
    private int lastTemperatureState = 0;
    private int currentTemperatureState = 999;

    @Override
    public void notifyBatteryTempWarnChanged(Context context, CustomBatteryInfo info) {
        String usbPresent = getUsbPresent();
        //if (usbPresent.equals("1")) {
            HealthInfo currentHealthInfo = info.getHealthInfo();
            int currentBatteryHealth = currentHealthInfo.batteryHealth;
            int currentTemperature = currentHealthInfo.batteryTemperatureTenthsCelsius;
            int batteryStatus = currentHealthInfo.batteryStatus ;
            if (currentTemperature >= 600) {
                currentTemperatureState = high_temp_60;
            } else if (currentTemperature >= 550 && currentTemperature < 600){
                currentTemperatureState = high_temp_55;
            } else if (currentTemperature >= -200 && currentTemperature < 50){
                currentTemperatureState = low_temp_5;
            } else if (currentTemperature <= -200){
                currentTemperatureState = low_temp_20;
            } else {
                currentTemperatureState = 999;
            }
            if (currentTemperatureState != lastTemperatureState) {
                // if (mLastBatteryHealth != currentBatteryHealth) {
                Intent intent = new Intent(Intent.ACTION_BATTERY_WARM_TEMP_CHANGED);
                Log.i(TAG, "send ACTION_BATTERY_WARM_TEMP_CHANGED CustomBatteryInfo = " + info);
                intent.putExtra(Intent.EXTRA_BATTERY_HEALTH, currentBatteryHealth);
                intent.putExtra(BatteryManager.EXTRA_TEMPERATURE, currentTemperature);
                intent.putExtra(BatteryManager.EXTRA_STATUS, batteryStatus);
                context.sendBroadcastAsUser(intent, UserHandle.ALL);
            // }
                // mLastBatteryHealth = currentHealthInfo.batteryHealth;
                lastTemperatureState = currentTemperatureState;
            }
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
