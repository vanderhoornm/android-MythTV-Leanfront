/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mythtv.leanfront.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.mythtv.leanfront.R;

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
    private int mArrowStatus = 0;
    private static final int ARROW_NORMAL = 0;
    // Left/right is being used for FF/REW
    private static final int ARROW_LR_FF = 1;
    // Up/Down is being used for FF/REW
    private static final int ARROW_UD_FF = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(getString(R.string.playback_tag));
        if (fragment instanceof PlaybackFragment) {
            mPlaybackFragment = (PlaybackFragment) fragment;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            mPlaybackFragment.skipToNext();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            mPlaybackFragment.skipToPrevious();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            mPlaybackFragment.rewind();
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            mPlaybackFragment.fastForward();
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
            int keycode = event.getKeyCode();

            if (keycode == KeyEvent.KEYCODE_DPAD_CENTER
                || keycode == KeyEvent.KEYCODE_ENTER) {
                boolean wasVisible = mPlaybackFragment.isControlsOverlayVisible();
                mArrowStatus = ARROW_NORMAL;
                mPlaybackFragment.tickle(false);
                if (!wasVisible)
                    return true;
            }

            if (keycode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                mPlaybackFragment.tickle(true);
                mPlaybackFragment.fastForward();
                return true;
            }

            if (keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (!mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowStatus = ARROW_LR_FF;
                }
                mPlaybackFragment.tickle(mArrowStatus != ARROW_NORMAL);
                if (mArrowStatus == ARROW_LR_FF) {
                    mPlaybackFragment.fastForward();
                    return true;
                }
            }

            if (keycode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                mPlaybackFragment.tickle(true);
                mPlaybackFragment.rewind();
                return true;
            }

            if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (!mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowStatus = ARROW_LR_FF;
                }
                mPlaybackFragment.tickle(mArrowStatus != ARROW_NORMAL);
                if (mArrowStatus == ARROW_LR_FF) {
                    mPlaybackFragment.rewind();
                    return true;
                }
            }

            if (keycode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowStatus = ARROW_UD_FF;
                }
                mPlaybackFragment.tickle(mArrowStatus != ARROW_NORMAL);
                if (mArrowStatus == ARROW_UD_FF) {
                    mPlaybackFragment.jumpBack();
                    return true;
                }
            }

            if (keycode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!mPlaybackFragment.isControlsOverlayVisible()) {
                    mArrowStatus = ARROW_UD_FF;
                }
                mPlaybackFragment.tickle(mArrowStatus != ARROW_NORMAL);
                if (mArrowStatus == ARROW_UD_FF) {
                    mPlaybackFragment.jumpForward();
                    return true;
                }
            }
            mArrowStatus = ARROW_NORMAL;
        }
        return super.dispatchKeyEvent(event);
    }

}
