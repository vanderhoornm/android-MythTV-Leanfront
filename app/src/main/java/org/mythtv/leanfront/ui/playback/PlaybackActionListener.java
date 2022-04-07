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
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.PlaybackControlsRow;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackParameters;
import org.mythtv.leanfront.exoplayer2.source.SampleQueue;
import com.google.android.exoplayer2.util.MimeTypes;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Playlist;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.player.VideoPlayerGlue;

import java.util.ArrayList;
import java.util.List;

class PlaybackActionListener implements VideoPlayerGlue.OnActionClickedListener {

    private final PlaybackFragment playbackFragment;
    private Playlist mPlaylist;

    static final float[] STRETCH_VALUES = {0.75f, 0.88f, 1.0f, 1.18f, 1.33f, 1.5f};
    static final float[] SCALE_VALUES = {0.88f, 1.0f, 1.17f, 1.33f, 1.5f};
    private float mScale = 1.0f;
    float mStretch = 1.0f;
    float mPivotX = 0.5f;
    float mPivotY = 0.5f;
    long sampleOffsetUs = 0;
    long priorSampleOffsetUs = 0;
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

    public boolean onMenu() {
        playbackFragment.hideControlsOverlay(false);
        PlaybackControlsRow row =  playbackFragment.mPlayerGlue.getControlsRow();
        ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getPrimaryActionsAdapter();
        List<Object> fullList = new ArrayList<>();
        fullList.addAll(adapter.unmodifiableList());
        adapter = (ArrayObjectAdapter) row.getSecondaryActionsAdapter();
        fullList.addAll(adapter.unmodifiableList());
        ArrayList<String> prompts = new ArrayList<>();
        ArrayList<Action> actions = new ArrayList<>();
        for (Object obj : fullList) {
            if (obj instanceof PlaybackControlsRow.PlayPauseAction
                    || obj instanceof PlaybackControlsRow.FastForwardAction
                    || obj instanceof PlaybackControlsRow.RewindAction)
                continue;
            else if (obj instanceof PlaybackControlsRow.MultiAction) {
                PlaybackControlsRow.MultiAction action
                        = (PlaybackControlsRow.MultiAction) obj;
                if (action.getId() == Video.ACTION_PLAYLIST_PLAY)
                    continue;
                prompts.add(action.getLabel(action.getIndex()));
                actions.add(action);
            }
            else if (obj instanceof Action) {
                Action action = (Action) obj;
                prompts.add(action.getLabel1().toString());
                actions.add(action);
            }
        }
        final ArrayList<Action> finalActions = actions; // needed because used in inner class
        // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        OnActionClickedListener parent = playbackFragment.mPlayerGlue;
        builder
                .setItems(prompts.toArray(new String[0]),
                        new DialogInterface.OnClickListener() {
                            ArrayList<Action> mActions = finalActions;
                            OnActionClickedListener mParent = parent;

                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                if (which < mActions.size()) {
                                    mParent.onActionClicked(mActions.get(which));
                                }
                            }
                        });
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 4;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
        return true;
    }

    @Override
    public void onPlayCompleted(VideoPlayerGlue.MyAction playlistPlayAction) {
        playbackFragment.setBookmark(Video.ACTION_SET_LASTPLAYPOS);
        if (playlistPlayAction.getIndex() == 1) // playlist selected
            onNext();
        else if (playbackFragment.mIsBounded) {
            Log.i(TAG, CLASS + " onPlayCompleted checking File Length.");
            playbackFragment.mIsPlayResumable = true;
            playbackFragment.getFileLength(true);
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
        float xScale = mScale * mStretch;
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
        showStretchSelector();
    }

    @Override
    public void onCaption() {
        // This code could be used for rotating among captions instead of showing a list
        // playbackFragment.mTextSelection = playbackFragment.trackSelector(C.TRACK_TYPE_TEXT, playbackFragment.mTextSelection,
        //         R.string.msg_subtitle_on, R.string.msg_subtitle_off, true, true);
        showCaptionSelector();
    }

    private void showCaptionSelector () {
        playbackFragment.hideControlsOverlay(false);
        PlaybackFragment.TrackInfo tracks
                = new PlaybackFragment.TrackInfo(playbackFragment,C.TRACK_TYPE_TEXT);

        ArrayList<String> prompts = new ArrayList<>();
        ArrayList<Integer> actions = new ArrayList<>();
        for (int ix = 0; ix < tracks.trackList.size(); ix++) {
            PlaybackFragment.TrackEntry entry = tracks.trackList.get(ix);
            prompts.add(entry.description);
            actions.add(ix);
        }
        prompts.add(playbackFragment.getString(R.string.msg_subtitle_off));
        actions.add(-1);

        final ArrayList<Integer> finalActions = actions; // needed because used in inner class
        // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder
                .setTitle(R.string.title_select_caption)
                .setItems(prompts.toArray(new String[0]),
                        new DialogInterface.OnClickListener() {
                            ArrayList<Integer> mActions = finalActions;

                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                if (which < mActions.size()) {
                                    playbackFragment.trackSelector
                                        (C.TRACK_TYPE_TEXT, mActions.get(which),
                                         R.string.msg_subtitle_on, R.string.msg_subtitle_off,true,false);
                                }
                            }
                        });
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 4;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
    }

    @Override
    public void onAudioTrack() {
        // This code could be used for rotating among audio tracks instead of showing a list
        // playbackFragment.mAudioSelection = playbackFragment.trackSelector(C.TRACK_TYPE_AUDIO, playbackFragment.mAudioSelection,
        //         R.string.msg_audio_track, R.string.msg_audio_track_off, true, true);
        showAudioSelector();
    }

    private void showAudioSelector () {
        playbackFragment.hideControlsOverlay(false);
        PlaybackFragment.TrackInfo tracks
                = new PlaybackFragment.TrackInfo(playbackFragment,C.TRACK_TYPE_AUDIO);

        ArrayList<String> prompts = new ArrayList<>();
        ArrayList<Integer> actions = new ArrayList<>();
        for (int ix = 0; ix < tracks.trackList.size(); ix++) {
            PlaybackFragment.TrackEntry entry = tracks.trackList.get(ix);
            prompts.add(entry.description);
            actions.add(ix);
        }
        prompts.add(playbackFragment.getString(R.string.msg_audio_track_off));
        actions.add(-1);

        final ArrayList<Integer> finalActions = actions; // needed because used in inner class
        // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder
                .setTitle(R.string.title_select_audio)
                .setItems(prompts.toArray(new String[0]),
                        new DialogInterface.OnClickListener() {
                            ArrayList<Integer> mActions = finalActions;

                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                if (which < mActions.size()) {
                                    playbackFragment.trackSelector
                                            (C.TRACK_TYPE_AUDIO, mActions.get(which),
                                                    R.string.msg_audio_track, R.string.msg_audio_track_off,true,false);
                                }
                            }
                        });
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 4;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
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
        playbackFragment.hideControlsOverlay(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_speed)
                .setView(R.layout.leanback_preference_widget_seekbar);
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 2;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
        SeekBar seekBar = mDialog.findViewById(R.id.seekbar);
        seekBar.setMax(800);
        seekBar.setProgress(Math.round(playbackFragment.mSpeed * 100.0f));
        TextView seekValue = mDialog.findViewById(R.id.seekbar_value);
        seekValue.setText( (int)(playbackFragment.mSpeed * 100.0f) + "%");
        mDialog.setOnKeyListener(
            (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        if (event.getAction() == KeyEvent.ACTION_UP)
                            dlg.dismiss();
                        return true;
                }
                if (event.getAction() != KeyEvent.ACTION_DOWN)
                    return false;
                int value = seekBar.getProgress();
                switch(keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        if (value > 10)
                            value -= 10;
                        else
                            return true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_UP:
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
                return true;
            }
        );
        seekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                    value = value / 10 * 10;
                    if (value < 10)
                        value = 10;
                    seekValue.setText(value + "%");
                    playbackFragment.mSpeed = (float) value * 0.01f;
                    PlaybackParameters parms = new PlaybackParameters(playbackFragment.mSpeed);
                    playbackFragment.mPlayer.setPlaybackParameters(parms);
                }
               @Override
               public void onStartTrackingTouch(SeekBar seekBar) {  }
               @Override
               public void onStopTrackingTouch(SeekBar seekBar) {  }
            }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                    playbackFragment.hideNavigation();
                }
        );
    }

    private void showZoomSelector() {
        playbackFragment.hideControlsOverlay(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_zoom)
                .setView(R.layout.leanback_preference_widget_seekbar);
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 2;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
        SeekBar seekBar = mDialog.findViewById(R.id.seekbar);
        seekBar.setMax(200);
        seekBar.setProgress(Math.round(mScale * 100.0f));
        TextView summary = mDialog.findViewById(android.R.id.summary);
        summary.setText( playbackFragment.getString(R.string.seekbar_instructions));
        TextView seekValue = mDialog.findViewById(R.id.seekbar_value);
        seekValue.setText( (int)(mScale * 100.0f) + "%");
        mDialog.setOnKeyListener(
                (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            if (event.getAction() == KeyEvent.ACTION_UP)
                                dlg.dismiss();
                            return true;
                    }
                    if (event.getAction() != KeyEvent.ACTION_DOWN)
                        return false;
                    int value = Math.round((float)seekBar.getProgress() / 5.0f) * 5;
                    float newfvalue = 0.0f;
                    switch(keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                        case KeyEvent.KEYCODE_ZOOM_OUT:
                            if (value > 5)
                                value -= 5;
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                        case KeyEvent.KEYCODE_ZOOM_IN:
                            if (value <= 195)
                                value += 5;
                            break;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            newfvalue = SCALE_VALUES [0];
                            for (float ftest : SCALE_VALUES) {
                                if (ftest < mScale)
                                    newfvalue = ftest;
                            }
                            value = Math.round(newfvalue * 100.0f);
                            break;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            newfvalue = SCALE_VALUES [SCALE_VALUES.length-1];
                            for (float ftest : SCALE_VALUES) {
                                if (ftest > mScale) {
                                    newfvalue = ftest;
                                    break;
                                }
                            }
                            value = Math.round(newfvalue * 100.0f);
                            break;

                        case KeyEvent.KEYCODE_BACK:
                            return false;
                        default:
                            dlg.dismiss();
                            playbackFragment.getActivity().onKeyDown(event.getKeyCode(), event);
                            return true;
                    }
                    seekBar.setProgress(value);
                    return true;
                }
        );
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                        value = value / 5 * 5;
                        seekValue.setText(value + "%");
                        mScale = (float) value * 0.01f;
                        setScale();
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {  }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {  }
                }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                    playbackFragment.hideNavigation();
                }
        );
    }

    private void showStretchSelector() {
        playbackFragment.hideControlsOverlay(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_stretch)
                .setView(R.layout.leanback_preference_widget_seekbar);
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 2;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
        SeekBar seekBar = mDialog.findViewById(R.id.seekbar);
        seekBar.setMax(200);
        seekBar.setProgress(Math.round(mStretch * 100.0f));
        TextView summary = mDialog.findViewById(android.R.id.summary);
        summary.setText( playbackFragment.getString(R.string.seekbar_instructions));
        TextView seekValue = mDialog.findViewById(R.id.seekbar_value);
        seekValue.setText( (int)(mStretch * 100.0f) + "%");
        mDialog.setOnKeyListener(
                (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            if (event.getAction() == KeyEvent.ACTION_UP)
                                dlg.dismiss();
                            return true;
                    }
                    if (event.getAction() != KeyEvent.ACTION_DOWN)
                        return false;
                    int value = Math.round((float)seekBar.getProgress() / 5.0f) * 5;
                    float newfvalue = 0.0f;
                    switch(keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            if (value > 5)
                                value -= 5;
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            if (value <= 195)
                                value += 5;
                            break;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            newfvalue = STRETCH_VALUES [0];
                            for (float ftest : STRETCH_VALUES) {
                                if (ftest < mStretch)
                                    newfvalue = ftest;
                            }
                            value = Math.round(newfvalue * 100.0f);
                            break;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            newfvalue = STRETCH_VALUES [STRETCH_VALUES.length-1];
                            for (float ftest : STRETCH_VALUES) {
                                if (ftest > mStretch) {
                                    newfvalue = ftest;
                                    break;
                                }
                            }
                            value = Math.round(newfvalue * 100.0f);
                            break;
                        case KeyEvent.KEYCODE_BACK:
                            return false;
                        default:
                            dlg.dismiss();
                            playbackFragment.getActivity().onKeyDown(event.getKeyCode(), event);
                            return true;
                    }
                    seekBar.setProgress(value);
                    return true;
                }
        );
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                        value = value / 5 * 5;
                        seekValue.setText(value + "%");
                        mStretch = (float) value * 0.01f;
                        setScale();
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {  }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {  }
                }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                    playbackFragment.hideNavigation();
                }
        );
    }


    private void showPivotSelector() {
        playbackFragment.hideControlsOverlay(true);
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
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 2;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
        mDialog.setOnKeyListener(
                (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            if (event.getAction() == KeyEvent.ACTION_UP)
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
                    playbackFragment.hideNavigation();
                }
        );
    }

    String getPivotMessage() {
        if (mScale == 1.0f && mStretch == 1.0f)
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
        else if (x == 100)
            build.append(playbackFragment.getString(R.string.msg_pin_right_edge))
                    .append(" / ");
        else if (x > 50)
            build.append(playbackFragment.getString(R.string.msg_pin_right))
                    .append(" / ");

        if (y == 50)
            build.append(playbackFragment.getString(R.string.msg_pin_center));
        else if (y == 0)
            build.append(playbackFragment.getString(R.string.msg_pin_top));
        else if (y < 50)
            build.append(playbackFragment.getString(R.string.msg_pin_up));
        else if (y == 100)
            build.append(playbackFragment.getString(R.string.msg_pin_bottom));
        else if (y > 50)
            build.append(playbackFragment.getString(R.string.msg_pin_down));
        return build.toString();
    }

    @Override
    public void onAudioSync() {
        showAudioSyncSelector();
    }

    private void showAudioSyncSelector() {
        playbackFragment.hideControlsOverlay(true);
        playbackFragment.mPlayerGlue.setEnableControls(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(playbackFragment.getContext(),
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.title_select_audiosync)
                .setView(R.layout.leanback_preference_widget_seekbar);
        mDialog = builder.create();
        mDialog.show();
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        lp.x=0;
        lp.y=0;
        lp.width= Resources.getSystem().getDisplayMetrics().widthPixels / 2;
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(100,0,0,0)));
        SeekBar seekBar = mDialog.findViewById(R.id.seekbar);
        seekBar.setMax(5000); // --2500ms to +2500ms
        seekBar.setProgress((int)(sampleOffsetUs/1000 + 2500));
        TextView seekValue = mDialog.findViewById(R.id.seekbar_value);
        String text = String.format("%+d",sampleOffsetUs / 1000);
        seekValue.setText(text);
        mDialog.setOnKeyListener(
                (DialogInterface dlg, int keyCode, KeyEvent event) -> {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            if (event.getAction() == KeyEvent.ACTION_UP)
                                dlg.dismiss();
                            return true;
                    }
                    if (event.getAction() != KeyEvent.ACTION_DOWN)
                        return false;
                    int value = (int)(sampleOffsetUs/1000 + 2500);
                    switch(keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            if (value > 10)
                                value -= 10;
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                        case KeyEvent.KEYCODE_DPAD_UP:
                            if (value <= 4990)
                                value += 10;
                            break;
                        case KeyEvent.KEYCODE_BACK:
                            return false;
                        default:
                            dlg.dismiss();
                            playbackFragment.getActivity().onKeyDown(event.getKeyCode(), event);
                            return true;
                    }
                    seekBar.setProgress(value);
                    return true;
                }
        );
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                        value = value / 10 * 10;
                        sampleOffsetUs = ((long)value - 2500) * 1000;
                        String text1 = String.format("%+d",sampleOffsetUs / 1000);
                        seekValue.setText(text1);
                        setAudioSync();
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {  }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {  }
                }
        );
        mDialog.setOnDismissListener(
                (DialogInterface dialog) -> {
                    mDialog = null;
                    playbackFragment.hideNavigation();
                    playbackFragment.mPlayerGlue.setEnableControls(true);
                }
        );
    }

    public void setAudioSync() {
        boolean found = false;
        SampleQueue[] sampleQueues = playbackFragment.mMediaSource.getSampleQueues();
        for (SampleQueue sampleQueue : sampleQueues) {
            if (MimeTypes.isAudio(sampleQueue.getUpstreamFormat().sampleMimeType)) {
                sampleQueue.setSampleOffsetUs(sampleOffsetUs);
                found = true;
            }
        }
        if (found) {
            // This check is needed to prevent it continually resetting, because
            // this routine is called again 5 seconds after doing the moveBackward
            if (priorSampleOffsetUs != sampleOffsetUs)
                playbackFragment.moveBackward(0);
            priorSampleOffsetUs = sampleOffsetUs;
        }
    }

    @Override
    public void onBookmark() {
        playbackFragment.setBookmark(Video.ACTION_SET_BOOKMARK);
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

    // Gestures
    public boolean onTap() {
        if (mDialog == null) {
            playbackFragment.tickle();
            return true;
        }
        return false;
    }

    public boolean onDoubleTap() {
        if (mDialog == null) {
            playbackFragment.hideControlsOverlay(true);
            return true;
        }
        return false;
    }

}
