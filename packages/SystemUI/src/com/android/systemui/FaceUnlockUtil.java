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
import java.util.List;
import java.util.ArrayList;

import com.android.keyguard.KeyguardUpdateMonitor;

public class FaceUnlockUtil {
    private static final String TAG = "FaceUnlockUtil";
    private static FaceUnlockUtil mInstance;
    private static final String MAIN_FACE_ID_SETTING_KEY = "enroll_main_face_id";
    private static final String SECOND_FACE_ID_SETTING_KEY = "enroll_second_face_id";
    private static final String COUNT_DOWN_TIME_UNLOCK = "count_down_time_unlock";
    private int mFailTimes = 0;
    private List<FaceUnlockCallback> mCallbackList = new ArrayList<>();
    private boolean mIsInUnlocking = false;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mStartLockdown = false;

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

    public void setKeyguardUpdateMonitor(KeyguardUpdateMonitor keyguardUpdateMonitor) {
        this.mKeyguardUpdateMonitor = keyguardUpdateMonitor;
    }

    public void startLockdown() {
        this.mStartLockdown = true;
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
            mIsInUnlocking = true;
            if(!mCallbackList.isEmpty()){
                for(FaceUnlockCallback callback : mCallbackList){
                    if(callback != null){
                        callback.onStartFaceUnlock();
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void stopFaceUnlock(Context context){
        if(!isFaceUnlockEnable(context)){
            return;
        }
        try{
            Intent faceIntent = new Intent()
                    .setComponent(new ComponentName("com.android.settings","com.android.settings.anc.unlock.UnlockActivity"))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            faceIntent.putExtra("stop_unlock", true);
            context.startActivityAsUser(faceIntent, UserHandle.CURRENT);
            mIsInUnlocking = false;
        } catch(Exception e){
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
        boolean isLockdown = false;
        boolean isGmsActiveAdmin = false;
        if(mKeyguardUpdateMonitor != null){
            isLockdown = mKeyguardUpdateMonitor.isEncryptedOrLockdown(userId);
            isGmsActiveAdmin = mKeyguardUpdateMonitor.isGmsAdminActive();
        }
        if(isLockdown) {
            mStartLockdown = false;
        }
        Log.d(TAG,"isGmsActiveAdmin:"+isGmsActiveAdmin);
        return !isRebootView && faceUnlockSupported && hasFaceEnrolled && !isCounDown
                    && mFailTimes < 3 && !isLockdown && !mStartLockdown && !isGmsActiveAdmin ;
    }

    public boolean hasFaceUnlock(Context context){
        int userId = KeyguardUpdateMonitor.getCurrentUser();
        boolean faceUnlockSupported = isFaceUnlockSupported(context);
        boolean hasFaceEnrolled = hasFaceEnrolled(context);
        boolean isRebootView = isRebootView(context);
        boolean isLockdown = false;
        if(mKeyguardUpdateMonitor != null){
            isLockdown = mKeyguardUpdateMonitor.isEncryptedOrLockdown(userId);
        }
        if(isLockdown) {
            mStartLockdown = false;
        }
        return !isRebootView && faceUnlockSupported && hasFaceEnrolled && !isLockdown && !mStartLockdown;
    }

    public void setFailTimes(int failTimes,boolean isFinishKeyguard){
        this.mFailTimes = failTimes;
        mIsInUnlocking = false;
        if(!isFinishKeyguard && !mCallbackList.isEmpty()){
            for(FaceUnlockCallback callback : mCallbackList){
                if(callback != null){
                    callback.onFaceAuthResult(failTimes);
                }
            }
        }
    }

    public int getFailTimes(){
        return this.mFailTimes;
    }

    public void addCallback(FaceUnlockCallback callback) {
        if(callback == null || mCallbackList.contains(callback)){
            return;
        }
        mCallbackList.add(callback);
    }

    public boolean isInUnlocking(){
        return this.mIsInUnlocking;
    }

    public void removeCallback(FaceUnlockCallback callback) {
        if(callback == null || !mCallbackList.contains(callback)){
            return;
        }
        mCallbackList.remove(callback);
    }

    public interface FaceUnlockCallback{
        void onFaceAuthResult(int failTimes);

        void onStartFaceUnlock();
    }
}
