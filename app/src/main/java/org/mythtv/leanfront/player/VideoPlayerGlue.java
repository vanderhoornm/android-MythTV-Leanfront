/*
 * Copyright (C) 2017 The Android Open Source Project
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

package org.mythtv.leanfront.player;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Video;

/**
 * Manages customizing the actions in the {@link PlaybackControlsRow}. Adds and manages the
 * following actions to the primary and secondary controls:
 *
 * <ul>
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.RepeatAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.ThumbsDownAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.ThumbsUpAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.SkipNextAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.FastForwardAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.RewindAction}
 * </ul>
 *
 * Note that the superclass, {@link PlaybackTransportControlGlue}, manages the playback controls
 * row.
 */
public class VideoPlayerGlue extends PlaybackTransportControlGlue<LeanbackPlayerAdapter> {

    /** Listens for when skip to next and previous actions have been dispatched. */
    public interface OnActionClickedListener {
        /** Skip to the previous item in the queue. */
        void onPrevious();
        /** Skip to the next item in the queue. */
        void onNext();
        void onPlayCompleted();
        void onZoom();
        void onAspect();
        void onCaption();
        void onPivot();
        void onRewind();
        void onFastForward();
        void onJumpForward();
        void onJumpBack();
        void onSpeed(int increment);
    }

    private final OnActionClickedListener mActionListener;

    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;
    private PlaybackControlsRow.ClosedCaptioningAction mClosedCaptioningAction;
    private PivotAction mPivotAction;
    private ZoomAction mZoomAction;
    private AspectAction mAspectAction;
    private SpeedAction mSpeedDecAction;
    private SpeedAction mSpeedIncAction;
    private boolean mActionsVisible;
    private long mOffsetMillis = 0;

    public VideoPlayerGlue(
            Context context,
            LeanbackPlayerAdapter playerAdapter,
            OnActionClickedListener actionListener) {
        super(context, playerAdapter);

        mActionListener = actionListener;

        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        mRewindAction = new PlaybackControlsRow.RewindAction(context);
        mZoomAction = new ZoomAction(context);
        mAspectAction = new AspectAction(context);
        mSpeedDecAction = new SpeedAction(context, -1);
        mSpeedIncAction = new SpeedAction(context, 1);
        mClosedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(context);
        mPivotAction = new PivotAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        super.onCreatePrimaryActions(adapter);
        adapter.add(mSkipPreviousAction);
        adapter.add(mRewindAction);
        adapter.add(mFastForwardAction);
        adapter.add(mSkipNextAction);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
        super.onCreateSecondaryActions(adapter);
        adapter.add(mClosedCaptioningAction);
        adapter.add(mZoomAction);
        adapter.add(mAspectAction);
        adapter.add(mPivotAction);
        adapter.add(mSpeedDecAction);
        adapter.add(mSpeedIncAction);
        mActionsVisible = true;
    }

    public void setActions(boolean showActions) {
        if (showActions) {
            if (mActionsVisible)
                return;
            PlaybackControlsRow row =  getControlsRow();
            ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getPrimaryActionsAdapter();
            adapter.clear();
            onCreatePrimaryActions(adapter);
            adapter.notifyArrayItemRangeChanged(0,adapter.size());
            adapter = (ArrayObjectAdapter) row.getSecondaryActionsAdapter();
            adapter.clear();
            onCreateSecondaryActions(adapter);
            adapter.notifyArrayItemRangeChanged(0,adapter.size());
            mActionsVisible = true;
            onPlayStateChanged();
        }
        else {
            if (!mActionsVisible)
                return;
            PlaybackControlsRow row =  getControlsRow();
            ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getPrimaryActionsAdapter();
            adapter.clear();
            adapter.notifyArrayItemRangeChanged(0,0);
            adapter = (ArrayObjectAdapter) row.getSecondaryActionsAdapter();
            adapter.clear();
            adapter.notifyArrayItemRangeChanged(0,0);
            mActionsVisible = false;
        }
    }

