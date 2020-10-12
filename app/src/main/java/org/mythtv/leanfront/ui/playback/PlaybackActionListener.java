/*
 * Copyright (c) 2020 Peter Bennett
 *
 * This file is part of MythTV-leanfront.
 *
 * MythTV-leanfront is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * MythTV-leanfront is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with MythTV-leanfront.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mythtv.leanfront.ui.playback;

import android.content.DialogInterface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.Action;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackParameters;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Playlist;
import org.mythtv.leanfront.player.VideoPlayerGlue;

class PlaybackActionListener implements VideoPlayerGlue.OnActionClickedListener {

    private final PlaybackFragment playbackFragment;
    private Playlist mPlaylist;

    static final float[] ASPECT_VALUES = {1.0f, 1.1847f, 1.333333f, 1.5f, 0.75f, 0.875f};
    int mAspectIndex = 0;

    private static final float[] SCALE_VALUES = {0.875f, 1.0f, 1.166666f, 1.333333f, 1.5f};
    private static final int SCALE_DEFAULT_INDEX = 1;
    private int mScaleIndex = SCALE_DEFAULT_INDEX;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;

    float mAspect = 1.0f;
    static float[] PIVOTY_VALUES = {0.5f, 0.25f, 0.0f, 1.0f, 0.75f};
    int mPivotYIndex = 0;
    float mPivotY = 0.5f;

    private static final String TAG = "lfe";
    private static final String CLASS = "PlaybackActionListener";

    PlaybackActionListener(PlaybackFragment playbackFragment, Playlist playlist) {
        this.playbackFragment = playbackFragment;
        this.mPlaylist = playlist;
    }

    @Override
    public void onPrevious() {
        playbackFragment.skipToPrevious();
    }

    @Override
    public void onNext() {
        playbackFragment.skipToNext();
    }

    @Override
    public void onPlayCompleted() {
        playbackFragment.markWatched(true);
        if (playbackFragment.mIsBounded) {
            Log.i(TAG, CLASS + " onPlayCompleted checking File Length.");
            playbackFragment.mIsPlayResumable = true;
            playbackFragment.getFileLength();
        }
    }

    @Override
    public void onZoom() {
        zoom(0);
    }


    // mode = -1 for smaller, 0 for rotate, 1 for bigger
    public void zoom(int mode) {
        if (mode == 0) {
            if (++mScaleIndex >= SCALE_VALUES.length)
                mScaleIndex = 0;
        }
        else {
            int newValue = mScaleIndex + mode;
            if (newValue >= SCALE_VALUES.length
                    || newValue < 0)
                return;
            mScaleIndex = newValue;
        }

        mScaleX = SCALE_VALUES[mScaleIndex];
        mScaleY = SCALE_VALUES[mScaleIndex];
        setScale();

        int vertPerc = Math.round(mScaleY * 100.0f);
        StringBuilder msg = new StringBuilder(playbackFragment.getContext().getString(R.string.playback_zoom_size))
                .append(" ").append(vertPerc).append("%");
        if (playbackFragment.mToast != null)
            playbackFragment.mToast.cancel();
        playbackFragment.mToast = Toast.makeText(playbackFragment.getContext(),
                msg, Toast.LENGTH_LONG);
        playbackFragment.mToast.show();
    }

    void setScale() {
        SurfaceView view = playbackFragment.getSurfaceView();
        int height = view.getHeight();
        view.setPivotY(mPivotY * height);
        view.setScaleX(mScaleX * mAspect);
        view.setScaleY(mScaleY);
    }

    @Override
    public void onAspect() {
        if (++mAspectIndex >= ASPECT_VALUES.length)
            mAspectIndex = 0;
        mAspect = ASPECT_VALUES[mAspectIndex];
        setScale();

        int stretchPerc = Math.round(mAspect * 100.0f);
        StringBuilder msg = new StringBuilder(playbackFragment.getActivity().getString(R.string.playback_aspect_stretch))
                .append(" ").append(stretchPerc).append("%");
        if (playbackFragment.mToast != null)
            playbackFragment.mToast.cancel();
        playbackFragment.mToast = Toast.makeText(playbackFragment.getActivity(),
                msg, Toast.LENGTH_LONG);
        playbackFragment.mToast.show();
    }

    @Override
    public void onCaption() {
        playbackFragment.mTextSelection = playbackFragment.trackSelector(C.TRACK_TYPE_TEXT, playbackFragment.mTextSelection,
                R.string.msg_subtitle_on, R.string.msg_subtitle_off, true, true);
    }

    @Override
    public void onPivot() {
        if (++mPivotYIndex >= PIVOTY_VALUES.length)
            mPivotYIndex = 0;
        mPivotY = PIVOTY_VALUES[mPivotYIndex];
        setScale();
        String msg = playbackFragment.getActivity().getResources().getStringArray(R.array.msg_pin_values)[mPivotYIndex];
        if (playbackFragment.mToast != null)
            playbackFragment.mToast.cancel();
        playbackFragment.mToast = Toast.makeText(playbackFragment.getActivity(),
                msg, Toast.LENGTH_LONG);
        playbackFragment.mToast.show();
    }

    @Override
    public void onSpeed() {
        showSpeedSelector();
    }

    private void showSpeedSelector() {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_speed)
                .setView(R.layout.leanback_preference_widget_seekbar);
        dialog = builder.create();
        dialog.show();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        dialog.getWindow().setAttributes(lp);
        SeekBar seekBar = dialog.findViewById(R.id.seekbar);
        seekBar.setMax(800);
        seekBar.setProgress(Math.round(playbackFragment.mSpeed * 100.0f));
        TextView seekValue = dialog.findViewById(R.id.seekbar_value);
        seekValue.setText( (int)(playbackFragment.mSpeed * 100.0f) + " %");
        dialog.setOnKeyListener(
            (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        dlg.dismiss();
                        return true;
                }
                if (event.getAction() != KeyEvent.ACTION_DOWN)
                    return false;
                int value;
                switch(keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        value = seekBar.getProgress();
                        if (value > 10)
                            value -= 10;
                        else
                            return true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        value = seekBar.getProgress();
                        if (value <= 790)
                            value += 10;
                        else
                            return true;
                        break;
                    default:
                        return false;
                }
                seekBar.setProgress(value);
                seekValue.setText(value + " %");
                playbackFragment.mSpeed = (float) value * 0.01f;
                PlaybackParameters parms = new PlaybackParameters(playbackFragment.mSpeed);
                playbackFragment.mPlayer.setPlaybackParameters(parms);
                return true;
            }
        );
    }

    @Override
    public void onRewind() {
        playbackFragment.rewind();
    }

    @Override
    public void onFastForward() {
        playbackFragment.fastForward();
    }

    @Override
    // unused as we do not have OSD icons for this
    public void onJumpForward() {
        playbackFragment.jumpForward();
    }

    @Override
    // unused as we do not have OSD icons for this
    public void onJumpBack() {
        playbackFragment.jumpBack();
    }

    @Override
    public void onActionSelected(Action action) {
        playbackFragment.actionSelected(action);
    }

    @Override
    public void onAudioTrack() {
        playbackFragment.mAudioSelection = playbackFragment.trackSelector(C.TRACK_TYPE_AUDIO, playbackFragment.mAudioSelection,
                R.string.msg_audio_track, R.string.msg_audio_track_off, true, true);
    }

}
