package com.google.android.systemui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.res.Resources;
import android.util.ArraySet;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;

public final class OpaUtils
{
  static final Interpolator INTERPOLATOR_40_40 = new PathInterpolator(0.4F, 0.0F, 0.6F, 1.0F);
  static final Interpolator INTERPOLATOR_40_OUT = new PathInterpolator(0.4F, 0.0F, 1.0F, 1.0F);

  static Animator getAlphaAnimator(View paramView, float paramFloat, int paramInt1, int paramInt2, Interpolator paramInterpolator)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(11, paramFloat);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt1);
    localRenderNodeAnimator.setStartDelay(paramInt2);
    return localRenderNodeAnimator;
  }

  static Animator getAlphaAnimator(View paramView, float paramFloat, int paramInt, Interpolator paramInterpolator)
  {
    return getAlphaAnimator(paramView, paramFloat, paramInt, 0, paramInterpolator);
  }

  static ObjectAnimator getAlphaObjectAnimator(View paramView, float paramFloat, int paramInt1, int paramInt2, Interpolator paramInterpolator)
  {
    ObjectAnimator animator;
    animator = ObjectAnimator.ofFloat(paramView, View.ALPHA, paramFloat);
    animator.setInterpolator(paramInterpolator);
    animator.setDuration(paramInt1);
    animator.setStartDelay(paramInt2);
    return animator;
  }

  static Animator getDeltaAnimatorX(View paramView, Interpolator paramInterpolator, float paramFloat, int paramInt)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(8, paramView.getX() + paramFloat);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt);
    return localRenderNodeAnimator;
  }

  static Animator getDeltaAnimatorY(View paramView, Interpolator paramInterpolator, float paramFloat, int paramInt)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(9, paramView.getY() + paramFloat);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt);
    return localRenderNodeAnimator;
  }

  static float getDeltaDiamondPositionBottomX()
  {
    return 0.0F;
  }

  static float getDeltaDiamondPositionBottomY(Resources paramResources)
  {
    return getPxVal(paramResources, R.dimen.opa_diamond_translation);
  }

  static float getDeltaDiamondPositionLeftX(Resources paramResources)
  {
    return -getPxVal(paramResources, R.dimen.opa_diamond_translation);
  }

  static float getDeltaDiamondPositionLeftY()
  {
    return 0.0F;
  }

  static float getDeltaDiamondPositionRightX(Resources paramResources)
  {
    return getPxVal(paramResources, R.dimen.opa_diamond_translation);
  }

  static float getDeltaDiamondPositionRightY()
  {
    return 0.0F;
  }

  static float getDeltaDiamondPositionTopX()
  {
    return 0.0F;
  }

  static float getDeltaDiamondPositionTopY(Resources paramResources)
  {
    return -getPxVal(paramResources, R.dimen.opa_diamond_translation);
  }

  static Animator getLongestAnim(ArraySet<Animator> paramArraySet)
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

  static float getPxVal(Resources paramResources, int paramInt)
  {
    return paramResources.getDimensionPixelOffset(paramInt);
  }

  static Animator getScaleAnimatorX(View paramView, float paramFloat, int paramInt, Interpolator paramInterpolator)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(3, paramFloat);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt);
    return localRenderNodeAnimator;
  }

  static Animator getScaleAnimatorY(View paramView, float paramFloat, int paramInt, Interpolator paramInterpolator)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(4, paramFloat);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt);
    return localRenderNodeAnimator;
  }

  static ObjectAnimator getScaleObjectAnimator(View paramView, float paramFloat, int paramInt, Interpolator paramInterpolator)
  {
    ObjectAnimator anim;
    anim = ObjectAnimator.ofPropertyValuesHolder(paramView, new PropertyValuesHolder[] { PropertyValuesHolder.ofFloat(View.SCALE_X, new float[] { paramFloat }), PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[] { paramFloat }) });
    anim.setDuration(paramInt);
    anim.setInterpolator(paramInterpolator);
    return anim;
  }

  static Animator getTranslationAnimatorX(View paramView, Interpolator paramInterpolator, int paramInt)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(0, 0.0F);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt);
    return localRenderNodeAnimator;
  }

  static Animator getTranslationAnimatorY(View paramView, Interpolator paramInterpolator, int paramInt)
  {
    RenderNodeAnimator localRenderNodeAnimator = new RenderNodeAnimator(1, 0.0F);
    localRenderNodeAnimator.setTarget(paramView);
    localRenderNodeAnimator.setInterpolator(paramInterpolator);
    localRenderNodeAnimator.setDuration(paramInt);
    return localRenderNodeAnimator;
  }

  static ObjectAnimator getTranslationObjectAnimatorX(View paramView, Interpolator paramInterpolator, float paramFloat1, float paramFloat2, int paramInt)
  {
    ObjectAnimator animator;
    animator = ObjectAnimator.ofFloat(paramView, View.X,  paramFloat2, paramFloat2 + paramFloat1);
    animator.setInterpolator(paramInterpolator);
    animator.setDuration(paramInt);
    return animator;
  }

  static ObjectAnimator getTranslationObjectAnimatorY(View paramView, Interpolator paramInterpolator, float paramFloat1, float paramFloat2, int paramInt)
  {
    ObjectAnimator animator;
    animator = ObjectAnimator.ofFloat(paramView, View.Y, paramFloat2, paramFloat2 + paramFloat1);
    animator.setInterpolator(paramInterpolator);
    animator.setDuration(paramInt);
    return animator;
  }
}
