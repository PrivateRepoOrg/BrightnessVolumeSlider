package me.rijul.brightnessvolumeslider;

import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

/**
 * Created by rijul on 4/2/17.
 */

public class Row {
    RelativeLayout mParent;
    ImageView mIcon;
    RelativeLayout mToggleSlider;
    SeekBar mSeekBar;
    int mStream;
    int mLastAudibleLevel = 1;
    Row mMirror;

    Row(RelativeLayout parent, ImageView icon, RelativeLayout toggleSlider, SeekBar seekBar, int stream, Row mirror) {
        mParent = parent;
        mIcon = icon;
        mToggleSlider = toggleSlider;
        mSeekBar = seekBar;
        mStream = stream;
        mMirror = mirror;
    }
}
