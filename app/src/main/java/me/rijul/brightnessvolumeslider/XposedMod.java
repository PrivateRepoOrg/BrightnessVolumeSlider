package me.rijul.brightnessvolumeslider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.XC_MethodHook;
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

                LinearLayout plusLayout1 = (LinearLayout) LayoutInflater.from(moduleContext).inflate(R.layout.brightness_slider, null);
                plusLayout1.addView(brightnessIcon);
                toggleSlider.setVisibility(View.VISIBLE);
                brightnessIcon.setVisibility(View.VISIBLE);
                plusLayout1.addView(toggleSlider);

                LinearLayout plusLayout2 = (LinearLayout) LayoutInflater.from(moduleContext).inflate(R.layout.ringer_slider, brightnessView, false);
                brightnessView.addView(plusLayout1);
                brightnessView.addView(plusLayout2);

                Class<?> ToggleSlider = XposedHelpers.findClass("com.android.systemui.settings.ToggleSlider", toggleSlider.getClass().getClassLoader());
                XposedHelpers.findAndHookConstructor(ToggleSlider, Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        XposedHelpers.setObjectField(param.thisObject, "mContentDescription", (CharSequence) "Random accessibility description");
                    }
                });

                RelativeLayout volumeSlider = (RelativeLayout) XposedHelpers.newInstance(ToggleSlider, context);
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) toggleSlider.getLayoutParams();
                volumeSlider.setLayoutParams(llp);
                plusLayout2.removeView(plusLayout2.findViewById(R.id.ringer_slider));
                volumeSlider.setId(R.id.ringer_slider);
                plusLayout2.addView(volumeSlider);
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
