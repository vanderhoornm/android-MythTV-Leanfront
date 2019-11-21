/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.AsyncBackendCall;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.model.Playlist;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.player.VideoPlayerGlue;
import org.mythtv.leanfront.presenter.CardPresenter;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


/**
 * Plays selected video, loads playlist and related videos, and delegates playback to {@link
 * VideoPlayerGlue}.
 */
public class PlaybackFragment extends VideoSupportFragment {

    private static final int UPDATE_DELAY = 16;

    private VideoPlayerGlue mPlayerGlue;
    private LeanbackPlayerAdapter mPlayerAdapter;
    private SimpleExoPlayer mPlayer;
    private TrackSelector mTrackSelector;
    private PlaylistActionListener mPlaylistActionListener;

    private Video mVideo;
    private Playlist mPlaylist;
    private VideoLoaderCallbacks mVideoLoaderCallbacks;
    private CursorObjectAdapter mVideoCursorAdapter;
    private long mBookmark = 0;
    private boolean mWatched = false;
    private static float ASPECT_VALUES[] = {1.0f, 1.1847f, 1.333333f, 1.5f, 0.75f, 0.875f};
    private int mAspectIndex = 0;
    private float mAspect = 1.0f;
    private static float SCALE_VALUES[] = {1.0f, 1.166666f, 1.333333f, 1.5f, 0.875f};
    private int mScaleIndex = 0;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private Toast mToast = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideo = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        mBookmark = getActivity().getIntent().getLongExtra(VideoDetailsActivity.BOOKMARK, 0);
        mPlaylist = new Playlist();
        mWatched = (Integer.parseInt(mVideo.progflags, 10) & Video.FL_WATCHED) != 0;

        mVideoLoaderCallbacks = new VideoLoaderCallbacks(mPlaylist);

