package com.android.systemui;


import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.UserHandle;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class FaceUnlockUtil {
    private static FaceUnlockUtil mInstance;

    private FaceUnlockUtil(){

    }

    public static FaceUnlockUtil getInstance() {
        if(mInstance == null) {
            mInstance = new FaceUnlockUtil();
        }
        return mInstance;
    }

    public void startFaceUnlock(Context context){
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
}
