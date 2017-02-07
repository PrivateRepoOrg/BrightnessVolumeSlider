package me.rijul.brightnessvolumeslider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by rijul on 4/2/17.
 */

class VolumeBarListener implements SeekBar.OnSeekBarChangeListener, AsyncVolumeSetter.VolumeSetListener {
    private AudioManager mAudioManager;
    private Object mMirrorController;

    VolumeBarListener(AudioManager audioManager, Object mirrorController) {
        mAudioManager = audioManager;
        mMirrorController = mirrorController;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (progress < 0)
                seekBar.setProgress(0);
            new AsyncVolumeSetter(mAudioManager, Common.rows.get(Common.seekBars.indexOf(seekBar.getId())),
                    getImpliedLevel(seekBar.getMax(), seekBar.getProgress()), this).execute();
        }
    }



    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        new AsyncVolumeSetter(mAudioManager, Common.rows.get(Common.seekBars.indexOf(seekBar.getId())),
                getImpliedLevel(seekBar.getMax(), seekBar.getProgress()), this).execute();
        if (mMirrorController != null) {
            XposedHelpers.callMethod(mMirrorController, "showMirror");
            XposedHelpers.callMethod(mMirrorController, "setLocation", (View) seekBar.getParent().getParent());
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        new AsyncVolumeSetter(mAudioManager, Common.rows.get(Common.seekBars.indexOf(seekBar.getId())),
                getImpliedLevel(seekBar.getMax(), seekBar.getProgress()), this).execute();
        if (mMirrorController != null) {
            XposedHelpers.callMethod(mMirrorController, "hideMirror");
        }
    }

    private static int getImpliedLevel(final int m, int progress) {
        return progress;
    }

    @Override
    public void onVolumeSet(int volume, Row row) {
        int index = Common.streams.indexOf(row.mStream);
        Context moduleContext = Common.getContext(row.mToggleSlider.getContext(), BuildConfig.APPLICATION_ID);
        if (moduleContext!=null) {
            int id = volume == 0 ? Common.mutedIconDrawables.get(index) : Common.iconDrawables.get(index);
            Drawable drawable = moduleContext.getDrawable(id);
            row.mMirror.mIcon.setImageDrawable(drawable);
            row.mIcon.setImageDrawable(drawable);
        }
    }
}
