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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;

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
import org.mythtv.leanfront.data.MythDataSource;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.data.VideoDbHelper;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import org.mythtv.leanfront.model.Playlist;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.player.VideoPlayerGlue;
import org.mythtv.leanfront.presenter.CardPresenter;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.Date;



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
    private static final int ACTION_SET_BOOKMARK = 1;
    private static final int ACTION_SET_WATCHED = 2;

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

        long pos = mPlayerGlue.getCurrentPosition();
        long leng = mPlayerGlue.getDuration();
        if (pos > 5000 && pos < (leng-5000))
            mBookmark = pos;
        else
            mBookmark = 0;
        new AsyncBackendCall().execute(ACTION_SET_BOOKMARK);

        if (Util.SDK_INT <= 23) {
            releasePlayer();
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
        // possible characters for watched - "👁" "⏿" "👀"
        if ((progflags & video.FL_WATCHED) != 0)
            subtitle.append("\uD83D\uDC41");
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
/*        ProgressiveMediaSource.Factory pmf = new ProgressiveMediaSource.Factory
                (new DefaultDataSourceFactory(getActivity(), userAgent),
                        new DefaultExtractorsFactory()); */
        ProgressiveMediaSource.Factory pmf = new ProgressiveMediaSource.Factory
                (new MythDataSource.Factory(getActivity(), userAgent),
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
        new AsyncBackendCall().execute(ACTION_SET_WATCHED);
    }


    @Override
    // Overridden because the default tickle disables the fade timer.
    public void tickle() {
        setControlsOverlayAutoHideEnabled(false);
        showControlsOverlay(true);
        setControlsOverlayAutoHideEnabled(true);
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
            return new CursorLoader(
                    getActivity(),
                    VideoContract.VideoEntry.CONTENT_URI,
                    null,
                    VideoContract.VideoEntry.COLUMN_TITLE + " = ?",
                    new String[] {category},
                    null);
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
            play(mPlaylist.previous());
        }

        @Override
        public void onNext() {
            play(mPlaylist.next());
        }

        @Override
        public void onPlayCompleted() {
            markWatched(true);
        }
    }
    private class AsyncBackendCall extends AsyncTask<Integer, Void, Void> {

        protected Void doInBackground(Integer ... tasks) {
            for (int counter = 0; counter < tasks.length; counter++) {
                int task = tasks[counter];
                MainActivity main = MainActivity.getContext();
                String result = null;
                String url = null;
                switch (task) {
                    case ACTION_SET_BOOKMARK:
                        try {
                            boolean found = false;
                            SharedPreferences sharedPreferences
                                    = PreferenceManager.getDefaultSharedPreferences(main);
                            String pref = sharedPreferences.getString("pref_bookmark", "mythtv");
                            String fpsStr = sharedPreferences.getString("pref_fps", "30");
                            int fps = 30;
                            try {
                                fps = Integer.parseInt(fpsStr,10);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                fps = 30;
                            }
                            if ("mythtv".equals(pref)) {
                                // store a mythtv bookmark
                                url = XmlNode.mythApiUrl(
                                        "/Dvr/SetSavedBookmark?OffsetType=duration&RecordedId="
                                                + mVideo.recordedid + "&Offset=" + mBookmark);
                                XmlNode bkmrkData = XmlNode.fetch(url, "POST");
                                result = bkmrkData.getString();
                                if ("true".equals(result))
                                    found = true;
                                else {
                                    // store a mythtv position bookmark (in case there is no seek table)
                                    long posBkmark = mBookmark * fps / 1000;
                                    url = XmlNode.mythApiUrl(
                                            "/Dvr/SetSavedBookmark?RecordedId="
                                                    + mVideo.recordedid + "&Offset=" + posBkmark);
                                    bkmrkData = XmlNode.fetch(url, "POST");
                                    result = bkmrkData.getString();
                                    // if this is successfull we still need to set it local as well
                                    // so do not set found to true
//                                    if ("true".equals(result))
//                                        found = true;
                                }
                            }
                            if ("local".equals(pref) || !found) {
                                // Use local bookmark

                                // Gets the data repository in write mode
                                VideoDbHelper dbh = new VideoDbHelper(main);
                                SQLiteDatabase db = dbh.getWritableDatabase();

                                // Create a new map of values, where column names are the keys
                                ContentValues values = new ContentValues();
                                Date now = new Date();
                                values.put(VideoContract.StatusEntry.COLUMN_LAST_USED, now.getTime());
                                values.put(VideoContract.StatusEntry.COLUMN_BOOKMARK, mBookmark);

                                // First try an update
                                String selection = VideoContract.StatusEntry.COLUMN_VIDEO_URL + " = ?";
                                String[] selectionArgs = {mVideo.videoUrl};

                                int count = db.update(
                                        VideoContract.StatusEntry.TABLE_NAME,
                                        values,
                                        selection,
                                        selectionArgs);

                                if (count == 0) {
                                    // Try an insert instead
                                    values.put(VideoContract.StatusEntry.COLUMN_VIDEO_URL, mVideo.videoUrl);
                                    // Insert the new row, returning the primary key value of the new row
                                    long newRowId = db.insert(VideoContract.StatusEntry.TABLE_NAME,
                                            null, values);
                                }
                                db.close();
                            }
                        } catch (IOException | XmlPullParserException e) {
                            e.printStackTrace();
                        }
                        break;
                    case ACTION_SET_WATCHED:
                        try {
                            // set recording watched
                            url = XmlNode.mythApiUrl(
                                    "/Dvr/UpdateRecordedWatchedStatus?RecordedId="
                                            + mVideo.recordedid + "&Watched=" + mWatched);
                            XmlNode resultData = XmlNode.fetch(url, "POST");
                            result = resultData.getString();
                            if (main != null)
                                main.getMainFragment().startFetch();
                        } catch (IOException | XmlPullParserException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
            return null;
        }

    }

}
