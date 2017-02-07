package me.rijul.brightnessvolumeslider;

import android.animation.LayoutTransition;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Created by rijul on 12/12/16.
 */
public class XposedMod implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
    private AudioManager mAudioManager;
    private Drawable mThumb;
    private VolumeListener mVolumeListener;
    private VolumeBarListener mVolumeBarListener;
    private RowExpander mRowExpander;
    private View.OnClickListener mMuteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context moduleContext = Common.getContext(v.getContext(), BuildConfig.APPLICATION_ID);
            int index = Common.icons.indexOf(v.getId());
            Row row = Common.rows.get(index);
            if (mAudioManager.getStreamVolume(row.mStream)==0) {
                mAudioManager.setStreamVolume(row.mStream, row.mLastAudibleLevel, 0);
                XposedHelpers.callMethod(row.mToggleSlider, "setValue", row.mLastAudibleLevel);
                if (moduleContext!=null)
                    row.mIcon.setImageDrawable(moduleContext.getDrawable(Common.iconDrawables.get(index)));
            } else {
                row.mLastAudibleLevel = mAudioManager.getStreamVolume(row.mStream);
                mAudioManager.setStreamVolume(row.mStream, 0, 0);
                XposedHelpers.callMethod(row.mToggleSlider, "setValue", 0);
                if (moduleContext!=null)
                    row.mIcon.setImageDrawable(moduleContext.getDrawable(Common.mutedIconDrawables.get(index)));
            }
        }
    };
    private BroadcastReceiver mSilentModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Context moduleContext = Common.getContext(context, BuildConfig.APPLICATION_ID);
            if (moduleContext==null)
                return;
            if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION) && intent.hasExtra(AudioManager.EXTRA_RINGER_MODE)) {
                int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                Row row = Common.rows.get(Common.streams.indexOf(AudioManager.STREAM_RING));
                int iconAddress = R.drawable.ic_volume_notification;
                int zen_mode;
                float alpha = 1.0f;
                try {
                    zen_mode = Settings.Global.getInt(context.getContentResolver(), "zen_mode");
                    if ((zen_mode==0) || (zen_mode==1))
                        iconAddress = (currentVolume==0) ? R.drawable.ic_volume_ringer_vibrate : R.drawable.ic_volume_notification;
                    else {
                        iconAddress = R.drawable.ic_volume_ringer_mute;
                        alpha = 0.5f;
                        }
                } catch (Settings.SettingNotFoundException ignored) {}

                Drawable drawable = moduleContext.getDrawable(iconAddress);
                row.mIcon.setImageDrawable(drawable);
                row.mIcon.setAlpha(alpha);
                row.mMirror.mIcon.setImageDrawable(drawable);
                row.mMirror.mIcon.setAlpha(alpha);

                XposedHelpers.callMethod(row.mToggleSlider, "setValue", currentVolume);
            }
        }
    };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            Class<?> QSPanel = XposedHelpers.findClass("com.android.systemui.qs.QSPanel", lpparam.classLoader);
            Class<?> QSDragPanel = XposedHelpers.findClass("com.android.systemui.qs.QSDragPanel", lpparam.classLoader);
            XC_MethodHook setBrightnessMirrorHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    for(Row row : Common.rows) {
                        XposedHelpers.callMethod(row.mToggleSlider, "setMirror", row.mMirror.mToggleSlider);
                        XposedHelpers.callMethod(row.mToggleSlider, "setMirrorController", param.args[0]);
                    }
                    mVolumeBarListener = new VolumeBarListener(mAudioManager, param.args[0]);
                    }
                };
            try {
                findAndHookMethod(QSPanel, "setBrightnessMirror", "com.android.systemui.statusbar.policy.BrightnessMirrorController", setBrightnessMirrorHook);
            } catch (NoSuchMethodError ignored) {
                Common.log("Could not hook QSPanel#setBrightnessMirror(BrightnessMirrorController)");
            }
            try {
                findAndHookMethod(QSDragPanel, "setBrightnessMirror", "com.android.systemui.statusbar.policy.BrightnessMirrorController", setBrightnessMirrorHook);
            } catch (NoSuchMethodError ignored) {
                Common.log("Could not hook QSDragPanel#setBrightnessMirror(BrightnessMirrorController)");
            }

            Class<?> BrightnessMirrorController = XposedHelpers.findClass("com.android.systemui.statusbar.policy.BrightnessMirrorController", lpparam.classLoader);
            findAndHookMethod(BrightnessMirrorController, "setLocation", View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.args[0] = ((View) param.args[0]).getParent();
                }
            });

            XC_MethodHook setUpViewsHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                    final Context moduleContext = Common.getContext(context, BuildConfig.APPLICATION_ID);
                    if (moduleContext==null)
                        return;

                    mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    mVolumeListener = new VolumeListener(new Handler(), mAudioManager);

                    Row row;
                    int currentVolume;
                    Drawable drawable;
                    for(int i=0; i<Common.rows.size(); ++i) {
                        row = Common.rows.get(i);
                        currentVolume = mAudioManager.getStreamVolume(row.mStream);
                        XposedHelpers.callMethod(row.mToggleSlider, "setMax", mAudioManager.getStreamMaxVolume(row.mStream));
                        XposedHelpers.callMethod(row.mToggleSlider, "setValue", currentVolume);

                        drawable = moduleContext.getDrawable((currentVolume==0) ?
                                Common.mutedIconDrawables.get(i) : Common.iconDrawables.get(i));
                        row.mIcon.setImageDrawable(drawable);
                        row.mMirror.mIcon.setImageDrawable(drawable);
                        row.mIcon.setOnClickListener(mMuteListener);
                    }
                }
            };
            try {
                findAndHookMethod(QSPanel, "setupViews", setUpViewsHook);
            } catch (NoSuchMethodError ignored) {
                Common.log("Could not hook QSPanel#setupViews()");
            }
            try {
                findAndHookMethod(QSDragPanel, "setupViews", setUpViewsHook);
            } catch (NoSuchMethodError ignored) {
                Common.log("Could not hook QSDragPanel#setupViews()");
            }

            //use this hook to turn on or off listening for events
            XC_MethodHook setListeningHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    boolean listening = (boolean) param.args[0];
                    View mBrightnessView = (View) XposedHelpers.getObjectField(param.thisObject, "mBrightnessView");
                    if (listening) {
                        mBrightnessView.getContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mVolumeListener);
                        mBrightnessView.getContext().registerReceiver(mSilentModeReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
                    } else {
                        //mBrightnessView.getContext().getContentResolver().unregisterContentObserver(mVolumeListener);
                        mBrightnessView.getContext().unregisterReceiver(mSilentModeReceiver);
                    }
                    for (Row row : Common.rows)
                        row.mSeekBar.setOnSeekBarChangeListener(listening ? mVolumeBarListener : null);
                }
            };
            try {
                findAndHookMethod(QSPanel, "setListening", boolean.class, setListeningHook);
            } catch (NoSuchMethodError ignored) {
                Common.log("Could not hook QSPanel#setListening(boolean)");
            }
            try {
                findAndHookMethod(QSDragPanel, "setListening", boolean.class, setListeningHook);
            } catch (NoSuchMethodError ignored) {
                Common.log("Could not hook QSDragPanel#setListening(boolean)");
            }

            XC_MethodHook initRowHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object row = param.getResult();
                    SeekBar seekBar = (SeekBar) XposedHelpers.getObjectField(row, "slider");
                    mThumb = seekBar.getThumb();
                }
            };
            try {
                Class<?> VolumeDialog = XposedHelpers.findClass("com.android.systemui.volume.VolumeDialog", lpparam.classLoader);
                findAndHookMethod(VolumeDialog, "initRow", int.class, int.class, int.class, boolean.class, initRowHook);
            } catch (XposedHelpers.ClassNotFoundError | ClassCastException | NoSuchMethodError e) {
                Common.log("Could not hook VolumeDialog#initRow(int,int,int,boolean)");
                XposedBridge.log(e);
            }
        }
    }

    @Override
    public void handleInitPackageResources(final InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.systemui"))
            return;

        resparam.res.hookLayout(resparam.packageName, "layout", "quick_settings_brightness_dialog", new XC_LayoutInflated() {
            boolean alreadyRun = false;
            ArrayList<Row> mirrorRows = new ArrayList<>();
            LayoutTransition mLayoutTransition;

            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                LinearLayout parentBrightnessView = (LinearLayout) liparam.view;
                Context context = parentBrightnessView.getContext();
                Context moduleContext = Common.getContext(context, BuildConfig.APPLICATION_ID);
                if (moduleContext==null)
                    return;
                if (Common.rows.size()>=3)
                    return;

                parentBrightnessView.setOrientation(LinearLayout.VERTICAL);
                //parentBrightnessView.setPadding(0,parentBrightnessView.getPaddingTop(),0,0);

                int brightnessIconId = resparam.res.getIdentifier("brightness_icon", "id", resparam.packageName);
                ImageView brightnessIcon = (ImageView) parentBrightnessView.findViewById(brightnessIconId);
                parentBrightnessView.removeView(brightnessIcon);

                int brightnessSliderId = resparam.res.getIdentifier("brightness_slider", "id", resparam.packageName);
                RelativeLayout brightnessToggleSlider = (RelativeLayout) parentBrightnessView.findViewById(brightnessSliderId);
                parentBrightnessView.removeView(brightnessToggleSlider);

                RelativeLayout brightnessRow = (RelativeLayout) LayoutInflater.from(moduleContext).
                        inflate(R.layout.brightness_layout, parentBrightnessView, false);
                //brightnessRow.setPaddingRelative(brightnessRow.getPaddingStart(), brightnessRow.getPaddingTop() +
                  //      Common.dpToPx(context, 8), brightnessRow.getPaddingEnd(), brightnessRow.getPaddingBottom());

                ImageView newBrightnessIcon = (ImageView) brightnessRow.findViewById(R.id.brightness_icon);
                RelativeLayout.LayoutParams newBrightnessIconLayoutParams =
                        (RelativeLayout.LayoutParams) newBrightnessIcon.getLayoutParams();
                brightnessRow.removeView(newBrightnessIcon);

                SeekBar newBrightnessSeekBar = (SeekBar) brightnessRow.findViewById(R.id.brightness_slider);
                RelativeLayout.LayoutParams brightnessSeekBarLayoutParams
                        = (RelativeLayout.LayoutParams) newBrightnessSeekBar.getLayoutParams();
                brightnessSeekBarLayoutParams.removeRule(RelativeLayout.END_OF);
                brightnessSeekBarLayoutParams.addRule(RelativeLayout.END_OF, brightnessIconId);
                brightnessRow.removeView(newBrightnessSeekBar);

                ImageView expandButton = (ImageView) brightnessRow.findViewById(R.id.volume_expand_button);
                brightnessRow.removeView(expandButton);

                brightnessIcon.setLayoutParams(newBrightnessIconLayoutParams);
                brightnessRow.addView(brightnessIcon);
                brightnessIcon.setVisibility(View.VISIBLE);

                SeekBar brightnessSeekBar = (SeekBar) XposedHelpers.getObjectField(brightnessToggleSlider, "mSlider");
                brightnessSeekBar.setPaddingRelative(Common.dpToPx(context, 8), 0, Common.dpToPx(context, 8), 0);
                if (mThumb!=null)
                    brightnessSeekBar.setThumb(mThumb.getConstantState().newDrawable());

                brightnessRow.addView(expandButton);
                expandButton.setVisibility(View.INVISIBLE);
                if (alreadyRun) {
                    //show and activate this only in the front
                    expandButton.setVisibility(View.VISIBLE);
                    mRowExpander = new RowExpander(expandButton);
                    expandButton.setOnClickListener(mRowExpander);
                }

                brightnessToggleSlider.setLayoutParams(brightnessSeekBarLayoutParams);
                brightnessRow.addView(brightnessToggleSlider);
                brightnessToggleSlider.setVisibility(View.VISIBLE);
                parentBrightnessView.addView(brightnessRow);
                brightnessToggleSlider.bringToFront();


                //LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) brightnessIcon.getLayoutParams();
                //LinearLayout.LayoutParams relativeLayoutParams = (LinearLayout.LayoutParams) toggleSlider.getLayoutParams();

                Class<?> ToggleSlider = XposedHelpers.findClass("com.android.systemui.settings.ToggleSlider",
                        brightnessToggleSlider.getClass().getClassLoader());
                XposedHelpers.findAndHookConstructor(ToggleSlider, Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        XposedHelpers.setObjectField(param.thisObject, "mContentDescription",
                                (CharSequence) "Random accessibility description");
                    }
                });

                RelativeLayout parent;
                RelativeLayout slider;
                ImageView icon;
                SeekBar seekBar;
                SeekBar originalSeekBar;
                for(int i=0; i<Common.layouts.size(); ++i) {
                    parent = (RelativeLayout) LayoutInflater.from(moduleContext).
                            inflate(Common.layouts.get(i), parentBrightnessView, false);
                    icon = (ImageView) parent.findViewById(Common.icons.get(i));
                    originalSeekBar = (SeekBar) parent.findViewById(Common.toggleSliders.get(i));

                    slider = (RelativeLayout) XposedHelpers.newInstance(ToggleSlider, context);
                    slider.setId(Common.toggleSliders.get(i));
                    slider.setLayoutParams(originalSeekBar.getLayoutParams());
                    parent.removeView(originalSeekBar);
                    seekBar = (SeekBar) XposedHelpers.getObjectField(slider, "mSlider");
                    seekBar.setId(Common.seekBars.get(i));
                    seekBar.setThumb(mThumb==null ? moduleContext.getDrawable(Common.iconDrawables.get(i)) :
                            mThumb.getConstantState().newDrawable());
                    seekBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
                    seekBar.setThumbTintMode(PorterDuff.Mode.SRC_ATOP);
                    seekBar.setPaddingRelative(Common.dpToPx(context, 8), 0, Common.dpToPx(context, 8), 0);

                    parent.setVisibility(View.GONE);

                    parent.addView(slider);
                    parentBrightnessView.addView(parent);



                    if (alreadyRun) {
                        Common.rows.add(new Row(parent, icon, slider, seekBar, Common.streams.get(i), mirrorRows.get(i)));
                    }
                    else {
                        mirrorRows.add(new Row(parent,icon,slider, seekBar, Common.streams.get(i), null));
                        //parent.removeView(parent.findViewById(R.id.volume_settings_button));
                        if (i>=Common.layouts.size()-1)
                            alreadyRun = true;
                    }

                    //pass on the events from parent into the slider
                    final RelativeLayout finalSlider = slider;
                    parent.setOnTouchListener(new View.OnTouchListener() {
                        private final Rect mSliderHitRect = new Rect();
                        private boolean mDragging;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            finalSlider.getHitRect(mSliderHitRect);
                            if (!mDragging && event.getActionMasked() == MotionEvent.ACTION_DOWN
                                    && event.getY() < mSliderHitRect.top) {
                                mDragging = true;
                            }
                            if (mDragging) {
                                event.offsetLocation(-mSliderHitRect.left, -mSliderHitRect.top);
                                finalSlider.dispatchTouchEvent(event);
                                if (event.getActionMasked() == MotionEvent.ACTION_UP
                                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                                    mDragging = false;
                                }
                                return true;
                            }
                            return false;
                        }
                    });
                }
            }
        });
    }
}