        // Loads the playlist.
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_TITLE, mVideo.title);
        getLoaderManager()
                .initLoader(VideoLoaderCallbacks.QUEUE_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        mVideoCursorAdapter = setupRelatedVideosCursor();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || mPlayer == null)) {
            initializePlayer();
        }
    }

    /** Pauses the player. */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();

        if (mPlayerGlue != null && mPlayerGlue.isPlaying()) {
            mPlayerGlue.pause();
        }
        setBookmark();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    private void setBookmark() {
        long pos = mPlayerGlue.getCurrentPosition();
        long leng = mPlayerGlue.getDuration();
        if (pos > 5000 && pos < (leng-5000))
            mBookmark = pos;
        else
            mBookmark = 0;
        new AsyncBackendCall(mVideo, mBookmark, mWatched,
                null).execute(Video.ACTION_SET_BOOKMARK);
//        new AsyncBackendCall(mVideo, mBookmark).execute(ACTION_SET_BOOKMARK);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        mPlayer = ExoPlayerFactory.newSimpleInstance(getActivity(), mTrackSelector);
        mPlayerAdapter = new LeanbackPlayerAdapter(getActivity(), mPlayer, UPDATE_DELAY);
        mPlaylistActionListener = new PlaylistActionListener(mPlaylist);
        mPlayerGlue = new VideoPlayerGlue(getActivity(), mPlayerAdapter, mPlaylistActionListener);
        mPlayerGlue.setHost(new VideoSupportFragmentGlueHost(this));
        mPlayerGlue.playWhenPrepared();

        play(mVideo);

        ArrayObjectAdapter mRowsAdapter = initializeRelatedVideosRow();
        setAdapter(mRowsAdapter);
        // Scaling example
//        SurfaceView view = getSurfaceView();
//        view.setScaleX(1.3333f);
//        view.setScaleY(1.3333f);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mTrackSelector = null;
            mPlayerGlue = null;
            mPlayerAdapter = null;
            mPlaylistActionListener = null;
        }
    }

    private void play(Video video) {
        mPlayerGlue.setTitle(video.title);

        StringBuilder subtitle = new StringBuilder();
        int progflags = Integer.parseInt(video.progflags);
        // This is to marl unwatched when play starts - does not seem a good idea.
        // possible characters for watched - "👁" "⏿" "👀"
//        if ((progflags & video.FL_WATCHED) != 0)
//            markWatched(false);
        if (video.season != null && video.season.compareTo("0") > 0) {
            subtitle.append('S').append(video.season).append('E').append(video.episode)
                    .append(' ');
        }
        subtitle.append(video.subtitle);
        mPlayerGlue.setSubtitle(subtitle);
        prepareMediaForPlaying(Uri.parse(video.videoUrl));
        if (mBookmark > 0)
            mPlayerGlue.seekTo(mBookmark);
        mPlayerGlue.play();
    }

    private void prepareMediaForPlaying(Uri mediaSourceUri) {
        String userAgent = Util.getUserAgent(getActivity(), "VideoPlayerGlue");
        ProgressiveMediaSource.Factory pmf = new ProgressiveMediaSource.Factory
                  (new DefaultDataSourceFactory(getActivity(), userAgent),
                   new DefaultExtractorsFactory());
        MediaSource mediaSource = pmf.createMediaSource(mediaSourceUri);
        mPlayer.prepare(mediaSource);
    }

    private ArrayObjectAdapter initializeRelatedVideosRow() {
        /*
         * To add a new row to the mPlayerAdapter and not lose the controls row that is provided by the
         * glue, we need to compose a new row with the controls row and our related videos row.
         *
         * We start by creating a new {@link ClassPresenterSelector}. Then add the controls row from
         * the media player glue, then add the related videos row.
         */
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(
                mPlayerGlue.getControlsRow().getClass(), mPlayerGlue.getPlaybackRowPresenter());
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(presenterSelector);

        rowsAdapter.add(mPlayerGlue.getControlsRow());

        HeaderItem header = new HeaderItem(getString(R.string.related_movies));
        ListRow row = new ListRow(header, mVideoCursorAdapter);
        rowsAdapter.add(row);

        setOnItemViewClickedListener(new ItemViewClickedListener());

        return rowsAdapter;
    }

    private CursorObjectAdapter setupRelatedVideosCursor() {
        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        videoCursorAdapter.setMapper(new VideoCursorMapper());

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_TITLE, mVideo.title);
        getLoaderManager().initLoader(VideoLoaderCallbacks.RELATED_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        return videoCursorAdapter;
    }

    public void skipToNext() {
        mPlayerGlue.next();
    }

    public void skipToPrevious() {
        mPlayerGlue.previous();
    }

    public void rewind() {
        mPlayerGlue.rewind();
    }

    public void fastForward() {
        mPlayerGlue.fastForward();
    }

    public void jumpForward() {
        mPlayerGlue.jumpForward();
    }

    public void jumpBack() {
        mPlayerGlue.jumpBack();
    }

    public void markWatched(boolean watched) {
        mWatched = watched;
        new AsyncBackendCall(mVideo, mBookmark, mWatched,
                null).execute(Video.ACTION_SET_WATCHED);
//        new AsyncBackendCall(mVideo,mBookmark).execute(ACTION_SET_WATCHED);
    }


    public void tickle(boolean arrowFFRew) {
        setControlsOverlayAutoHideEnabled(false);
        showControlsOverlay(true);
        if (arrowFFRew)
            setControlsOverlayAutoHideEnabled(true);
    }

    @Override
    // Overridden because the default tickle disables the fade timer.
    public void tickle() {
        tickle(false);
    }

        /** Opens the video details page when a related video has been clicked. */
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;

                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        getActivity(),
                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                        VideoDetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    /** Loads a playlist with videos from a cursor and also updates the related videos cursor. */
    protected class VideoLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        static final int RELATED_VIDEOS_LOADER = 1;
        static final int QUEUE_VIDEOS_LOADER = 2;

        private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();

        private final Playlist playlist;

        private VideoLoaderCallbacks(Playlist playlist) {
            this.playlist = playlist;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            // When loading related videos or videos for the playlist, query by category.
            String category = args.getString(VideoContract.VideoEntry.COLUMN_TITLE);
            String orderby =  VideoContract.VideoEntry.COLUMN_TITLE + ","
                    +VideoContract.VideoEntry.COLUMN_AIRDATE  + ","
                    +VideoContract.VideoEntry.COLUMN_STARTTIME;
            return new CursorLoader(
                    getActivity(),
                    VideoContract.VideoEntry.CONTENT_URI,
                    null,
                    VideoContract.VideoEntry.COLUMN_TITLE + " = ?",
                    new String[] {category},
                    orderby);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            int id = loader.getId();
            if (id == QUEUE_VIDEOS_LOADER) {
                playlist.clear();
                do {
                    Video video = (Video) mVideoCursorMapper.convert(cursor);

                    // Set the current position to the selected video.
                    if (video.id == mVideo.id) {
                        playlist.setCurrentPosition(playlist.size());
                    }

                    playlist.add(video);

                } while (cursor.moveToNext());
            } else if (id == RELATED_VIDEOS_LOADER) {
                mVideoCursorAdapter.changeCursor(cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mVideoCursorAdapter.changeCursor(null);
        }
    }

    class PlaylistActionListener implements VideoPlayerGlue.OnActionClickedListener {

        private Playlist mPlaylist;

        PlaylistActionListener(Playlist playlist) {
            this.mPlaylist = playlist;
        }

        @Override
        public void onPrevious() {
            Video v = mPlaylist.previous();
            if (v != null) {
                setBookmark();
                // TODO: Refactor so that we can resume from bookmark
                mBookmark = 0;
                mVideo = v;
                play(mVideo);
            }
        }

        @Override
        public void onNext() {
            Video v = mPlaylist.next();
            if (v != null) {
                setBookmark();
                mBookmark = 0;
                mVideo = v;
                play(mVideo);
            }
        }

        @Override
        public void onPlayCompleted() {
            markWatched(true);
        }

        @Override
        public void onZoom() {
            if (++mScaleIndex >= SCALE_VALUES.length)
                mScaleIndex = 0;
            mScaleX = SCALE_VALUES[mScaleIndex];
            mScaleY = SCALE_VALUES[mScaleIndex];
            setScale();

            int vertPerc = Math.round(mScaleY * 100.0f);
            StringBuilder msg = new StringBuilder(getActivity().getString(R.string.playback_zoom_size))
                    .append(" ").append(vertPerc).append("%");
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(),
                    msg, Toast.LENGTH_LONG);
            mToast.show();
        }

        @Override
        public void onAspect() {
            if (++mAspectIndex >= ASPECT_VALUES.length)
                mAspectIndex = 0;
            mAspect = ASPECT_VALUES[mAspectIndex];
            setScale();

            int stretchPerc = Math.round(mAspect * 100.0f);
            StringBuilder msg = new StringBuilder(getActivity().getString(R.string.playback_aspect_stretch))
                    .append(" ").append(stretchPerc).append("%");
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(),
                    msg, Toast.LENGTH_LONG);
            mToast.show();
        }

        private void setScale() {
            SurfaceView view = getSurfaceView();
            view.setScaleX(mScaleX * mAspect);
            view.setScaleY(mScaleY);
        }
    }

}
