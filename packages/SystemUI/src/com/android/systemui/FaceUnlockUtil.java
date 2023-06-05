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
    private FaceUnlockCallback mCallback;

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

    private boolean isRebootView(Context context){
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

    private boolean hasFaceEnrolled(Context context) {
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

    private boolean getCountDownUnlock(Context context){
        return getIntSettingValue(context, COUNT_DOWN_TIME_UNLOCK,0) > 0;
    }

    public void startFaceUnlock(Context context){
        if(getCountDownUnlock(context)){
            return;
        }
        try{
            Intent faceIntent = new Intent()
                .setComponent(new ComponentName("com.android.settings","com.android.settings.anc.unlock.UnlockActivity"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivityAsUser(faceIntent, UserHandle.CURRENT);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    

    private boolean isFaceUnlockSupported(Context context){
        return true;
    }

    public boolean isFaceUnlockEnable(Context context){
        int userId = KeyguardUpdateMonitor.getCurrentUser();
        boolean faceUnlockSupported = isFaceUnlockSupported(context);
        boolean hasFaceEnrolled = hasFaceEnrolled(context);
        boolean isCounDown = getCountDownUnlock(context);
        boolean isRebootView = isRebootView(context);
        Log.d(TAG, "isFaceUnlockEnable faceUnlockSupported: " + faceUnlockSupported +
                ",hasFaceEnrolled:" + hasFaceEnrolled + ",isCounDown:" + isCounDown +
                ",isRebootView:" +isRebootView+",mFailTimes:"+mFailTimes);
        return !isRebootView && faceUnlockSupported && hasFaceEnrolled && !isCounDown && mFailTimes < 3;
    }

    public void setFailTimes(int failTimes,boolean isFinishKeyguard){
        this.mFailTimes = failTimes;
        if(!isFinishKeyguard && mCallback != null){
            mCallback.onFaceAuthResult(failTimes);
        }
    }

    public int getFailTimes(){
        return this.mFailTimes;
    }

    public void addCallback(FaceUnlockCallback callback) {
        this.mCallback = callback;
    }

    public void removeCallback() {
        this.mCallback = null;
    }

    public interface FaceUnlockCallback{
        void onFaceAuthResult(int failTimes);
    }
}
