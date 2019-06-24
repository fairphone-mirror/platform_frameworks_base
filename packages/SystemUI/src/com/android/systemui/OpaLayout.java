package com.google.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.AnimatorSet.Builder;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.OverviewProxyService.OverviewProxyListener;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.phone.ButtonInterface;
import android.annotation.DrawableRes;
import java.util.ArrayList;
import com.android.systemui.R;
import com.android.settingslib.Utils;


public class OpaLayout
  extends FrameLayout
  implements ButtonInterface
{
  private static final String TAG = "OpaLayout";
  private final Interpolator HOME_DISAPPEAR_INTERPOLATOR = new PathInterpolator(0.65F, 0.0F, 1.0F, 1.0F);
  private final ArrayList<View> mAnimatedViews = new ArrayList();
  private int mAnimationState = 0;
  private View mBlue;
  private View mBottom;
  private final Runnable mCheckLongPress = new Runnable()
  {
    public void run()
    {
      if (mIsPressed) {
           mLongClicked = true;
      }
    }
  };
  private final ArraySet<Animator> mCurrentAnimators = new ArraySet();
  private boolean mDelayTouchFeedback;
   private final Runnable mDiamondAnimation = new Runnable()
  {
    public void run()
    {
        cancelCurrentAnimation();
         startDiamondAnimation();
    }
  };
  private boolean mDiamondAnimationDelayed;
  private final Interpolator mDiamondInterpolator = new PathInterpolator(0.2F, 0.0F, 0.2F, 1.0F);
  private long mGestureAnimationSetDuration;
  private AnimatorSet mGestureAnimatorSet;
  private AnimatorSet mGestureLineSet;
  private int mGestureState = 0;
  private View mGreen;
  private ImageView mHalo;
  private int mHaloDiameter;
  private KeyButtonView mHome;
  private boolean mIsPressed;
  private boolean mIsVertical;
  private View mLeft;
  private boolean mLongClicked;
  private boolean mOpaEnabled;
  private boolean mOpaEnabledNeedsUpdate;
  private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener()
  {
    public void onConnectionChanged(boolean paramAnonymousBoolean)
    {
        updateOpaLayout();
    }

    public void onInteractionFlagsChanged(int paramAnonymousInt)
    {
        updateOpaLayout();
    }
  };
  private OverviewProxyService mOverviewProxyService;
  private View mRed;
  private Resources mResources;
  private final Runnable mRetract = new Runnable()
  {
    public void run()
    {
      cancelCurrentAnimation();
      startRetractAnimation();
    }
  };
  private View mRight;
  private int mScrollTouchSlop;
  private long mStartTime;
  private View mTop;
  private int mTouchDownX;
  private int mTouchDownY;
  private ImageView mWhite;
  private ImageView mWhiteCutout;
  private boolean mWindowVisible;
  private View mYellow;
  private Context mContext;
  private KeyButtonDrawable mHaloIcon;
  private View mCurrentView = null;

  public OpaLayout(Context paramContext)
  {
    this(paramContext, null);
  }

  public OpaLayout(Context paramContext, AttributeSet paramAttributeSet)
  {
    this(paramContext, paramAttributeSet, 0);
  }

  public OpaLayout(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
  {
    this(paramContext, paramAttributeSet, paramInt, 0);
  }

  public OpaLayout(Context paramContext, AttributeSet paramAttributeSet, int paramInt1, int paramInt2)
  {
    super(paramContext, paramAttributeSet, paramInt1, paramInt2);
    mScrollTouchSlop = ViewConfiguration.get(paramContext).getScaledTouchSlop();
    mContext=paramContext;
  }

  private boolean allowAnimations()
  {
    boolean bool;
    if ((isAttachedToWindow()) && (mWindowVisible)) {
      bool = true;
    } else {
      bool = false;
    }
    return bool;
  }

  private void cancelCurrentAnimation()
  {
    if (!mCurrentAnimators.isEmpty())
    {
      for (int i = mCurrentAnimators.size() - 1; i >= 0; i--)
      {
        Animator localAnimator = (Animator)mCurrentAnimators.valueAt(i);
        localAnimator.removeAllListeners();
        localAnimator.cancel();
      }
      mCurrentAnimators.clear();
      mAnimationState = 0;
    }
    if (mGestureAnimatorSet != null)
    {
      mGestureAnimatorSet.cancel();
      mGestureState = 0;
    }
  }

  private void endCurrentAnimation()
  {
    if (!mCurrentAnimators.isEmpty())
    {
      for (int i = mCurrentAnimators.size() - 1; i >= 0; i--)
      {
        Animator localAnimator = (Animator)mCurrentAnimators.valueAt(i);
        localAnimator.removeAllListeners();
        localAnimator.end();
      }
      mCurrentAnimators.clear();
    }
    mAnimationState = 0;
  }

  private ArraySet<Animator> getCollapseAnimatorSet()
  {
    ArraySet localArraySet = new ArraySet();
    Animator localAnimator1 = OpaUtils.getScaleAnimatorX(mWhite, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);
    Animator localAnimator2 = OpaUtils.getScaleAnimatorY(mWhite, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);
    Animator localAnimator3 = OpaUtils.getScaleAnimatorX(mWhiteCutout, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);
    Animator localAnimator4 = OpaUtils.getScaleAnimatorY(mWhiteCutout, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);
    Animator localAnimator5 = OpaUtils.getScaleAnimatorX(mHalo, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);
    Animator localAnimator6 = OpaUtils.getScaleAnimatorY(mHalo, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);
    Animator localAnimator7 = OpaUtils.getAlphaAnimator(mHalo, 1.0F, 150, Interpolators.FAST_OUT_SLOW_IN);     
    if (mIsVertical) {
      localAnimator1 = OpaUtils.getTranslationAnimatorY(mRed, OpaUtils.INTERPOLATOR_40_OUT, 133);
    } else {
      localAnimator1 = OpaUtils.getTranslationAnimatorX(mRed, OpaUtils.INTERPOLATOR_40_OUT, 133);
    }
    localArraySet.add(localAnimator1);
    localArraySet.add(OpaUtils.getScaleAnimatorX(mRed, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mRed, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    if (mIsVertical) {
      localAnimator1 = OpaUtils.getTranslationAnimatorY(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 150);
    } else {
      localAnimator1 = OpaUtils.getTranslationAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 150);
    }
    localArraySet.add(localAnimator1);
    localArraySet.add(OpaUtils.getScaleAnimatorX(mBlue, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mBlue, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    if (mIsVertical) {
      localAnimator1 = OpaUtils.getTranslationAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 133);
    } else {
      localAnimator1 = OpaUtils.getTranslationAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 133);
    }
    localArraySet.add(localAnimator1);
    localArraySet.add(OpaUtils.getScaleAnimatorX(mYellow, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mYellow, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    if (this.mIsVertical) {
      localAnimator1 = OpaUtils.getTranslationAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 150);
    } else {
      localAnimator1 = OpaUtils.getTranslationAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 150);
    }
    localArraySet.add(localAnimator1);
    localArraySet.add(OpaUtils.getScaleAnimatorX(mGreen, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mGreen, 1.0F, 200, OpaUtils.INTERPOLATOR_40_OUT));
    localAnimator1.setStartDelay(33L);
    localAnimator2.setStartDelay(33L);
    localAnimator3.setStartDelay(33L);
    localAnimator4.setStartDelay(33L);
    localAnimator5.setStartDelay(33L);
    localAnimator6.setStartDelay(33L);
    localAnimator7.setStartDelay(33L);
    localArraySet.add(localAnimator1);
    localArraySet.add(localAnimator2);
    localArraySet.add(localAnimator3);
    localArraySet.add(localAnimator4);
    localArraySet.add(localAnimator5);
    localArraySet.add(localAnimator6);
    localArraySet.add(localAnimator7);
    getLongestAnim(localArraySet).addListener(new AnimatorListenerAdapter()
    {
      public void onAnimationEnd(Animator paramAnonymousAnimator)
      {
        mCurrentAnimators.clear();
        skipToStartingValue();
      }
    });
    return localArraySet;
  }

  private ArraySet<Animator> getDiamondAnimatorSet()
  {
    ArraySet localArraySet = new ArraySet();
    localArraySet.add(OpaUtils.getDeltaAnimatorY(mTop, this.mDiamondInterpolator, -OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mTop, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mTop, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getDeltaAnimatorY(mBottom, this.mDiamondInterpolator, OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mBottom, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mBottom, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getDeltaAnimatorX(mLeft, mDiamondInterpolator, -OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mLeft, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mLeft, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getDeltaAnimatorX(mRight, mDiamondInterpolator, OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mRight, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mRight, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mWhite, 0.625F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mWhite, 0.625F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mWhiteCutout, 0.625F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mWhiteCutout, 0.625F, 200, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mHalo, 0.47619048F, 100, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mHalo, 0.47619048F, 100, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getAlphaAnimator(mHalo, 0.0F, 100, Interpolators.FAST_OUT_SLOW_IN));
    getLongestAnim(localArraySet).addListener(new AnimatorListenerAdapter()
    {
      public void onAnimationCancel(Animator paramAnonymousAnimator)
      {
        mCurrentAnimators.clear();
      }
      
      public void onAnimationEnd(Animator paramAnonymousAnimator)
      {
        startLineAnimation();
      }
    });
    return localArraySet;
  }

  private AnimatorSet getGestureAnimatorSet()
  {
    if (mGestureLineSet != null)
    {
      mGestureLineSet.removeAllListeners();
      mGestureLineSet.cancel();
      return mGestureLineSet;
    }
    mGestureLineSet = new AnimatorSet();
    ObjectAnimator localObjectAnimator1 = OpaUtils.getScaleObjectAnimator(mWhite, 0.0F, 100, OpaUtils.INTERPOLATOR_40_OUT);
    ObjectAnimator localObjectAnimator2 = OpaUtils.getScaleObjectAnimator(mWhiteCutout, 0.0F, 100, OpaUtils.INTERPOLATOR_40_OUT);
    ObjectAnimator localObjectAnimator3 = OpaUtils.getScaleObjectAnimator(mHalo, 0.0F, 100, OpaUtils.INTERPOLATOR_40_OUT);
    localObjectAnimator1.setStartDelay(50L);
    localObjectAnimator2.setStartDelay(50L);
    mGestureLineSet.play(localObjectAnimator1).with(localObjectAnimator2).with(localObjectAnimator3);
    localObjectAnimator2 = OpaUtils.getScaleObjectAnimator(mTop, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN);
    mGestureLineSet.play(localObjectAnimator2).with(localObjectAnimator1).with(OpaUtils.getAlphaObjectAnimator(mRed, 1.0F, 50, 130, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(this.mYellow, 1.0F, 50, 130, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(mBlue, 1.0F, 50, 113, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(mGreen, 1.0F, 50, 113, Interpolators.LINEAR)).with(OpaUtils.getScaleObjectAnimator(mBottom, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN)).with(OpaUtils.getScaleObjectAnimator(mLeft, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN)).with(OpaUtils.getScaleObjectAnimator(mRight, 0.8F, 200, Interpolators.FAST_OUT_SLOW_IN));
    if (mIsVertical)
    {
      localObjectAnimator1 = OpaUtils.getTranslationObjectAnimatorY(mRed, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), mRed.getY() + OpaUtils.getDeltaDiamondPositionLeftY(), 350);
      localObjectAnimator1.addListener(new AnimatorListenerAdapter()
      {
        public void onAnimationEnd(Animator paramAnonymousAnimator)
        {
          startCollapseAnimation();
        }
      });
      mGestureLineSet.play(localObjectAnimator1).with(localObjectAnimator3).with(OpaUtils.getTranslationObjectAnimatorY(mBlue, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getY() + OpaUtils.getDeltaDiamondPositionBottomY(mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getY() + OpaUtils.getDeltaDiamondPositionRightY(), 350)).with(OpaUtils.getTranslationObjectAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getY() + OpaUtils.getDeltaDiamondPositionTopY(mResources), 350));
    }
    else
    {
      localObjectAnimator3 = OpaUtils.getTranslationObjectAnimatorX(mRed, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mRed.getX() + OpaUtils.getDeltaDiamondPositionTopX(), 350);
      localObjectAnimator3.addListener(new AnimatorListenerAdapter()
      {
        public void onAnimationEnd(Animator paramAnonymousAnimator)
        {
          startCollapseAnimation();
        }
      });
      mGestureLineSet.play(localObjectAnimator3).with(localObjectAnimator1).with(OpaUtils.getTranslationObjectAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getX() + OpaUtils.getDeltaDiamondPositionLeftX(mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getX() + OpaUtils.getDeltaDiamondPositionBottomX(), 350)).with(OpaUtils.getTranslationObjectAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getX() + OpaUtils.getDeltaDiamondPositionRightX(mResources), 350));
    }
    return mGestureLineSet;
  }

  private ArraySet<Animator> getLineAnimatorSet()
  {
    ArraySet localArraySet = new ArraySet();
    if (mIsVertical)
    {
      localArraySet.add(OpaUtils.getDeltaAnimatorY(mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
      localArraySet.add(OpaUtils.getDeltaAnimatorX(mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
      localArraySet.add(OpaUtils.getDeltaAnimatorY(mBlue, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
      localArraySet.add(OpaUtils.getDeltaAnimatorY(mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
      localArraySet.add(OpaUtils.getDeltaAnimatorX(mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
      localArraySet.add(OpaUtils.getDeltaAnimatorY(mGreen, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
    }
    else
    {
      localArraySet.add(OpaUtils.getDeltaAnimatorX(mRed, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
      localArraySet.add(OpaUtils.getDeltaAnimatorY(mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
      localArraySet.add(OpaUtils.getDeltaAnimatorX(mBlue, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
      localArraySet.add(OpaUtils.getDeltaAnimatorX(mYellow, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
      localArraySet.add(OpaUtils.getDeltaAnimatorY(mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
      localArraySet.add(OpaUtils.getDeltaAnimatorX(mGreen, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
    }
    localArraySet.add(OpaUtils.getScaleAnimatorX(mWhite, 0.0F, 83, HOME_DISAPPEAR_INTERPOLATOR));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mWhite, 0.0F, 83, HOME_DISAPPEAR_INTERPOLATOR));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mWhiteCutout, 0.0F, 83, HOME_DISAPPEAR_INTERPOLATOR));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mWhiteCutout, 0.0F, 83, HOME_DISAPPEAR_INTERPOLATOR));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mHalo, 0.0F, 83, HOME_DISAPPEAR_INTERPOLATOR));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mHalo, 0.0F, 83, HOME_DISAPPEAR_INTERPOLATOR));
    getLongestAnim(localArraySet).addListener(new AnimatorListenerAdapter()
    {
      public void onAnimationCancel(Animator paramAnonymousAnimator)
      {
        mCurrentAnimators.clear();
      }
      
      public void onAnimationEnd(Animator paramAnonymousAnimator)
      {
        startCollapseAnimation();
      }
    });
    return localArraySet;
  }

  private Animator getLongestAnim(ArraySet<Animator> paramArraySet)
  {
    long l1 = Long.MIN_VALUE;
    Object localObject = null;
    int i = paramArraySet.size() - 1;
    while (i >= 0)
    {
      Animator localAnimator = (Animator)paramArraySet.valueAt(i);
      long l2 = l1;
      if (localAnimator.getTotalDuration() > l1)
      {
        localObject = localAnimator;
        l2 = localAnimator.getTotalDuration();
      }
      i--;
      l1 = l2;
    }
    return (Animator)localObject;
  }

  private ArraySet<Animator> getRetractAnimatorSet()
  {
    ArraySet localArraySet = new ArraySet();
    localArraySet.add(OpaUtils.getTranslationAnimatorX(mRed, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getTranslationAnimatorY(mRed, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mRed, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mRed, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getTranslationAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getTranslationAnimatorY(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mBlue, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mBlue, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getTranslationAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getTranslationAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mGreen, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mGreen, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getTranslationAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getTranslationAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 190));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mYellow, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mYellow, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mWhite, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mWhite, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mWhiteCutout, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mWhiteCutout, 1.0F, 190, OpaUtils.INTERPOLATOR_40_OUT));
    localArraySet.add(OpaUtils.getScaleAnimatorX(mHalo, 1.0F, 190, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getScaleAnimatorY(mHalo, 1.0F, 190, Interpolators.FAST_OUT_SLOW_IN));
    localArraySet.add(OpaUtils.getAlphaAnimator(mHalo, 1.0F, 190, Interpolators.FAST_OUT_SLOW_IN));
    getLongestAnim(localArraySet).addListener(new AnimatorListenerAdapter()
    {
      public void onAnimationEnd(Animator paramAnonymousAnimator)
      {
        mCurrentAnimators.clear();
        skipToStartingValue();
      }
    });
    return localArraySet;
  }

  private void setDotsVisible()
  {
    int i = this.mAnimatedViews.size();
    for (int j = 0; j < i; j++) {
      ((View)this.mAnimatedViews.get(j)).setAlpha(1.0F);
    }
  }

  private void skipToStartingValue()
  {
    int i = this.mAnimatedViews.size();
    for (int j = 0; j < i; j++)
    {
      View localView = (View)this.mAnimatedViews.get(j);
      localView.setScaleY(1.0F);
      localView.setScaleX(1.0F);
      localView.setTranslationY(0.0F);
      localView.setTranslationX(0.0F);
      localView.setAlpha(0.0F);
    }
    mHalo.setAlpha(1.0F);
    mWhite.setAlpha(1.0F);
    mWhiteCutout.setAlpha(1.0F);
    mAnimationState = 0;
    mGestureState = 0;
  }

  private void startAll(ArraySet<Animator> paramArraySet)
  {
    for (int i = paramArraySet.size() - 1; i >= 0; i--) {
      ((Animator)paramArraySet.valueAt(i)).start();
    }
  }

  private void startCollapseAnimation()
  {
    if (allowAnimations())
    {
      mCurrentAnimators.clear();
      mCurrentAnimators.addAll(getCollapseAnimatorSet());
      mAnimationState = 3;
      startAll(this.mCurrentAnimators);
    }
    else
    {
      skipToStartingValue();
    }
  }

  private void startDiamondAnimation()
  {
    if (allowAnimations())
    {
      mCurrentAnimators.clear();
      setDotsVisible();
      mCurrentAnimators.addAll(getDiamondAnimatorSet());
      mAnimationState = 1;
      startAll(this.mCurrentAnimators);
    }
    else
    {
      skipToStartingValue();
    }
  }

  private void startLineAnimation()
  {
    if (allowAnimations())
    {
      mCurrentAnimators.clear();
      mCurrentAnimators.addAll(getLineAnimatorSet());
      mAnimationState = 3;
      startAll(this.mCurrentAnimators);
    }
    else
    {
      skipToStartingValue();
    }
  }

  private void startRetractAnimation()
  {
    if (allowAnimations())
    {
      mCurrentAnimators.clear();
      mCurrentAnimators.addAll(getRetractAnimatorSet());
      mAnimationState = 2;
      startAll(this.mCurrentAnimators);
    }
    else
    {
      skipToStartingValue();
    }
  }

  public void abortCurrentGesture()
  {
    mHome.abortCurrentGesture();
    mIsPressed = false;
    mLongClicked = false;
    mDiamondAnimationDelayed = false;
    removeCallbacks(mDiamondAnimation);
    removeCallbacks(mCheckLongPress);
    if ((mAnimationState == 3) || (mAnimationState == 1)) {
      mRetract.run();
    }
  }

  public boolean getOpaEnabled()
  {
    Log.d(TAG, "getOpaEnabled mOpaEnabled "+mOpaEnabled );
    return mOpaEnabled;
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    mOverviewProxyService.addCallback(this.mOverviewProxyListener);
    updateOpaLayout();
  }

  protected void onConfigurationChanged(Configuration paramConfiguration)
  {
    super.onConfigurationChanged(paramConfiguration);
    Log.d(TAG, "onConfigurationChanged" );
    updateOpaLayout();
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
  }

  protected void onFinishInflate()
  {
    super.onFinishInflate();
    mResources = getResources();
    mBlue = findViewById(R.id.blue);
    mRed = findViewById(R.id.red);
    mYellow = findViewById(R.id.yellow);
    mGreen = findViewById(R.id.green);
    mWhite = ((ImageView)findViewById(R.id.white));
    mWhiteCutout = ((ImageView)findViewById(R.id.white_cutout));
    mHalo = ((ImageView)findViewById(R.id.halo));
    mHome = ((KeyButtonView)findViewById(R.id.home_button));
    Log.d(TAG, "onFinishInflate" );

    mHaloDiameter = mResources.getDimensionPixelSize(R.dimen.halo_diameter);

    Paint paint = new Paint();
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    mWhiteCutout.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    mAnimatedViews.add(mBlue);
    mAnimatedViews.add(mRed);
    mAnimatedViews.add(mYellow);
    mAnimatedViews.add(mGreen);
    mAnimatedViews.add(mWhite);
    mAnimatedViews.add(mWhiteCutout);
    mAnimatedViews.add(mHalo);
    mOpaEnabledNeedsUpdate = true;
    mOverviewProxyService = ((OverviewProxyService)Dependency.get(OverviewProxyService.class));
    boolean quickStepEnabled = mOverviewProxyService.shouldShowSwipeUpUI();
    mOpaEnabled=!quickStepEnabled;     
    setOpaEnabled(mOpaEnabled); 
  }

  public boolean onInterceptTouchEvent(MotionEvent paramMotionEvent)
  {
     Log.d(TAG, "onInterceptTouchEvent" );
    if ((getOpaEnabled()) && (ValueAnimator.areAnimatorsEnabled()) && (mGestureState == 0))
    {
      int action=paramMotionEvent.getAction();
      Log.d(TAG, "onInterceptTouchEvent action "+action );
      int i =1;          
      int j = 1;
      int k = 1;
      switch (action)
      {
      default: 
        break;
      case MotionEvent.ACTION_MOVE: 
        i = Math.abs((int)paramMotionEvent.getRawX() - mTouchDownX);
        if (mIsVertical) {
          j = QuickStepContract.getQuickStepTouchSlopPx();
        } else {
          j = QuickStepContract.getQuickScrubTouchSlopPx();
        }
        if (i > j) {
          j = 1;
        } else {
          j = 0;
        }
        int m = Math.abs((int)paramMotionEvent.getRawY() - this.mTouchDownY);
        if (mIsVertical) {
          i = QuickStepContract.getQuickScrubTouchSlopPx();
        } else {
          i = QuickStepContract.getQuickStepTouchSlopPx();
        }
        if (m > i) {
          i = k;
        } else {
          i = 0;
        }
        if ((j != 0) || (i != 0)) {
          abortCurrentGesture();
        }
        break;
      case MotionEvent.ACTION_UP: 
      case MotionEvent.ACTION_CANCEL:
        if (mDiamondAnimationDelayed)
        {
          if ((mIsPressed) && (!mLongClicked)) {
            postDelayed(mRetract, 200L);
          }
        }
        else
        {
          if (mAnimationState == 1)
          {
            long l1 = SystemClock.elapsedRealtime();
            long l2 = mStartTime;
            removeCallbacks(mRetract);
            postDelayed(mRetract, 100L - (l1 - l2));
            removeCallbacks(mDiamondAnimation);
            removeCallbacks(mCheckLongPress);
            return false;
          }
          if ((!mIsPressed) || (mLongClicked)) {
            j = 0;
          }
          if (j != 0) {
            mRetract.run();
          }
        }
        mIsPressed = false;
        break;
      case  MotionEvent.ACTION_DOWN: 
        mTouchDownX = ((int)paramMotionEvent.getRawX());
        mTouchDownY = ((int)paramMotionEvent.getRawY());
        j = 0;
        if (!mCurrentAnimators.isEmpty()) {
          if (mAnimationState == 2)
          {
            endCurrentAnimation();
            j = 1;
          }
          else
          {
            return false;
          }
        }
        mStartTime = SystemClock.elapsedRealtime();
        mLongClicked = false;
        mIsPressed = true;
        removeCallbacks(mDiamondAnimation);
        removeCallbacks(mRetract);
        removeCallbacks(mCheckLongPress);
        postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
        if ((mDelayTouchFeedback) && (j == 0))
        {
          mDiamondAnimationDelayed = true;
          postDelayed(mDiamondAnimation, ViewConfiguration.getTapTimeout());
        }
        else
        {
          mDiamondAnimationDelayed = false;
          startDiamondAnimation();
        }
        break;
      }
      return false;
    }
    return false;
  }


  public void onWindowVisibilityChanged(int paramInt)
  {
    super.onWindowVisibilityChanged(paramInt);
    boolean bool;
    if (paramInt == 0) {
      bool = true;
    } else {
      bool = false;
    }
    mWindowVisible = bool;
    if (paramInt == 0)
    {
      updateOpaLayout();
    }
    else
    {
      cancelCurrentAnimation();
      skipToStartingValue();
    }
  }

  public void setAccessibilityDelegate(View.AccessibilityDelegate paramAccessibilityDelegate)
  {
    super.setAccessibilityDelegate(paramAccessibilityDelegate);
    mHome.setAccessibilityDelegate(paramAccessibilityDelegate);
  }

  public void setDarkIntensity(float paramFloat)
  {
      Log.d(TAG, "setDarkIntensity  paramFloat "+paramFloat );
    if ((mWhite.getDrawable() instanceof KeyButtonDrawable)) {
      ((KeyButtonDrawable)mWhite.getDrawable()).setDarkIntensity(paramFloat);
    }
     if ((mWhite.getDrawable() instanceof KeyButtonDrawable)) {
    ((KeyButtonDrawable)mHalo.getDrawable()).setDarkIntensity(paramFloat);
    }
    mWhite.invalidate();
    mHalo.invalidate();
    mHome.setDarkIntensity(paramFloat);
  }

  public void setDelayTouchFeedback(boolean paramBoolean)
  {
    mHome.setDelayTouchFeedback(paramBoolean);
    mDelayTouchFeedback = paramBoolean;
  }

  public void setImageDrawable(Drawable paramDrawable)
  {
    mWhite.setImageDrawable(paramDrawable);
    mWhiteCutout.setImageDrawable(paramDrawable);
    Log.d(TAG, "setImageDrawable" );
  }

  public void setOnLongClickListener(View.OnLongClickListener paramOnLongClickListener)
  {
    mHome.setOnLongClickListener(paramOnLongClickListener);
  }

  public void setOnTouchListener(View.OnTouchListener paramOnTouchListener)
  {
    mHome.setOnTouchListener(paramOnTouchListener);
  }

  public void setOpaEnabled(boolean paramBoolean)
  {
    mOpaEnabled = paramBoolean;
    mOpaEnabledNeedsUpdate = false;
    updateOpaLayout();
    Log.d(TAG, "setOpaEnabled paramBoolean "+paramBoolean );
  }

  public void setHaloImageDrawable(Drawable paramDrawable)
  {
    mHalo.setImageDrawable(paramDrawable);
    Log.d(TAG, "setHaloImageDrawable" );
  }

  public void setVertical(boolean paramBoolean)
  {
    if ((mIsVertical != paramBoolean) && (mGestureAnimatorSet != null))
    {
      mGestureAnimatorSet.cancel();
      mGestureAnimatorSet = null;
      skipToStartingValue();
    }
    mIsVertical = paramBoolean;
    mHome.setVertical(paramBoolean);
    Log.d(TAG, "setVertical "+paramBoolean);
    if (mIsVertical)
    {
      mTop = mGreen;
      mBottom = mBlue;
      mRight = mYellow;
      mLeft = mRed;
    }
    else
    {
      mTop = mRed;
      mBottom = mYellow;
      mLeft = mBlue;
      mRight = mGreen;
    }
    Log.d(TAG, "setVertical  X");
  }

  public void updateOpaLayout()
  {
    Log.d(TAG, "updateOpaLayout" );
    boolean bool1 = mOverviewProxyService.shouldShowSwipeUpUI();
    boolean bool2 = mOpaEnabled;
    int i = 0;
    int j;
    if ((bool2) && (!bool1)) {
      j = 1;
    } else {
      j = 0;
    }
    Object localObject;
    if (j != 0) {
      j = i;
    } else {
      j = 4;
    }
    Log.d(TAG, "updateOpaLayout halo visibility "+j );
    Log.d(TAG, "updateOpaLayout  mHalo alpha "+mHalo.getAlpha());

    ((ImageView)mHalo).setVisibility(j);
    localObject = (FrameLayout.LayoutParams)mWhite.getLayoutParams();
    i = -1;
    if (bool1) {
      j = -1;
    } else {
      j = mHaloDiameter;
    }
    ((FrameLayout.LayoutParams)localObject).width = j;
    if (bool1) {
      j = i;
    } else {
      j = mHaloDiameter;
    }
    ((FrameLayout.LayoutParams)localObject).height = j;
    mWhite.setLayoutParams((ViewGroup.LayoutParams)localObject);
    mWhiteCutout.setLayoutParams((ViewGroup.LayoutParams)localObject);
  }
}
