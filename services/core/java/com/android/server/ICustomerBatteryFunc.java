package com.android.server;

import android.content.Context;
import android.hardware.health.HealthInfo;

/**
 * <pre>
 *     author : zhangtianwen
 *     e-mail : tianwen.zhang@t2mobile.com
 *     time   : 2021/04/16
 *     desc   : Customer function interface.
 *     version: 1.0
 * </pre>
 */
public interface ICustomerBatteryFunc {

    void notifyBatteryTempWarnChanged(Context context, CustomBatteryInfo info);

    /**
     * Customer funcation paramas.
     * maybe batteryinfo,maybe ourself's driver point
     */
    class CustomBatteryInfo {

        private HealthInfo mHealthInfo;

        public CustomBatteryInfo setHeathInfo(HealthInfo healthInfo) {
            mHealthInfo = healthInfo;
            return this;
        }

        public HealthInfo getHealthInfo() {
            return mHealthInfo;
        }

        @Override
        public String toString() {
            return "CustomBatteryInfo{" +
                    "mHealthInfo=" + mHealthInfo +
                    '}';
        }
    }

}
