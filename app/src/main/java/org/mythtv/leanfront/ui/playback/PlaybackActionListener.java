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
import android.view.Gravity;
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
    private float mScale = 1.0f;
    float mAspect = 1.0f;
    float mPivotX = 0.5f;
    float mPivotY = 0.5f;
    AlertDialog mDialog;

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
        showZoomSelector();
    }

    void setScale() {
        SurfaceView view = playbackFragment.getSurfaceView();
        int height = view.getHeight();
        int width = view.getWidth();
        float xScale = mScale * mAspect;
        if (mScale == 1.0f)
            mPivotY = 0.5f;
        if (xScale == 1.0f)
            mPivotX = 0.5f;
        view.setPivotX(mPivotX * width);
        view.setPivotY(mPivotY * height);
        view.setScaleX(xScale);
        view.setScaleY(mScale);
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
        showPivotSelector();
    }

    @Override
    public void onSpeed() {
        showSpeedSelector();
    }

    private void showSpeedSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_speed)
                .setView(R.layout.leanback_preference_widget_seekbar);
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        mDialog.getWindow().setAttributes(lp);
        SeekBar seekBar = mDialog.findViewById(R.id.seekbar);
        seekBar.setMax(800);
        seekBar.setProgress(Math.round(playbackFragment.mSpeed * 100.0f));
        TextView seekValue = mDialog.findViewById(R.id.seekbar_value);
        seekValue.setText( (int)(playbackFragment.mSpeed * 100.0f) + " %");
        mDialog.setOnKeyListener(
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
                    case KeyEvent.KEYCODE_BACK:
                        return false;
                    default:
                        dlg.dismiss();
                        playbackFragment.getActivity().onKeyDown(event.getKeyCode(), event);
                        return true;
                }
                seekBar.setProgress(value);
                seekValue.setText(value + " %");
                playbackFragment.mSpeed = (float) value * 0.01f;
                PlaybackParameters parms = new PlaybackParameters(playbackFragment.mSpeed);
                playbackFragment.mPlayer.setPlaybackParameters(parms);
                return true;
            }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                }
        );
    }

    private void showZoomSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_zoom)
                .setView(R.layout.leanback_preference_widget_seekbar);
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        mDialog.getWindow().setAttributes(lp);
        SeekBar seekBar = mDialog.findViewById(R.id.seekbar);
        seekBar.setMax(200);
        seekBar.setProgress(Math.round(mScale * 100.0f));
        TextView seekValue = mDialog.findViewById(R.id.seekbar_value);
        seekValue.setText( (int)(mScale * 100.0f) + " %");
        mDialog.setOnKeyListener(
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
                        case KeyEvent.KEYCODE_ZOOM_OUT:
                            value = seekBar.getProgress();
                            if (value > 5)
                                value -= 5;
                            else
                                return true;
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                        case KeyEvent.KEYCODE_ZOOM_IN:
                            value = seekBar.getProgress();
                            if (value <= 195)
                                value += 5;
                            else
                                return true;
                            break;
                        case KeyEvent.KEYCODE_BACK:
                            return false;
                        default:
                            dlg.dismiss();
                            playbackFragment.getActivity().onKeyDown(event.getKeyCode(), event);
                            return true;
                    }
                    seekBar.setProgress(value);
                    seekValue.setText(value + " %");
                    mScale = (float) value * 0.01f;
                    setScale();
                    return true;
                }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                }
        );
    }

    private void showPivotSelector() {
        int x = Math.round(mPivotX * 100.0f);
        int y = Math.round(mPivotY * 100.0f);
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_position)
                .setMessage(getPivotMessage());
        mDialog = builder.create();
        mDialog.show();
        TextView messageText = (TextView)mDialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        mDialog.getWindow().setAttributes(lp);
        mDialog.setOnKeyListener(
                (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            dlg.dismiss();
                            return true;
                    }
                    if (event.getAction() != KeyEvent.ACTION_DOWN)
                        return false;
                    int xvalue = Math.round(mPivotX * 100.0f);
                    int yvalue = Math.round(mPivotY * 100.0f);
                    switch(keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            if (xvalue > 0)
                                xvalue -= 10;
                            else
                                return true;
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            if (xvalue < 100)
                                xvalue += 10;
                            else
                                return true;
                            break;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            if (yvalue > 0)
                                yvalue -= 10;
                            else
                                return true;
                            break;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            if (yvalue < 100)
                                yvalue += 10;
                            else
                                return true;
                            break;
                        case KeyEvent.KEYCODE_BACK:
                            return false;
                        default:
                            dlg.dismiss();
                            playbackFragment.getActivity().onKeyDown(event.getKeyCode(), event);
                            return true;
                    }
                    mPivotX = (float) xvalue * 0.01f;
                    mPivotY = (float) yvalue * 0.01f;
                    setScale();
                    mDialog.setMessage(getPivotMessage());
                    return true;
                }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                }
        );
    }

    String getPivotMessage() {
        if (mScale == 1.0f && mAspect == 1.0f)
            return playbackFragment.getString(R.string.msg_cannot_move);
        int x = Math.round(mPivotX * 100.0f);
        int y = Math.round(mPivotY * 100.0f);
        StringBuilder build = new StringBuilder(playbackFragment.getString(R.string.msg_pic_pos));
        build.append(" ");
        if (x == 50)
            build.append(playbackFragment.getString(R.string.msg_pin_center))
                    .append(" / ");
        else if (x == 0)
            build.append(playbackFragment.getString(R.string.msg_pin_left_edge))
                    .append(" / ");
        else if (x < 50)
            build.append(playbackFragment.getString(R.string.msg_pin_left))
                    .append(" / ");
//            build.append("Left, ");
        else if (x == 100)
            build.append(playbackFragment.getString(R.string.msg_pin_right_edge))
                    .append(" / ");
//            build.append("Right Edge, ");
        else if (x > 50)
            build.append(playbackFragment.getString(R.string.msg_pin_right))
                    .append(" / ");
//            build.append("Right, ");

        if (y == 50)
            build.append(playbackFragment.getString(R.string.msg_pin_center));
//            build.append("Center.");
        else if (y == 0)
            build.append(playbackFragment.getString(R.string.msg_pin_top));
//            build.append("Top Edge.");
        else if (y < 50)
            build.append(playbackFragment.getString(R.string.msg_pin_up));
//            build.append("Up.");
        else if (y == 100)
            build.append(playbackFragment.getString(R.string.msg_pin_bottom));
//            build.append("Bottom Edge.");
        else if (y > 50)
            build.append(playbackFragment.getString(R.string.msg_pin_down));
//            build.append("Down.");
        return build.toString();
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
