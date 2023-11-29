/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.keyguard;

import android.annotation.CallSuper;
import android.annotation.Nullable;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingCollector;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.flags.Flags;
import com.android.systemui.statusbar.policy.DevicePostureController;
import com.android.systemui.util.ViewController;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.android.systemui.FaceUnlockUtil;

import javax.inject.Inject;


/** Controller for a {@link KeyguardSecurityView}. */
public abstract class KeyguardInputViewController<T extends KeyguardInputView>
        extends ViewController<T> implements KeyguardSecurityView {

    private final SecurityMode mSecurityMode;
    private final KeyguardSecurityCallback mKeyguardSecurityCallback;
    private final EmergencyButton mEmergencyButton;
    private final EmergencyButtonController mEmergencyButtonController;
    private boolean mPaused;
    protected KeyguardMessageAreaController<BouncerKeyguardMessageArea> mMessageAreaController;
    protected ImageView mIvFaceUnlock;

    // The following is used to ignore callbacks from SecurityViews that are no longer current
    // (e.g. face unlock). This avoids unwanted asynchronous events from messing with the
    // state for the current security method.
    private KeyguardSecurityCallback mNullCallback = new KeyguardSecurityCallback() {};
    private final FeatureFlags mFeatureFlags;

    protected KeyguardInputViewController(T view, SecurityMode securityMode,
            KeyguardSecurityCallback keyguardSecurityCallback,
            EmergencyButtonController emergencyButtonController,
            @Nullable KeyguardMessageAreaController.Factory messageAreaControllerFactory,
            FeatureFlags featureFlags) {
        super(view);
        mSecurityMode = securityMode;
        mKeyguardSecurityCallback = keyguardSecurityCallback;
        mEmergencyButton = view == null ? null : view.findViewById(R.id.emergency_call_button);
        mEmergencyButtonController = emergencyButtonController;
        mFeatureFlags = featureFlags;
        if (messageAreaControllerFactory != null) {
            try {
                BouncerKeyguardMessageArea kma = view.requireViewById(R.id.bouncer_message_area);
                mMessageAreaController = messageAreaControllerFactory.create(kma);
                mMessageAreaController.init();
                mMessageAreaController.setIsVisible(true);
            } catch (IllegalArgumentException exception) {
                Log.e("KeyguardInputViewController",
                        "Ensure that a BouncerKeyguardMessageArea is included in the layout");
            }
        }
        try{
            mIvFaceUnlock = view.requireViewById(R.id.iv_face_unlock);
        }catch (Exception e){
            Log.e("KeyguardInputViewController", "init face unlock view error");
        }
    }

    private final View.OnClickListener mFaceIconClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(FaceUnlockUtil.getInstance().isFaceUnlockEnable(getContext())){
                FaceUnlockUtil.getInstance().startFaceUnlock(getContext());
                if(mMessageAreaController != null){
                    mMessageAreaController.setMessage(R.string.face_unlocking);
                }
                getKeyguardSecurityCallback().userActivity();
            }
        }
    };

    private final FaceUnlockUtil.FaceUnlockCallback mCallback = new FaceUnlockUtil.FaceUnlockCallback() {
        @Override
        public void onFaceAuthResult(int failTimes){
            if(mMessageAreaController != null){
                if(failTimes >= 3) {
                    switch(getSecurityMode()){
                        case Pattern:
                            mMessageAreaController.setMessage(R.string.required_to_use_pattern);
                            break;
                        case Password:
                            mMessageAreaController.setMessage(R.string.required_to_use_password);
                            break;
                        default:
                            mMessageAreaController.setMessage(R.string.required_to_use_pin);
                            break;
                    }
                    stopFaceViewAnim();
                    if(mIvFaceUnlock != null) {
                        mIvFaceUnlock.setImageDrawable(getContext().getDrawable(R.drawable.ic_lock));
                    }
                } else if(failTimes > 0) {
                    mMessageAreaController.setMessage(R.string.face_unlock_failed);
                    shakeFaceView();
                } else {
                    stopFaceViewAnim();
                    if(mIvFaceUnlock != null) {
                        mIvFaceUnlock.setVisibility(View.GONE);
                    }
                }
            }
        }

        @Override
        public void onStartFaceUnlock(){
            scaleFaceView();
        }
    };

    private void scaleFaceView() {
        if(mIvFaceUnlock != null) {
            mIvFaceUnlock.setImageDrawable(getContext().getDrawable(R.drawable.face_dialog_pulse_dark_to_light));
            stopFaceViewAnim();
            ScaleAnimation scaleAnim = new ScaleAnimation(1.0f,0.75f,1.0f,0.75f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF,0.5f);
            scaleAnim.setRepeatCount(20);
            scaleAnim.setDuration(200);
            scaleAnim.setRepeatMode(Animation.REVERSE);
            mIvFaceUnlock.startAnimation(scaleAnim);
        }
    }

    private void shakeFaceView() {
        if(mIvFaceUnlock != null) {
            mIvFaceUnlock.setImageDrawable(getContext().getDrawable(R.drawable.face_dialog_pulse_dark_to_light));
            stopFaceViewAnim();
            mIvFaceUnlock.setOnClickListener(mFaceIconClickListener);
            TranslateAnimation transAnim = new TranslateAnimation(0, -10, 0, 0);
            transAnim.setRepeatCount(10);
            transAnim.setDuration(150);
            transAnim.setRepeatMode(Animation.REVERSE);
            mIvFaceUnlock.startAnimation(transAnim);
        }
    }

    private void stopFaceViewAnim() {
        if(mIvFaceUnlock != null) {
            mIvFaceUnlock.clearAnimation();
            mIvFaceUnlock.setOnClickListener(null);
        }
    }

    @Override
    protected void onInit() {
        mEmergencyButtonController.init();
    }

    @Override
    @CallSuper
    protected void onViewAttached() {
        updateMessageAreaVisibility();
        if(FaceUnlockUtil.getInstance().isFaceUnlockEnable(getContext())){
            FaceUnlockUtil.getInstance().addCallback(mCallback);
        }
    }

    private void updateMessageAreaVisibility() {
        if (mMessageAreaController == null) return;
        if (mFeatureFlags.isEnabled(Flags.REVAMPED_BOUNCER_MESSAGES)) {
            mMessageAreaController.disable();
        } else {
            mMessageAreaController.setIsVisible(true);
        }
    }


    @Override
    protected void onViewDetached() {
        if(mIvFaceUnlock != null){
            mIvFaceUnlock.setVisibility(View.GONE);
            mIvFaceUnlock.setOnClickListener(null);
        }
        stopFaceViewAnim();
        FaceUnlockUtil.getInstance().removeCallback(mCallback);
    }

    SecurityMode getSecurityMode() {
        return mSecurityMode;
    }

    protected KeyguardSecurityCallback getKeyguardSecurityCallback() {
        if (mPaused) {
            return mNullCallback;
        }

        return mKeyguardSecurityCallback;
    }

    @Override
    public void reset() {
        mMessageAreaController.setMessage("", false);
    }

    @Override
    public void onPause() {
        mPaused = true;
    }

    @Override
    public void onResume(int reason) {
        mPaused = false;
        if(mIvFaceUnlock != null){
            mIvFaceUnlock.setVisibility(FaceUnlockUtil.getInstance().hasFaceUnlock(getContext()) ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void showPromptReason(int reason) {
    }

    @Override
    public void showMessage(CharSequence message, ColorStateList colorState, boolean animated) {
    }

    public void startAppearAnimation() {
        if (TextUtils.isEmpty(mMessageAreaController.getMessage())
                && getInitialMessageResId() != 0) {
            mMessageAreaController.setMessage(
                    mView.getResources().getString(getInitialMessageResId()),
                    /* animate= */ false);
        }
        mView.startAppearAnimation();
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return mView.startDisappearAnimation(finishRunnable);
    }

    @Override
    public CharSequence getTitle() {
        return mView.getTitle();
    }

    /** Finds the index of this view in the suppplied parent view. */
    public int getIndexIn(KeyguardSecurityViewFlipper view) {
        return view.indexOfChild(mView);
    }

    /** Determines the message to show in the bouncer when it first appears. */
    protected abstract int getInitialMessageResId();

    /** Factory for a {@link KeyguardInputViewController}. */
    public static class Factory {
        private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
        private final LockPatternUtils mLockPatternUtils;
        private final LatencyTracker mLatencyTracker;
        private final KeyguardMessageAreaController.Factory mMessageAreaControllerFactory;
        private final InputMethodManager mInputMethodManager;
        private final DelayableExecutor mMainExecutor;
        private final Resources mResources;
        private final LiftToActivateListener mLiftToActivateListener;
        private final TelephonyManager mTelephonyManager;
        private final EmergencyButtonController.Factory mEmergencyButtonControllerFactory;
        private final FalsingCollector mFalsingCollector;
        private final DevicePostureController mDevicePostureController;
        private final KeyguardViewController mKeyguardViewController;
        private final FeatureFlags mFeatureFlags;

        @Inject
        public Factory(KeyguardUpdateMonitor keyguardUpdateMonitor,
                LockPatternUtils lockPatternUtils,
                LatencyTracker latencyTracker,
                KeyguardMessageAreaController.Factory messageAreaControllerFactory,
                InputMethodManager inputMethodManager, @Main DelayableExecutor mainExecutor,
                @Main Resources resources, LiftToActivateListener liftToActivateListener,
                TelephonyManager telephonyManager, FalsingCollector falsingCollector,
                EmergencyButtonController.Factory emergencyButtonControllerFactory,
                DevicePostureController devicePostureController,
                KeyguardViewController keyguardViewController,
                FeatureFlags featureFlags) {
            mKeyguardUpdateMonitor = keyguardUpdateMonitor;
            mLockPatternUtils = lockPatternUtils;
            mLatencyTracker = latencyTracker;
            mMessageAreaControllerFactory = messageAreaControllerFactory;
            mInputMethodManager = inputMethodManager;
            mMainExecutor = mainExecutor;
            mResources = resources;
            mLiftToActivateListener = liftToActivateListener;
            mTelephonyManager = telephonyManager;
            mEmergencyButtonControllerFactory = emergencyButtonControllerFactory;
            mFalsingCollector = falsingCollector;
            mDevicePostureController = devicePostureController;
            mKeyguardViewController = keyguardViewController;
            mFeatureFlags = featureFlags;
        }

        /** Create a new {@link KeyguardInputViewController}. */
        public KeyguardInputViewController create(KeyguardInputView keyguardInputView,
                SecurityMode securityMode, KeyguardSecurityCallback keyguardSecurityCallback) {
            EmergencyButtonController emergencyButtonController =
                    mEmergencyButtonControllerFactory.create(
                            keyguardInputView.findViewById(R.id.emergency_call_button));

            if (keyguardInputView instanceof KeyguardPatternView) {
                return new KeyguardPatternViewController((KeyguardPatternView) keyguardInputView,
                        mKeyguardUpdateMonitor, securityMode, mLockPatternUtils,
                        keyguardSecurityCallback, mLatencyTracker, mFalsingCollector,
                        emergencyButtonController, mMessageAreaControllerFactory,
                        mDevicePostureController, mFeatureFlags);
            } else if (keyguardInputView instanceof KeyguardPasswordView) {
                return new KeyguardPasswordViewController((KeyguardPasswordView) keyguardInputView,
                        mKeyguardUpdateMonitor, securityMode, mLockPatternUtils,
                        keyguardSecurityCallback, mMessageAreaControllerFactory, mLatencyTracker,
                        mInputMethodManager, emergencyButtonController, mMainExecutor, mResources,
                        mFalsingCollector, mKeyguardViewController,
                        mFeatureFlags);
            } else if (keyguardInputView instanceof KeyguardPINView) {
                return new KeyguardPinViewController((KeyguardPINView) keyguardInputView,
                        mKeyguardUpdateMonitor, securityMode, mLockPatternUtils,
                        keyguardSecurityCallback, mMessageAreaControllerFactory, mLatencyTracker,
                        mLiftToActivateListener, emergencyButtonController, mFalsingCollector,
                        mDevicePostureController, mFeatureFlags);
            } else if (keyguardInputView instanceof KeyguardSimPinView) {
                return new KeyguardSimPinViewController((KeyguardSimPinView) keyguardInputView,
                        mKeyguardUpdateMonitor, securityMode, mLockPatternUtils,
                        keyguardSecurityCallback, mMessageAreaControllerFactory, mLatencyTracker,
                        mLiftToActivateListener, mTelephonyManager, mFalsingCollector,
                        emergencyButtonController, mFeatureFlags);
            } else if (keyguardInputView instanceof KeyguardSimPukView) {
                return new KeyguardSimPukViewController((KeyguardSimPukView) keyguardInputView,
                        mKeyguardUpdateMonitor, securityMode, mLockPatternUtils,
                        keyguardSecurityCallback, mMessageAreaControllerFactory, mLatencyTracker,
                        mLiftToActivateListener, mTelephonyManager, mFalsingCollector,
                        emergencyButtonController, mFeatureFlags);
            }

            throw new RuntimeException("Unable to find controller for " + keyguardInputView);
        }
    }
}
