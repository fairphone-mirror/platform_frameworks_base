package com.android.server;

import android.content.Context;
import android.hardware.health.V1_0.HealthInfo;

/**
 * <pre>
 *     author : zhangtianwen
 *     e-mail : tianwen.zhang@t2mobile.com
 *     time   : 2021/04/16
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public interface ICustomerBatteryFunc {

    void notifyBatteryTempWarnChanged(Context context, CustomBatteryInfo info);

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
