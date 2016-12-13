package me.rijul.brightnessvolumeslider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Created by rijul on 12/12/16.
 */
public class XposedMod implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
//        if (lpparam.packageName.equals("com.android.systemui")) {
//            Class<?> QSPanel = XposedHelpers.findClass("com.android.systemui.qs.QSPanel", lpparam.classLoader);
//            XposedHelpers.findAndHookMethod(QSPanel, "showBrightnessSlider", new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    super.beforeHookedMethod(param);
//
//                    View view = (View) param.thisObject;
//                    Context context = view.getContext();
//                    int sliderId = context.getResources().getIdentifier("brightness_slider", "id", lpparam.packageName);
//                    View brightnessSlider = (View) XposedHelpers.callMethod(param.thisObject, "findViewById", sliderId);
//                    brightnessSlider.setVisibility(View.GONE);
//
//                    View mBrightnessView = (View) XposedHelpers.getObjectField(param.thisObject, "mBrightnessView");
//                    mBrightnessView.setVisibility(View.VISIBLE);
//
//                    param.setResult(true);
//                }
//            });
//        }
    }

    @Override
    public void handleInitPackageResources(final InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.systemui"))
            return;

        resparam.res.hookLayout(resparam.packageName, "layout", "quick_settings_brightness_dialog", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                Context context = liparam.view.getContext();
                LinearLayout brightnessView = (LinearLayout) liparam.view;

                Context moduleContext = getContext(context, BuildConfig.APPLICATION_ID);
                if (moduleContext==null)    //uninstalled module, so fuck off
                    return;

                brightnessView.setOrientation(LinearLayout.VERTICAL);

                int iconId = resparam.res.getIdentifier("brightness_icon", "id", resparam.packageName);
                ImageView brightnessIcon = (ImageView) brightnessView.findViewById(iconId);
                brightnessView.removeView(brightnessIcon);

                int sliderId = resparam.res.getIdentifier("brightness_slider", "id", resparam.packageName);
                RelativeLayout toggleSlider = (RelativeLayout) brightnessView.findViewById(sliderId);
                brightnessView.removeView(toggleSlider);


/*
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) toggleSlider.getLayoutParams();
                Class<?> toggleSliderClass = XposedHelpers.findClass("com.android.systemui.settings.ToggleSlider", toggleSlider.getClass().getClassLoader());
                XposedBridge.log("[BrightnessVolumeSlider] toggleSliderClass is null ? " + (toggleSliderClass==null));
                XposedHelpers.findAndHookMethod(toggleSliderClass, "getContentDescription", XC_MethodReplacement.returnConstant("Fuck!"));
                RelativeLayout newToggleSlider = (RelativeLayout) XposedHelpers.newInstance(toggleSliderClass, context);

                newToggleSlider.setLayoutParams(llp);
                newToggleSlider.setId(sliderId);
*/
                LinearLayout plusLayout1 = (LinearLayout) LayoutInflater.from(moduleContext).inflate(R.layout.brightness_slider, null);
                plusLayout1.addView(brightnessIcon);
                toggleSlider.setVisibility(View.VISIBLE);
                brightnessIcon.setVisibility(View.VISIBLE);
                plusLayout1.addView(toggleSlider);

                LinearLayout plusLayout2 = (LinearLayout) LayoutInflater.from(moduleContext).inflate(R.layout.volume_slider, null);
                brightnessView.addView(plusLayout1);
                brightnessView.addView(plusLayout2);
            }
        });
    }

    Context getContext(Context context, String packageName) {
        try {
            return context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
