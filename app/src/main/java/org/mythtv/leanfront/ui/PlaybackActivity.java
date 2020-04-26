/*
 * Copyright (c) 2014 The Android Open Source Project
 * Copyright (c) 2019-2020 Peter Bennett
 *
 * Incorporates code from "Android TV Samples"
 * <https://github.com/android/tv-samples>
 * Modified by Peter Bennett
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

package org.mythtv.leanfront.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.SeekBar;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Settings;

/**
 * Loads PlaybackFragment and delegates input from a game controller.
 * <br>
 * For more information on game controller capabilities with leanback, review the
 * <a href="https://developer.android.com/training/game-controllers/controller-input.html">docs</href>.
 */
public class PlaybackActivity extends LeanbackActivity {
    private static final float GAMEPAD_TRIGGER_INTENSITY_ON = 0.5f;
    // Off-condition slightly smaller for button debouncing.
    private static final float GAMEPAD_TRIGGER_INTENSITY_OFF = 0.45f;
    private boolean gamepadTriggerPressed = false;
    private PlaybackFragment mPlaybackFragment;
    private boolean mArrowSkipJump;
    private boolean mJumpEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(getString(R.string.playback_tag));
        if (fragment instanceof PlaybackFragment) {
            mPlaybackFragment = (PlaybackFragment) fragment;
        }
        // Prevent screen saver during playback
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mJumpEnabled = "true".equals(Settings.getString("pref_arrow_jump"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mPlaybackFragment.canEnd())
            super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BUTTON_L1:
                mPlaybackFragment.skipToPrevious();
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                mPlaybackFragment.skipToNext();
                return true;
            case KeyEvent.KEYCODE_BUTTON_L2:
                mPlaybackFragment.rewind();
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                mPlaybackFragment.fastForward();
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                finish();
                return true;
            case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
                mPlaybackFragment.getPlaylistActionListener().onAudioTrack();
                return true;
            case KeyEvent.KEYCODE_CAPTIONS:
                mPlaybackFragment.getPlaylistActionListener().onCaption();
                return true;
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                mPlaybackFragment.tickle();
                mPlaybackFragment.jumpForward();
                return true;
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                mPlaybackFragment.tickle();
                mPlaybackFragment.jumpBack();
                return true;
            case KeyEvent.KEYCODE_TV_ZOOM_MODE:
                mPlaybackFragment.getPlaylistActionListener().onAspect();
                return true;
            case KeyEvent.KEYCODE_ZOOM_IN:
                mPlaybackFragment.zoom(1);
                return true;
            case KeyEvent.KEYCODE_ZOOM_OUT:
                mPlaybackFragment.zoom(-1);
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // This method will handle gamepad events.
        if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > GAMEPAD_TRIGGER_INTENSITY_ON
                && !gamepadTriggerPressed) {
            mPlaybackFragment.rewind();
            gamepadTriggerPressed = true;
        } else if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > GAMEPAD_TRIGGER_INTENSITY_ON
                && !gamepadTriggerPressed) {
            mPlaybackFragment.fastForward();
            gamepadTriggerPressed = true;
        } else if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) < GAMEPAD_TRIGGER_INTENSITY_OFF
                && event.getAxisValue(MotionEvent.AXIS_RTRIGGER) < GAMEPAD_TRIGGER_INTENSITY_OFF) {
            gamepadTriggerPressed = false;
        }
        return super.onGenericMotionEvent(event);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event){
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            boolean isSeekBar = false;
            if (view instanceof SeekBar)
                isSeekBar = true;
            int keycode = event.getKeyCode();

            if (keycode == KeyEvent.KEYCODE_DPAD_CENTER
                || keycode == KeyEvent.KEYCODE_ENTER) {
                boolean wasVisible = mPlaybackFragment.isControlsOverlayVisible();
                if (wasVisible && mArrowSkipJump)
                    wasVisible = false;
                mArrowSkipJump = false;
                mPlaybackFragment.tickle(false,!mArrowSkipJump);
                if (!wasVisible)
                    return true;
            }

            if (keycode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                mPlaybackFragment.tickle();
                mPlaybackFragment.fastForward();
                return true;
            }

            if (keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (mJumpEnabled && !mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowSkipJump = true;
                }
                mPlaybackFragment.tickle(mArrowSkipJump,!mArrowSkipJump);
                if (mArrowSkipJump || isSeekBar) {
                    mPlaybackFragment.fastForward();
                    return true;
                }
            }

            if (keycode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                mPlaybackFragment.tickle();
                mPlaybackFragment.rewind();
                return true;
            }

            if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (mJumpEnabled && !mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowSkipJump = true;
                }
                mPlaybackFragment.tickle(mArrowSkipJump, !mArrowSkipJump);
                if (mArrowSkipJump || isSeekBar) {
                    mPlaybackFragment.rewind();
                    return true;
                }
            }

            if (mJumpEnabled && keycode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowSkipJump = true;
                }
                mPlaybackFragment.tickle(mArrowSkipJump, !mArrowSkipJump);
                if (mArrowSkipJump) {
                    mPlaybackFragment.jumpBack();
                    return true;
                }
            }

            if (mJumpEnabled && keycode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowSkipJump = true;
                }
                mPlaybackFragment.tickle(mArrowSkipJump, !mArrowSkipJump);
                if (mArrowSkipJump) {
                    mPlaybackFragment.jumpForward();
                    return true;
                }
            }
            mArrowSkipJump = false;
            mPlaybackFragment.setActions(true);
        }
        boolean ret = super.dispatchKeyEvent(event);
        mPlaybackFragment.actionSelected(null);
        return ret;
    }

}
