package me.rijul.brightnessvolumeslider;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

import de.robv.android.xposed.XposedHelpers;

class VolumeListener extends ContentObserver {
    private AudioManager mAudioManager;

    VolumeListener(Handler handler, AudioManager audioManager) {
        super(handler);
        mAudioManager = audioManager;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return false;
    }

    @Override
    public void onChange(boolean selfChange) {
        Row row;
        int currentVolume;
        int id;
        for(int i=0; i<Common.streams.size(); ++i) {
            row = Common.rows.get(i);
            currentVolume = mAudioManager.getStreamVolume(row.mStream);
            XposedHelpers.callMethod(row.mToggleSlider, "setValue", currentVolume);
            Context moduleContext = Common.getContext(row.mToggleSlider.getContext(), BuildConfig.APPLICATION_ID);
            if (moduleContext != null) {
                id = currentVolume == 0 ? Common.mutedIconDrawables.get(i) : Common.iconDrawables.get(i);
                row.mIcon.setImageDrawable(moduleContext.getDrawable(id));
            }
        }
    }
}