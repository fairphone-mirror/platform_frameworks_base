/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.systemui.statusbar.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.collect.Lists;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.Exception;
import java.util.ArrayList;
import java.lang.ref.WeakReference;

import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.mobile.TelephonyIcons;
import com.android.settingslib.SignalIcon.MobileIconGroup;
import com.android.systemui.R;

import com.qti.extphone.Client;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.ExtPhoneCallbackBase;
import com.qti.extphone.NrIconType;
import com.qti.extphone.Status;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Token;
//add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221026 begin
import com.android.ims.ImsManager;
import android.telephony.ServiceState;
import android.telephony.CellInfo;
import android.telephony.CellInfoNr;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import java.util.List;
//add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221026 end

public class FiveGServiceClient {
    private static final String TAG = "FiveGServiceClient";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG)||true;
    private static final int MESSAGE_REBIND = 1024;
    private static final int MESSAGE_REINIT = MESSAGE_REBIND+1;
    private static final int MESSAGE_NOTIFIY_MONITOR_CALLBACK = MESSAGE_REBIND+2;
    private static final int MAX_RETRY = 4;
    private static final int DELAY_MILLISECOND = 3000;
    private static final int DELAY_INCREMENT = 2000;

    private static FiveGServiceClient sInstance;
    private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>>
            mKeyguardUpdateMonitorCallbacks = Lists.newArrayList();
    @VisibleForTesting
    final SparseArray<IFiveGStateListener> mStatesListeners = new SparseArray<>();
    private final SparseArray<FiveGServiceState> mCurrentServiceStates = new SparseArray<>();
    private final SparseArray<FiveGServiceState> mLastServiceStates = new SparseArray<>();

    private Context mContext;
    private boolean mServiceConnected;
    private String mPackageName;
    private Client mClient;
    private int mInitRetryTimes = 0;
    private ExtTelephonyManager mExtTelephonyManager;
    private boolean mIsConnectInProgress = false;

    //add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221024 begin
    private TelephonyManager tm;
    private SubscriptionManager subMgr;
    private PhoneStateListener[] mPhoneStateListener = null;
    private static int mRegState = -1;
    private boolean isNSA = false;
    private int newNrIconType = -1;

    /**
    *register PhoneStateLister ,for opertor FET when UE register on NSA and call_mode_pref is not wifi_only
    * during volte call icon changed from 5G to 4G ,and after call ended, icon change to 5G icon again.
    **/
    private void registerPhoneStateListeners(int phoneId) {
        if (tm == null || subMgr == null ) {
            Log.e(TAG, "TelephonyManager or SubscriptionManager is null");
            return;
        }
        final SubscriptionInfo subInfo =
                      subMgr.getActiveSubscriptionInfoForSimSlotIndex(phoneId);
        if (subInfo == null) {
            Log.e(TAG, "registerPhoneStateListener subInfo : " + subInfo + " for phone Id: " + phoneId);
            return;
        }
        ImsManager imsManager = ImsManager.getInstance(mContext, phoneId);
        if (imsManager == null ) return ;
        int subId = subInfo.getSubscriptionId();
        tm = tm.createForSubscriptionId(subId);

        mPhoneStateListener[phoneId]  = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                boolean isVolteCall = false;
                int wfcMode = imsManager.getWfcMode();//call mode pref
                String mccmnc = subInfo.getMccString()+subInfo.getMncString();
                isVolteCall = (state != TelephonyManager.CALL_STATE_IDLE) && tm.isVolteAvailable() && !tm.isWifiCallingAvailable() && (wfcMode != 0);//if during volte call
                Log.d(TAG, "newNrIconType: = " + newNrIconType + ", isVolteCall =  " + isVolteCall + ", state = " + state + " , isVolteEnabled = " + tm.isVolteAvailable()
                    + " , isWifiCallingEnabled = " + tm.isWifiCallingAvailable() + ", isNSA = " + isNSA + ", wfcMode = " + wfcMode +" mccmnc = " + mccmnc);

                FiveGServiceState states = getCurrentServiceState(phoneId);
                if (isVolteCall  && (newNrIconType == 1) && isNSA && "46601".equals(mccmnc)) { // during volte call under NSA for FET
                    states.mNrIconType = 0;
                } else {
                    states.mNrIconType = newNrIconType;
                }
                update5GIcon(states, phoneId);
                notifyListenersIfNecessary(phoneId);
                Log.d(TAG, " states.mNrIconType =  " + states.mNrIconType);
            }
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                mRegState = serviceState.getState();
            }

            @Override
            public void onCellInfoChanged(List<CellInfo> cellInfo) {
                if(cellInfo != null && (mRegState == ServiceState.STATE_IN_SERVICE)) {
                    for (CellInfo cell : cellInfo) {
                        if(cell instanceof CellInfoNr) {
                            CellInfoNr nr = (CellInfoNr) cell;
                            if(nr.isRegistered()) {
                               isNSA = false;//SA registered
                            } else {
                               isNSA = true;//NSA registered
                            }
                            Log.d(TAG, "NR Cell Register state: " + nr.isRegistered() + ", isNSA = " + isNSA);
                        }
                    }
                }
            }
        };
        tm.listen(mPhoneStateListener[phoneId], PhoneStateListener.LISTEN_CALL_STATE|PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_CELL_INFO);
    }

    private void unRegisterPhoneStateListeners(int phoneId) {
        if (tm == null || subMgr == null ) {
            Log.e(TAG, "TelephonyManager or SubscriptionManager is null");
            return;
        }
        if (mPhoneStateListener[phoneId] != null) {
            final SubscriptionInfo subInfo =
                        subMgr.getActiveSubscriptionInfoForSimSlotIndex(phoneId);
            if (subInfo == null) {
                Log.e(TAG, "registerPhoneStateListener subInfo : " + subInfo +" for phone Id: " + phoneId);
                return;
            }
            tm = tm.createForSubscriptionId(subInfo.getSubscriptionId());
            tm.listen(mPhoneStateListener[phoneId], PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener[phoneId] = null;
        }
    }
    //add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221024 end

    public static class FiveGServiceState{
        private int mNrIconType;
        private MobileIconGroup mIconGroup;

        public FiveGServiceState(){
            mNrIconType = NrIconType.INVALID;
            mIconGroup = TelephonyIcons.UNKNOWN;
        }

        public boolean isNrIconTypeValid() {
            return mNrIconType != NrIconType.INVALID && mNrIconType != NrIconType.TYPE_NONE;
        }

        @VisibleForTesting
        public MobileIconGroup getIconGroup() {
            return mIconGroup;
        }

        @VisibleForTesting
        int getNrIconType() {
            return mNrIconType;
        }

        public void copyFrom(FiveGServiceState state) {
            this.mIconGroup = state.mIconGroup;
            this.mNrIconType = state.mNrIconType;
        }

        public boolean equals(FiveGServiceState state) {
            return this.mIconGroup == state.mIconGroup
                    && this.mNrIconType == state.mNrIconType;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("mNrIconType=").append(mNrIconType).append(", ").
                    append("mIconGroup=").append(mIconGroup);

            return builder.toString();
        }
    }

    public FiveGServiceClient(Context context) {
        mContext = context;
        mPackageName = mContext.getPackageName();
        if (mExtTelephonyManager == null) {
            mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        }
        //add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221024 begin
        tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        subMgr = (SubscriptionManager) mContext.getSystemService(
                  Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mPhoneStateListener = new PhoneStateListener[tm.getPhoneCount()];
        //add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221024 end

    }

    public static FiveGServiceClient getInstance(Context context) {
        if ( sInstance == null ) {
            sInstance = new FiveGServiceClient(context);
        }

        return sInstance;
    }

    public void registerCallback(KeyguardUpdateMonitorCallback callback) {
        mKeyguardUpdateMonitorCallbacks.add(
                new WeakReference<KeyguardUpdateMonitorCallback>(callback));
    }

    public void registerListener(int phoneId, IFiveGStateListener listener) {
        Log.d(TAG, "registerListener phoneId=" + phoneId);
        resetState(phoneId);
        mStatesListeners.put(phoneId, listener);
        if ( !isServiceConnected() ) {
            connectService();
        }else{
            initFiveGServiceState(phoneId);
        }
        registerPhoneStateListeners(phoneId);// add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221026
    }

    private void resetState(int phoneId) {
        Log.d(TAG, "resetState phoneId=" + phoneId);
        FiveGServiceState currentState = getCurrentServiceState(phoneId);
        currentState.mNrIconType = NrIconType.INVALID;
        currentState.mIconGroup = TelephonyIcons.UNKNOWN;

        FiveGServiceState lastState = getLastServiceState(phoneId);
        lastState.mNrIconType = NrIconType.INVALID;
        lastState.mIconGroup = TelephonyIcons.UNKNOWN;
    }

    public void unregisterListener(int phoneId) {
        Log.d(TAG, "unregisterListener phoneId=" + phoneId);
        mStatesListeners.remove(phoneId);
        mCurrentServiceStates.remove(phoneId);
        mLastServiceStates.remove(phoneId);
        unRegisterPhoneStateListeners(phoneId); // add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221026
    }

    public boolean isServiceConnected() {
        return mServiceConnected;
    }

    private void connectService() {
        if (!isServiceConnected() && !mIsConnectInProgress) {
            mIsConnectInProgress = true;
            Log.d(TAG, "Connect to ExtTelephony bound service...");
            mExtTelephonyManager.connectService(mServiceCallback);
        }
    }

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephony Service connected");
            mServiceConnected = true;
            mIsConnectInProgress = false;
            mClient = mExtTelephonyManager.registerCallback(mPackageName, mCallback);
            initFiveGServiceState();
            Log.d(TAG, "Client = " + mClient);
        }
        @Override
        public void onDisconnected() {
            Log.d(TAG, "ExtTelephony Service disconnected...");
            if (mServiceConnected) {
                mExtTelephonyManager.unRegisterCallback(mCallback);
            }
            mServiceConnected = false;
            mClient = null;
            mIsConnectInProgress = false;
            mHandler.sendEmptyMessageDelayed(MESSAGE_REBIND,
                    DELAY_MILLISECOND + DELAY_INCREMENT);
        }
    };

    @VisibleForTesting
    public FiveGServiceState getCurrentServiceState(int phoneId) {
        return getServiceState(phoneId, mCurrentServiceStates);
    }

    private FiveGServiceState getLastServiceState(int phoneId) {
        return getServiceState(phoneId, mLastServiceStates);
    }

    private static FiveGServiceState getServiceState(int key,
                                                     SparseArray<FiveGServiceState> array) {
        FiveGServiceState state = array.get(key);
        if ( state == null ) {
            state = new FiveGServiceState();
            array.put(key, state);
        }
        return state;
    }

    private void notifyListenersIfNecessary(int phoneId) {
        FiveGServiceState currentState = getCurrentServiceState(phoneId);
        FiveGServiceState lastState = getLastServiceState(phoneId);
        if ( !currentState.equals(lastState) ) {

            if ( DEBUG ) {
                Log.d(TAG, "phoneId(" + phoneId + ") Change in state from " + lastState + " \n"+
                        "\tto " + currentState);

            }

            lastState.copyFrom(currentState);
            IFiveGStateListener listener = mStatesListeners.get(phoneId);
            if (listener != null) {
                listener.onStateChanged(currentState);
            }

            mHandler.sendEmptyMessage(MESSAGE_NOTIFIY_MONITOR_CALLBACK);

        }
    }

    private void initFiveGServiceState() {
        Log.d(TAG, "initFiveGServiceState size=" + mStatesListeners.size());
        for( int i=0; i < mStatesListeners.size(); ++i ) {
            int phoneId = mStatesListeners.keyAt(i);
            initFiveGServiceState(phoneId);
        }
    }

    private void initFiveGServiceState(int phoneId) {
        Log.d(TAG, "mServiceConnected=" + mServiceConnected + " mClient=" + mClient);
        if ( mServiceConnected && mClient != null) {
            Log.d(TAG, "query 5G service state for phoneId " + phoneId);
            try {
                Token token = mExtTelephonyManager.queryNrIconType(phoneId, mClient);
                Log.d(TAG, "queryNrIconType result:" + token);
            } catch (Exception e) {
                Log.d(TAG, "initFiveGServiceState: Exception = " + e);
                if ( mInitRetryTimes < MAX_RETRY && !mHandler.hasMessages(MESSAGE_REINIT) ) {
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REINIT,
                            DELAY_MILLISECOND + mInitRetryTimes*DELAY_INCREMENT);
                    mInitRetryTimes +=1;
                }
            }
        }
    }

    @VisibleForTesting
    void update5GIcon(FiveGServiceState state,int phoneId) {
        state.mIconGroup = getNrIconGroup(state.mNrIconType, phoneId);
        Log.d(TAG,"update5GIcon state.mNrIconType = " + state.mNrIconType);
    }

    private MobileIconGroup getNrIconGroup(int nrIconType , int phoneId) {
        MobileIconGroup iconGroup = TelephonyIcons.UNKNOWN;
        switch (nrIconType){
            case NrIconType.TYPE_5G_BASIC:
                //[BUG]-Modify-Begin by shaopan.tang 2022-03-24 [FP4-3618]Display 5G instead of 5G basic
                //iconGroup = TelephonyIcons.FIVE_G_BASIC;
                iconGroup = TelephonyIcons.FIVE_G;
                //[BUG]-Modify-End by shaopan.tang
                break;
            case NrIconType.TYPE_5G_UWB:
                iconGroup = TelephonyIcons.FIVE_G_UWB;
                break;
        }
        return iconGroup;
    }

    private void notifyMonitorCallback() {
        for (int i = 0; i < mKeyguardUpdateMonitorCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = mKeyguardUpdateMonitorCallbacks.get(i).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
            }
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch ( msg.what ) {
                case MESSAGE_REBIND:
                    connectService();
                    break;

                case MESSAGE_REINIT:
                    initFiveGServiceState();
                    break;

                case MESSAGE_NOTIFIY_MONITOR_CALLBACK:
                    notifyMonitorCallback();
                    break;
            }

        }
    };


    @VisibleForTesting
    protected IExtPhoneCallback mCallback = new ExtPhoneCallbackBase() {
        @Override
        public void onNrIconType(int slotId, Token token, Status status, NrIconType
                nrIconType) throws RemoteException {
            Log.d(TAG,
                    "onNrIconType: slotId = " + slotId + " token = " + token + " " + "status"
                            + status + " NrIconType = " + nrIconType);

            //add by huan.sun for FP4S-665/666 show 4G icon under volte call at 20221024
            newNrIconType = nrIconType.get();
            Log.d(TAG, "newNrIconType= " + newNrIconType);
            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mNrIconType = nrIconType.get();
                update5GIcon(state, slotId);
                notifyListenersIfNecessary(slotId);
            }
        }
    };

    public interface IFiveGStateListener {
        public void onStateChanged(FiveGServiceState state);
    }
}
