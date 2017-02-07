package me.rijul.brightnessvolumeslider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by rijul on 4/2/17.
 */

class Common {
    static List<Integer> layouts = Arrays.asList(R.layout.media_layout, R.layout.ringer_layout, R.layout.alarm_layout);
    static List<Integer> icons = Arrays.asList(R.id.media_icon, R.id.ringer_icon, R.id.alarm_icon);
    static List<Integer> toggleSliders = Arrays.asList(R.id.media_slider, R.id.ringer_slider, R.id.alarm_slider);
    static List<Integer> iconDrawables = Arrays.asList(R.drawable.ic_volume_media, R.drawable.ic_volume_notification, R.drawable.ic_volume_alarm);
    static List<Integer> mutedIconDrawables = Arrays.asList(R.drawable.ic_volume_media_mute, R.drawable.ic_volume_ringer_vibrate, R.drawable.ic_volume_alarm_mute);
    static List<Integer> streams = Arrays.asList(AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM);
    static List<Integer> seekBars = Arrays.asList(R.id.media_seekbar, R.id.ringer_seekbar, R.id.alarm_seekbar);
    static ArrayList<Row> rows = new ArrayList<Row>();

    static void log(String s) {
        XposedBridge.log("[BrightnessVolumeSlider" + BuildConfig.VERSION_NAME + "] " + s);
    }

    static Context getContext(Context context, String packageName) {
        try {
            return context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
