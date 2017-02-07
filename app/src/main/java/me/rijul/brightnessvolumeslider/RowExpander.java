package me.rijul.brightnessvolumeslider;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by rijul on 6/2/17.
 */

class RowExpander implements View.OnClickListener {
    int mExpandButtonAnimationDuration;
    boolean mExpanded = false;
    ImageView mExpandButton;
    int mDuration = 100;

    RowExpander(ImageView button) {
        mExpandButton = button;
        mExpandButtonAnimationDuration = mDuration;
    }

    @Override
    public void onClick(final View v) {
        Drawable drawable = mExpandButton.getDrawable();
        if (drawable instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) drawable.getConstantState().newDrawable();
            mExpandButton.setImageDrawable(animatedVectorDrawable);
            mExpandButton.setClickable(false);
            animatedVectorDrawable.start();
            mExpandButton.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mExpanded = !mExpanded;
                    int identifier = mExpanded ? R.drawable.ic_volume_collapse_animation : R.drawable.ic_volume_expand_animation;
                    Context moduleContext = Common.getContext(v.getContext(), BuildConfig.APPLICATION_ID);
                    if (moduleContext!=null)
                        mExpandButton.setImageDrawable(moduleContext.getDrawable(identifier));
                    showOrHideRows(mExpanded ? 0 : Common.rows.size()-1);
                }
            }, mExpandButtonAnimationDuration);
        }
    }

    private void showOrHideRows(final int index) {
        final RelativeLayout parent, parentMirror;
        try {
            Row row = Common.rows.get(index);
            parent = row.mParent;
            parentMirror = row.mMirror.mParent;
        } catch (IndexOutOfBoundsException ignored) {
            mExpandButton.setClickable(true);
            return;
        }
        Context context = parent.getContext();
        Context moduleContext = Common.getContext(context, BuildConfig.APPLICATION_ID);
        if (moduleContext!=null) {
            Animation animation = AnimationUtils.loadAnimation(moduleContext, mExpanded ? R.anim.row_in : R.anim.row_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    showOrHideRows(mExpanded ? index + 1 : index - 1);
                    if (mExpanded) {
                        parent.setVisibility(View.VISIBLE);
                        parentMirror.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!mExpanded) {
                        parent.setVisibility(View.GONE);
                        parentMirror.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            parent.startAnimation(animation);
        }
    }
}
