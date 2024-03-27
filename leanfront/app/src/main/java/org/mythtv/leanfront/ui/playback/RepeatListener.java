package org.mythtv.leanfront.ui.playback;

import android.os.Handler;
import android.view.MotionEvent;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
 * click is fired immediately, next one after the initialInterval, and subsequent
 * ones after the normalInterval.
 *
 * <p>Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
 * achieve this.
 */
public class RepeatListener {

    private Handler handler = new Handler();

    private final int initialInterval;
    private final int normalInterval;
    private PlaybackFragment playbackFragment;
    private int direction;
    private boolean active;

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, normalInterval);
            playbackFragment.ffRew(direction);
        }
    };

    /**
     * @param initialInterval The interval after first click event
     * @param normalInterval The interval after second and subsequent click
     *       events
     * @param playbackFragment
     * @param direction -1 for back, +1 for forward.
     */
    public RepeatListener(int initialInterval, int normalInterval,
            PlaybackFragment playbackFragment, int direction) {
        this.initialInterval = initialInterval;
        this.normalInterval = normalInterval;
        this.playbackFragment = playbackFragment;
        this.direction = direction;
    }

    public boolean onTouch(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialInterval);
                playbackFragment.ffRew(direction);
                active = true;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                active = false;
                return true;
        }

        return false;
    }

    public void cancel() {
        if (active) {
            handler.removeCallbacks(handlerRunnable);
            active = false;
        }
    }

}
