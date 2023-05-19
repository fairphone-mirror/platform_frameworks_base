package com.android.systemui;


import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings;

import com.android.keyguard.KeyguardUpdateMonitor;

public class FaceUnlockUtil {
    private static final String TAG = "FaceUnlockUtil";
    private static FaceUnlockUtil mInstance;
    private static final String MAIN_FACE_ID_SETTING_KEY = "enroll_main_face_id";
    private static final String SECOND_FACE_ID_SETTING_KEY = "enroll_second_face_id";
    private static final String COUNT_DOWN_TIME_UNLOCK = "count_down_time_unlock";
    private int mFailTimes = 0;

    private FaceUnlockUtil(){

    }

    public static FaceUnlockUtil getInstance() {
        if(mInstance == null) {
            synchronized (FaceUnlockUtil.class) {
                if(mInstance == null){
                    mInstance = new FaceUnlockUtil();
                }
            }
        }
        return mInstance;
    }

    public boolean isRebootView(Context context){
        if(isFaceUnlockSupported(context)) {
            int userId = KeyguardUpdateMonitor.getCurrentUser();
            UserManager userManager = context.getSystemService(UserManager.class);
            return !userManager.isUserUnlocked(userId);

        }
        return false;
    }

    private int getIntSettingValue(Context context, String key, int defaultValue) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                key, defaultValue,
                UserHandle.USER_CURRENT);
    }

    public boolean hasFaceEnrolled(Context context) {
        if(isFaceUnlockSupported(context)) {
            int mainFaceId = getIntSettingValue(context, MAIN_FACE_ID_SETTING_KEY, 0);
            int secondFaceId = getIntSettingValue(context, SECOND_FACE_ID_SETTING_KEY, 0);
            return mainFaceId > 0 || secondFaceId > 0;
        }
        return false;
    }

    public void setCountDownUnlock(Context context, int coutDown){
        Log.d(TAG, "setCountDownUnlock: " + coutDown);
        Settings.System.putIntForUser(context.getContentResolver(),
                COUNT_DOWN_TIME_UNLOCK, coutDown,
                UserHandle.USER_CURRENT);
    }

    public boolean getCountDownUnlock(Context context){
        return getIntSettingValue(context, COUNT_DOWN_TIME_UNLOCK,0) > 0;
    }

    public void startFaceUnlock(Context context){
        if(getCountDownUnlock(context)){
            return;
        }
        try{
            Intent faceIntent = new Intent()
                .setComponent(new ComponentName("com.fp.faceunlock","com.fp.faceunlock.anc.unlock.UnlockActivity"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivityAsUser(faceIntent, UserHandle.CURRENT);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    

    public boolean isFaceUnlockSupported(Context context){
        PackageManager packageManager =  context.getPackageManager();
        try{
            packageManager.getPackageInfo("com.fp.faceunlock",PackageManager.GET_ACTIVITIES);
            return true;
        }catch(NameNotFoundException e){
            return false;
        }
    }

    public void setFailTimes(int failTimes){
        this.mFailTimes = failTimes;
    }

    public int getFailTimes(){
        return this.mFailTimes;
    }
}
