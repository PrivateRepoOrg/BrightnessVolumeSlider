package me.rijul.brightnessvolumeslider;

import android.media.AudioManager;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * Created by rijul on 5/2/17.
 */

public class AsyncVolumeSetter extends AsyncTask<Void, Void, Void> {
    public interface VolumeSetListener {
        public void onVolumeSet(int volume, Row row);
    }

    AudioManager mAudioManager;
    Row mRow;
    int mVolume;
    VolumeSetListener mListener;

    AsyncVolumeSetter(AudioManager audioManager, Row row, int volume, VolumeSetListener listener) {
        mAudioManager = audioManager;
        mRow = row;
        mVolume = volume;
        mListener = listener;
    }
    @Override
    protected Void doInBackground(Void[] params) {
        mAudioManager.setStreamVolume(mRow.mStream, mVolume, 0);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (mListener!=null)
            mListener.onVolumeSet(mVolume, mRow);
    }
}