    @Override
    public void onActionClicked(Action action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action);
            return;
        }
        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action);
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    private boolean shouldDispatchAction(Action action) {
        return action == mRewindAction
                || action == mFastForwardAction
                || action == mClosedCaptioningAction
                || action == mZoomAction
                || action == mAspectAction
                || action == mPivotAction
                || action == mSpeedDecAction
                || action == mSpeedIncAction;
    }

    private void dispatchAction(Action action) {
        // Primary actions are handled manually.
        if (action == mRewindAction) {
            mActionListener.onRewind();
        } else if (action == mFastForwardAction) {
            mActionListener.onFastForward();
        } else if (action == mZoomAction) {
            mActionListener.onZoom();
        } else if (action == mAspectAction) {
            mActionListener.onAspect();
        } else if (action == mClosedCaptioningAction) {
            mActionListener.onCaption();
        } else if (action == mPivotAction) {
            mActionListener.onPivot();
        } else if (action == mSpeedDecAction) {
            mActionListener.onSpeed(-1);
        } else if (action == mSpeedIncAction) {
            mActionListener.onSpeed(1);
        } else if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down
            // and repeat.
            notifyActionChanged(
                    multiAction,
                    (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter());
        }
    }

    private void notifyActionChanged(
            PlaybackControlsRow.MultiAction action, ArrayObjectAdapter adapter) {
        if (adapter != null) {
            int index = adapter.indexOf(action);
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    @Override
    public void next() {
        mActionListener.onNext();
    }

    @Override
    public void previous() {
        mActionListener.onPrevious();
    }

    @Override
    protected void onPlayCompleted() {
        mActionListener.onPlayCompleted();
        super.onPlayCompleted();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return super.onKey(v, keyCode, event);
    }

    static int getIconHighlightColor(Context context) {
        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.playbackControlsIconHighlightColor,
                outValue, true)) {
            return outValue.data;
        }
        return context.getResources().getColor(R.color.lb_playback_icon_highlight_no_theme);
    }

    @Override
    public long getCurrentPosition() {
        return super.getCurrentPosition() + mOffsetMillis;
    }

    @Override
    // This method was copied from PlaybackBaseControlGlue
    // so we can modify the duration
    protected void onUpdateDuration() {
        PlaybackControlsRow controlsRow = getControlsRow();
        LeanbackPlayerAdapter adapter = getPlayerAdapter();
        if (controlsRow != null) {
            controlsRow.setDuration(
                    adapter.isPrepared() ? myGetDuration() : -1);
        }
        else
            // This is to satisfy the @CallSuper annotation
            super.onUpdateDuration();
    }

    public long myGetDuration() {
        long duration = getDuration();
        if (duration >= 0)
            duration += mOffsetMillis;
        return duration;
    }

    public long getOffsetMillis() {
        return mOffsetMillis;
    }

    public void setOffsetMillis(long offsetMillis) {
        this.mOffsetMillis = offsetMillis;
    }

    /**
     * An action for displaying a Zoom icon.
     */
    public static class ZoomAction extends PlaybackControlsRow.MultiAction {

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public ZoomAction(Context context) {
            this(context, getIconHighlightColor(context));
        }

        /**
         * Constructor
         * @param context Context used for loading resources.
         * @param highlightColor Color for the highlighted icon state.
         */
        public ZoomAction(Context context, int highlightColor) {
            super(Video.ACTION_ZOOM);
            Resources res = context.getResources();
            Drawable[] drawables = new Drawable[1];
            drawables[0] = ResourcesCompat.getDrawable(res, R.drawable.ic_zoom_button, null);
            setDrawables(drawables);
        }
    }

    /**
     * An action for displaying an Aspect icon.
     */
    public static class AspectAction extends PlaybackControlsRow.MultiAction {

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public AspectAction(Context context) {
            this(context, getIconHighlightColor(context));
        }

        /**
         * Constructor
         * @param context Context used for loading resources.
         * @param highlightColor Color for the highlighted icon state.
         */
        public AspectAction(Context context, int highlightColor) {
            super(Video.ACTION_ASPECT);
            Resources res = context.getResources();
            Drawable[] drawables = new Drawable[1];
            drawables[0] = ResourcesCompat.getDrawable(res, R.drawable.ic_aspect_button, null);
            setDrawables(drawables);
        }
    }

    /**
     * An action for displaying an Aspect icon.
     */
    public static class PivotAction extends PlaybackControlsRow.MultiAction {

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public PivotAction(Context context) {
            this(context, getIconHighlightColor(context));
        }

        /**
         * Constructor
         * @param context Context used for loading resources.
         * @param highlightColor Color for the highlighted icon state.
         */
        public PivotAction(Context context, int highlightColor) {
            super(Video.ACTION_PIVOT);
            Resources res = context.getResources();
            Drawable[] drawables = new Drawable[1];
            drawables[0] = ResourcesCompat.getDrawable(res, R.drawable.ic_up_down_button, null);
            setDrawables(drawables);
        }
    }

    /**
     * An action for displaying a Speed icon.
     */
    public static class SpeedAction extends PlaybackControlsRow.MultiAction {

        int mIncrement;

        /**
         * Constructor
         * @param context Context used for loading resources.
         * @param increment -1 for down, +1 for up
         */
        public SpeedAction(Context context, int increment) {
            super(Video.ACTION_SPEED);
            this.mIncrement = increment;
            Resources res = context.getResources();
            Drawable[] drawables = new Drawable[1];
            int icon;
            if (mIncrement == -1)
                icon = R.drawable.ic_speed_decrease;
            else
                icon = R.drawable.ic_speed_increase;
            drawables[0] = ResourcesCompat.getDrawable(res, icon, null);
            setDrawables(drawables);
        }
    }


}
