/*
 * Copyright (c) 2017 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.widget.Action;
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
import org.mythtv.leanfront.data.MythHttpDataSource;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.Playlist;
import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.player.MyExtractorsFactory;
import org.mythtv.leanfront.player.VideoPlayerGlue;
import org.mythtv.leanfront.presenter.CardPresenter;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.ssa.SsaDecoder;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Plays selected video, loads playlist and related videos, and delegates playback to {@link
 * VideoPlayerGlue}.
 */
public class PlaybackFragment extends VideoSupportFragment
        implements AsyncBackendCall.OnBackendCallListener {

    private static final int UPDATE_DELAY = 16;

    private VideoPlayerGlue mPlayerGlue;
    private LeanbackPlayerAdapter mPlayerAdapter;
    private SimpleExoPlayer mPlayer;
    private DefaultTrackSelector mTrackSelector;
    private PlaylistActionListener mPlaylistActionListener;
    private PlayerEventListener mPlayerEventListener;

    private Video mVideo;
    private Playlist mPlaylist;
    private VideoLoaderCallbacks mVideoLoaderCallbacks;
    private CursorObjectAdapter mVideoCursorAdapter;
    private long mBookmark = 0;
    private boolean mWatched = false;
    private static final float[] ASPECT_VALUES = {1.0f, 1.1847f, 1.333333f, 1.5f, 0.75f, 0.875f};
    private int mAspectIndex = 0;
    private float mAspect = 1.0f;
    private static final float[] SCALE_VALUES = {0.875f, 1.0f, 1.166666f, 1.333333f, 1.5f};
    private static final int SCALE_DEFAULT_INDEX = 1;
    private int mScaleIndex = SCALE_DEFAULT_INDEX;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private static float[] PIVOTY_VALUES = {0.5f, 0.25f, 0.0f, 1.0f, 0.75f};
    private int mPivotYIndex = 0;
    private float mPivotY = 0.5f;
    private static final float[] SPEED_VALUES = {0.5f, 0.75f, 0.9f, 1.0f,
            1.1f, 1.25f, 1.5f, 1.75f,
            2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
    private static final int SPEED_1_INDEX = 3;
    private int mSpeedIndex = SPEED_1_INDEX;
    private float mSpeed = SPEED_VALUES[SPEED_1_INDEX];
    private Toast mToast = null;
    private SubtitleView mSubtitles;
    // for these selections, -2 = default, -1 = disabled,
    // 0 or above = enabled track number
    private int mTextSelection = -2;
    private int mAudioSelection = -2;
    private long mFileLength = -1;
    private MythHttpDataSource.Factory mDsFactory;
    private MediaSource mMediaSource;
    private MythHttpDataSource mDataSource;
    // Bounded indicates we have a fixed file length
    private boolean mIsBounded = true;
    private long mOffsetBytes = 0;
    private boolean mIsPlayResumable;
    private boolean mIsSpeedChangeConfirmed = false;
    private ScheduledFuture<?> mSchedCheckSpeed;
    // Settings
    private int mSkipFwd = 1000 * Settings.getInt("pref_skip_fwd");
    private int mSkipBack = 1000 * Settings.getInt("pref_skip_back");
    private int mJump = 60000 * Settings.getInt("pref_jump");
    private String mAudio = Settings.getString("pref_audio");
    private boolean mFrameMatch = "true".equals(Settings.getString("pref_framerate_match"));

    SsaDecoder ssadec;
    private View mFocusView;
    private Action mCurrentAction;
    private long mRecordid = -1;

    private static final String TAG = "lfe";
    private static final String CLASS = "PlaybackFragment";

    private XmlNode mStreamInfo = null;
    private Dialog mRateBanner = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideo = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        mBookmark = getActivity().getIntent().getLongExtra(VideoDetailsActivity.BOOKMARK, 0);
        mRecordid = getActivity().getIntent().getLongExtra(VideoDetailsActivity.RECORDID, -1);
        mPlaylist = new Playlist();
        mWatched = (Integer.parseInt(mVideo.progflags, 10) & Video.FL_WATCHED) != 0;

        // For live TV start off as unbounded
        if (mRecordid >= 0)
            mIsBounded = false;

        mVideoLoaderCallbacks = new VideoLoaderCallbacks(mPlaylist);

        // Loads the playlist.
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_TITLE, mVideo.title);
        args.putInt(VideoContract.VideoEntry.COLUMN_RECTYPE, mVideo.rectype);
        args.putString(VideoContract.VideoEntry.COLUMN_RECGROUP, mVideo.recGroup);
        args.putString(VideoContract.VideoEntry.COLUMN_FILENAME, mVideo.filename);

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

    /**
     * Pauses the player.
     */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();

        if (mPlayerGlue != null && mPlayerGlue.isPlaying()) {
            mPlayerGlue.pause();
        }
        if (mRecordid >= 0) {
            // Terminate Live TV
            new AsyncBackendCall(mVideo, mRecordid, false,
                    null).execute(
                    Video.ACTION_STOP_RECORDING,
                    Video.ACTION_REMOVE_RECORD_RULE);
        }
        else
            setBookmark();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    private void setBookmark() {
        long pos = mPlayerGlue.getCurrentPosition();
        long leng = mPlayerGlue.myGetDuration();
        if (leng == -1 || (pos > 5000 && pos < (leng - 500)))
            mBookmark = pos;
        else
            mBookmark = 0;
        new AsyncBackendCall(mVideo, mBookmark, mWatched,
                null).execute(Video.ACTION_SET_BOOKMARK);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    boolean canEnd() {
        if (mRecordid >= 0) {
                // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                        R.style.Theme_AppCompat_Dialog_Alert);
                builder
                        .setTitle(R.string.title_are_you_sure)
                        .setItems(R.array.prompt_stop_livetv,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // The 'which' argument contains the index position
                                        // of the selected item
                                        // 0 = don't stop, 1 = stop
                                        if (which == 1) {
                                            getActivity().finish();
                                        }
                                    }
                                });
                builder.show();
                return false;
            }
        return true;
    }


    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        Log.i(TAG, CLASS + " Initializing Player for " + mVideo.title + " " + mVideo.videoUrl);
        mTrackSelector = new DefaultTrackSelector(getContext());
//        mTrackSelector = new DefaultTrackSelector();
        DefaultRenderersFactory rFactory = new DefaultRenderersFactory(getContext());
        int extMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
        if ("mediacodec".equals(mAudio))
            extMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        else if ("ffmpeg".equals(mAudio))
            extMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        rFactory.setExtensionRendererMode(extMode);
        rFactory.setEnableDecoderFallback(true);
//        mPlayer = ExoPlayerFactory.newSimpleInstance(getActivity(), rFactory, mTrackSelector);
        SimpleExoPlayer.Builder builder = new SimpleExoPlayer.Builder(getContext(),rFactory);
        builder.setTrackSelector(mTrackSelector);
        mPlayer = builder.build();

        mSubtitles = getActivity().findViewById(R.id.leanback_subtitles);
        Player.TextComponent textComponent = mPlayer.getTextComponent();
        if (textComponent != null && mSubtitles != null)
            textComponent.addTextOutput(mSubtitles);

        mPlayerEventListener = new PlayerEventListener();
        mPlayer.addListener(mPlayerEventListener);

        mPlayerAdapter = new LeanbackPlayerAdapter(getActivity(), mPlayer, UPDATE_DELAY);
        mPlaylistActionListener = new PlaylistActionListener(mPlaylist);
        mPlayerGlue = new VideoPlayerGlue(getActivity(), mPlayerAdapter,
                mPlaylistActionListener, mRecordid < 0);
        mPlayerGlue.setHost(new VideoSupportFragmentGlueHost(this));
        hideControlsOverlay(false);
        play(mVideo);
        ArrayObjectAdapter mRowsAdapter = initializeRelatedVideosRow();
        setAdapter(mRowsAdapter);
        mPlayerGlue.setupSelectedListener();
    }

    private void audioFix() {
        if (MainFragment.executor != null) {
            ScheduledFuture<?> sched;
            sched = MainFragment.executor.schedule(new Runnable() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            // Enable subtitle if necessary
                            if (mTextSelection != -2)
                                mTextSelection = trackSelector(C.TRACK_TYPE_TEXT, mTextSelection,
                                        0, 0, true, false);
                            // change audio track if necessary
                            if (mAudioSelection != -2)
                                mAudioSelection = trackSelector(C.TRACK_TYPE_AUDIO, mAudioSelection,
                                        0, 0, true, false);
                            // This may not be needed with new Exoplayer release
//                            else {
//                                // disable and enable to fix audio sync
//                                enableTrack(C.TRACK_TYPE_AUDIO, false);
//                                try {
//                                    Thread.sleep(100);
//                                } catch (InterruptedException e) {
//                                }
//                                enableTrack(C.TRACK_TYPE_AUDIO, true);
//                            }

                        }
                    });
                }
            }, 5000, TimeUnit.MILLISECONDS);
        }
    }

    private void playWait(int delay) {
        if (MainFragment.executor != null) {
            ScheduledFuture<?> sched;
            sched = MainFragment.executor.schedule(new Runnable() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            play(mVideo);
                        }
                    });
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    // Unused
//    private void enableTrack(int trackType, boolean enable) {
//        MappingTrackSelector.MappedTrackInfo mti = mTrackSelector.getCurrentMappedTrackInfo();
//        if (mti == null)
//            return;
//
//        for (int rendIx = 0 ; rendIx < mti.getRendererCount(); rendIx ++) {
//            if (mti.getRendererType(rendIx) == trackType) {
//                mTrackSelector.setParameters(
//                        mTrackSelector
//                                .buildUponParameters()
//                                .setRendererDisabled(rendIx, !enable)
//                );
//            }
//        }
//    }

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

        mVideo = video;
        if (mFrameMatch && android.os.Build.VERSION.SDK_INT >= 23) {
            mStreamInfo = AsyncBackendCall.getCachedStreamInfo(mVideo.videoUrl);
            // If we do not have the stream info - request it and we will be called again.
            if (mStreamInfo == null) {
                new AsyncBackendCall(mVideo, 0, mWatched,
                        this).execute(Video.ACTION_GET_STREAM_INFO);
                return;
            }
            String modeStr = mStreamInfo.getAttribute("LEANFRONT_MODE");
            int mode = -1;
            if (modeStr == null) {
                mode = setupRefreshRate();
            } else
                mode = Integer.parseInt(modeStr);
            if (mode != -1) {
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Display.Mode currMode = display.getMode();
                if (mode != currMode.getModeId()) {
                    if (mRateBanner == null) {
                        // Display banner
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
                                R.style.Theme_AppCompat);
                        float frameRate = Float.parseFloat(mStreamInfo.getAttribute("LEANFRONT_RATE"));
                        String msg = getContext().getString(R.string.msg_setting_framerate, frameRate);
                        builder.setMessage(msg);
                        mRateBanner = builder.show();
                        TextView messageText = mRateBanner.findViewById(android.R.id.message);
                        messageText.setGravity(Gravity.CENTER);
                        DisplayMetrics metrics = new DisplayMetrics();
                        display.getMetrics(metrics);
                        int height = metrics.heightPixels;
                        int padding = height * 5 / 12;
                        messageText.setPadding(0, padding, 0, padding);
                        mRateBanner.show();
                        // show for 2 seconds
                        playWait(2000);
                        return;
                    }
                    else {
                        // banner has displayed
                        mRateBanner.dismiss();
                        mRateBanner = null;
                        Window window = getActivity().getWindow();
                        WindowManager.LayoutParams params = window.getAttributes();
                        params.preferredDisplayModeId = mode;
                        window.setAttributes(params);
                    }
                }
            }
        }
        if (mIsBounded) {
            mOffsetBytes = 0;
            mPlayerGlue.setOffsetMillis(0);
        }
        Log.i(TAG, CLASS + " Playing offset mSec:" + mPlayerGlue.getOffsetMillis());

        mPlayerGlue.setTitle(video.title);

        StringBuilder subtitle = new StringBuilder();

        // This is to mark unwatched when play starts - does not seem a good idea.
        // possible characters for watched - "👁" "⏿" "👀"
        //        int progflags = Integer.parseInt(video.progflags);
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
        else
            mPlayerGlue.seekTo(100);
        // disable and enable audio to fix sync errors
        audioFix();
        // set desired playback speed
        PlaybackParameters parms = new PlaybackParameters(mSpeed);
        mPlayer.setPlaybackParameters(parms);
        // This makes future seeks faster.
        mPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        mPlayerGlue.playWhenPrepared();
    }

    private int setupRefreshRate() {
        // Setup video frame rate
        float frameRate = 0.0f;
        String fieldOrder = null;
        if (mStreamInfo.getString("Count") == null) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(),
                    getActivity().getString(R.string.msg_unable_get_framerate),
                    Toast.LENGTH_LONG);
            mToast.show();
        } else {
            int count = Integer.parseInt(mStreamInfo.getString("Count"));
            XmlNode streamNode = mStreamInfo.getNode("VideoStreamInfos").getNode("VideoStreamInfo");
            for (int ix = 0; ix < count && streamNode != null; ix++) {
                if ("V".equals(streamNode.getString("CodecType"))) {
                    frameRate = Float.parseFloat(streamNode.getString("FrameRate"));
                    fieldOrder = streamNode.getString("FieldOrder");
                    break;
                }
                streamNode = streamNode.getNextSibling();
            }
        }
        float ratio = 1.0f;
        float desiredRefreshRate = 1.0f;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        if (frameRate > 1.0f) {
            if ("PR".equals(fieldOrder))
                desiredRefreshRate = frameRate;
            else {
                if (frameRate < 35.0f)
                    // assume interlaced, prefer double rate
                    desiredRefreshRate = frameRate * 2;
                else
                    desiredRefreshRate = frameRate;
            }
            ratio = refreshRate / desiredRefreshRate;
            if (ratio > 1.0f)
                ratio = 1.0f / ratio;
        }
        // try to get ratio as close as possible to 1.0.
        int setId = -1;
        float setRate = 0.0f;
        if (ratio < 1.0f) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                float matchedRate = 0.0f;
                float halfRate = 0.0f;
                float dblRate = 0.0f;
                int matchedId = -1;
                int halfId = -1;
                int dblId = -1;
                float diffM = 999.0f;
                float diffH = 999.0f;
                float diffD = 999.0f;
                Display.Mode[] modes = display.getSupportedModes();
                Display.Mode currMode = display.getMode();
                for (Display.Mode mode : modes) {
                    if (mode.getPhysicalHeight() != currMode.getPhysicalHeight()
                            || mode.getPhysicalWidth() != currMode.getPhysicalWidth())
                        continue;
                    float rate = mode.getRefreshRate();
                    ratio = rate / desiredRefreshRate;
                    float diff = Math.abs(ratio - 1.0f);
                    if (diff < diffM) {
                        matchedId = mode.getModeId();
                        matchedRate = rate;
                        diffM = diff;
                    }
                    diff = Math.abs(ratio - 0.5f);
                    if (diff < diffH) {
                        halfId = mode.getModeId();
                        halfRate = rate;
                        diffH = diff;
                    }
                    diff = Math.abs(ratio - 2.0f);
                    if (diff < diffD) {
                        dblId = mode.getModeId();
                        dblRate = rate;
                        diffD = diff;
                    }
                }
                if (matchedId > -1 && diffM < 0.005f) {
                    setId = matchedId;
                    setRate = matchedRate;
                }
                else if (dblId > -1 && diffD < 0.005f) {
                    setId = dblId;
                    setRate = dblRate;
                }
                else if (halfId > -1 && diffH < 0.005f) {
                    setId = halfId;
                    setRate = halfRate;
                }
                String msg = null;
                if (setId == -1)
                    msg = getActivity().getString(R.string.msg_no_good_refresh,frameRate);
                if (msg != null) {
                    if (mToast != null)
                        mToast.cancel();
                    mToast = Toast.makeText(getActivity(),
                            msg,
                            Toast.LENGTH_LONG);
                    mToast.show();
                }
            }

                // This code would support android sdk < 23, however it is not tested
                // and few systems use sdk < 23
//            else {
//                float matchedRate = 0.0f;
//                float halfRate = 0.0f;
//                float dblRate = 0.0f;
//                float diffM = 999.0f;
//                float diffH = 999.0f;
//                float diffD = 999.0f;
//                float[] rates = display.getSupportedRefreshRates();
//                for (float rate : rates) {
//                    ratio = rate / desiredRefreshRate;
//
//                    float diff = Math.abs(ratio - 1.0f);
//                    if (diff < diffM) {
//                        matchedRate = rate;
//                        diffM = diff;
//                    }
//                    diff = Math.abs(ratio - 0.5f);
//                    if (diff < diffH) {
//                        halfRate = rate;
//                        diffH = diff;
//                    }
//                    diff = Math.abs(ratio - 2.0f);
//                    if (diff < diffD) {
//                        dblRate = rate;
//                        diffD = diff;
//                    }
//                }
//                float setRate = 0.0f;
//                if (matchedRate > 0.0f && diffM < 0.005f)
//                    setRate = matchedRate;
//                else if (dblRate > 0.0f && diffD < 0.005f)
//                    setRate = dblRate;
//                else if (halfRate > 0.0f && diffH < 0.005f)
//                    setRate = halfRate;
//
//                String msg = null;
//                if (setRate <= 1.0f)
//                    msg = getActivity().getString(R.string.msg_no_good_refresh,desiredRefreshRate);
//                else if (setRate != refreshRate) {
//                    params.preferredRefreshRate = setRate;
//                    msg = getActivity().getString(R.string.msg_setting_framerate, setRate);
//                }
//                if (msg != null) {
//                    if (mToast != null)
//                        mToast.cancel();
//                    mToast = Toast.makeText(getActivity(),
//                            msg,
//                            Toast.LENGTH_LONG);
//                    mToast.show();
//                }
//                if (setRate != refreshRate) {
//                    window.setAttributes(params);
//                }
//            }
        }
        mStreamInfo.setAttribute("LEANFRONT_MODE",String.valueOf(setId));
        mStreamInfo.setAttribute("LEANFRONT_RATE",String.valueOf(setRate));
        return setId;
    }

    private void prepareMediaForPlaying(Uri mediaSourceUri) {
        mFileLength = -1;
        mIsPlayResumable = false;
        getFileLength();
        String userAgent = Util.getUserAgent(getActivity(), "VideoPlayerGlue");
        mDsFactory = new MythHttpDataSource.Factory(userAgent, this);
        MyExtractorsFactory extFactory = new MyExtractorsFactory();
        ProgressiveMediaSource.Factory pmf = new ProgressiveMediaSource.Factory
                (mDsFactory,
                        extFactory);
        MediaItem item = MediaItem.fromUri(mediaSourceUri);
        mMediaSource = pmf.createMediaSource(item);
        mPlayer.setMediaSource(mMediaSource);
        mPlayer.prepare();
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
        args.putInt(VideoContract.VideoEntry.COLUMN_RECTYPE, mVideo.rectype);
        args.putString(VideoContract.VideoEntry.COLUMN_RECGROUP, mVideo.recGroup);
        args.putString(VideoContract.VideoEntry.COLUMN_FILENAME, mVideo.filename);

        getLoaderManager().initLoader(VideoLoaderCallbacks.RELATED_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        return videoCursorAdapter;
    }

    public void skipToNext() {
        // We do not support this for LiveTV
        if (mRecordid >= 0)
            return;
        Video v = mPlaylist.next();
        if (v != null) {
            setBookmark();
            mBookmark = 0;
            mVideo = v;
            play(mVideo);
        }

    }

    public void skipToPrevious() {
        // We do not support this for LiveTV
        if (mRecordid >= 0)
            return;
        Video v = mPlaylist.previous();
        if (v != null) {
            setBookmark();
            // TODO: Refactor so that we can resume from bookmark
            mBookmark = 0;
            mVideo = v;
            play(mVideo);
        }
    }

    /** Skips backwards 1 minute. */
    public void rewind() {
        long newPosition = mPlayerGlue.getCurrentPosition() - mSkipBack;
        newPosition = (newPosition < 0) ? 0 : newPosition;
        seekTo(newPosition,false);
    }

    /** Skips forward 1 minute. */
    public void fastForward() {
        moveForward(mSkipFwd);
    }

    /** Jumps backwards 5 min. */
    public void jumpBack() {
        long newPosition = mPlayerGlue.getCurrentPosition() - mJump;
        newPosition = (newPosition < 0) ? 0 : newPosition;
        seekTo(newPosition, false);
    }

    /** Jumps forward 5 min. */
    public void jumpForward() {
        moveForward(mJump);
    }

    private void moveForward(int millis) {
        boolean doReset = false;
        boolean resetDone = false;
        if (!mIsBounded) {
            seekTo(-1,true);
            resetDone = true;
        }
        long duration = mPlayerGlue.myGetDuration();
        if (duration > -1) {
            long newPosition = mPlayerGlue.getCurrentPosition() + millis;
            if (newPosition > duration - 1000) {
                newPosition = duration - 1000;
                doReset = true;
            }
            seekTo(newPosition, doReset && !resetDone);
        }
    }

    // set position to -1 for a reset with no seek.
    // set doReset true to refresh file size information.
    // If it is in unbounded state will reset to bounded state
    // regardless of parameters.
    private void seekTo(long position, boolean doReset) {
        long newPosition;
        if (position == -1)
            newPosition = mPlayerGlue.getCurrentPosition();
        else
            newPosition = position;
        if (mIsBounded && !doReset) {
            if (position != -1)
                mPlayerAdapter.seekTo(newPosition);
        }
        else {
            mIsBounded = true;
            mBookmark = newPosition;
            mOffsetBytes = 0;
            mPlayerGlue.setOffsetMillis(0);
            mPlayer.stop(true);
            play(mVideo);
        }
    }

    // trackSelection = current selection. -1 = disabled, -2 = leave as is
    // disable = true : Include disabled in the rotation
    // doChange = true : select a new track, false = leave same track
    // Return = new track selection.

    private int trackSelector(int trackType, int trackSelection,
            int msgOn, int msgOff, boolean disable, boolean doChange) {
        // optionList array - 0 = renderer, 1 = track group, 2 = track
        ArrayList<int[]> optionList = new ArrayList<>();
        ArrayList<Integer> renderList = new ArrayList<>();
        ArrayList<Format> formatList = new ArrayList<>();
        MappingTrackSelector.MappedTrackInfo mti = mTrackSelector.getCurrentMappedTrackInfo();
        boolean isPlaying = mPlayerGlue.isPlaying();
        if (mti == null)
            return -1;
        for (int rendIx = 0 ; rendIx < mti.getRendererCount(); rendIx ++) {
            if (mti.getRendererType(rendIx) == trackType) {
                renderList.add(rendIx);
                TrackGroupArray tga = mti.getTrackGroups(rendIx);
                if (tga != null) {
                    TrackGroup tg = null;
                    for (int tgIx = 0 ; tgIx < tga.length; tgIx++) {
                        tg = tga.get(tgIx);
                        if (tg != null) {
                            for (int trkIx = 0; trkIx < tg.length; trkIx++) {
                                int[] selection = new int[3];
                                // optionList array - 0 = renderer, 1 = track group, 2 = track
                                selection[0] = rendIx;
                                selection[1] = tgIx;
                                selection[2] = trkIx;
                                optionList.add(selection);
                                formatList.add(tg.getFormat(trkIx));
                            }
                        }
                    }
                }
            }
        }
        StringBuilder msg = new StringBuilder();
        if (doChange) {
            if (trackSelection == -2)
                trackSelection = -1;
            if (optionList.size() == 0)
                trackSelection = -1;
            else if (++trackSelection >= optionList.size()) {
                if (disable)
                    trackSelection = -1;
                else
                    trackSelection = 0;
            }
        }
        if (trackSelection >= 0) {
            int [] selection = optionList.get(trackSelection);
            DefaultTrackSelector.SelectionOverride ovr
                    = new DefaultTrackSelector.SelectionOverride(
                    selection[1], selection[2]);

            TrackGroupArray tga = mti.getTrackGroups(selection[0]);
            DefaultTrackSelector.ParametersBuilder parms
                    = mTrackSelector
                    .buildUponParameters()
                    .setSelectionOverride(selection[0], tga, ovr);
            if (disable)
                parms = parms.setRendererDisabled(selection[0], false);
            // This line causes playback to pause when enabling subtitle
            mTrackSelector.setParameters(parms);
            String language = formatList.get(trackSelection).language;
            if (language == null)
                language = new String();
            else {
                Locale locale = new Locale(language);
                String langDesc = locale.getDisplayLanguage();
                language = " (" + langDesc + ")";
            }
            if (msgOn > 0)
                msg.append(getActivity().getString(msgOn,
                        trackSelection+1, language));
        } else if (trackSelection == -1){
            if (optionList.size() > 0) {
                for (int ix = 0; ix < renderList.size(); ix++) {
                    mTrackSelector.setParameters(
                            mTrackSelector
                                    .buildUponParameters()
                                    .setRendererDisabled(renderList.get(ix), true)
                    );
                }
            }
            if (msgOff > 0)
                msg.append(getActivity().getString(msgOff));
        }

        if (msg.length() > 0) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(),
                    msg, Toast.LENGTH_LONG);
            mToast.show();
        }
        // For some reason changing the subtitle pauses playback. This fixes that.
        if (trackType == C.TRACK_TYPE_TEXT && isPlaying) {
            mPlayerGlue.pause();
            mPlayerGlue.play();
        }
        return trackSelection;
    }

    public void actionSelected(Action action) {
        View view = getView();
        TextView text = view.findViewById(R.id.button_selected);
        if (text == null)
            return;
        if (action != null) {
            mCurrentAction = action;
            text.setText(action.getLabel1());
            mFocusView = view.findFocus();
        } else {
            // Called with null when a key is pressed. Will clear help message
            // if we are no longer on the control.
            View newView = view.findFocus();
            if (newView != mFocusView) {
                text.setText("");
                mFocusView = newView;
                mCurrentAction = null;
            }
            // This is to handle buttons that change title, for example
            // play / pause.
            else if (mCurrentAction != null)
                text.setText(mCurrentAction.getLabel1());
        }
    }

    public void markWatched(boolean watched) {
        mWatched = watched;
        new AsyncBackendCall(mVideo, mBookmark, mWatched,
                null).execute(Video.ACTION_SET_WATCHED);
    }

    public void getFileLength() {
        new AsyncBackendCall(mVideo, mFileLength, mWatched,
                this).execute(Video.ACTION_FILELENGTH);
    }

    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        if (getContext() == null)
            return;
        int [] tasks = taskRunner.getTasks();
        switch (tasks[0]) {
            case Video.ACTION_FILELENGTH:
                long fileLength = taskRunner.getFileLength();
                // If file has got bigger, resume with bigger file
                Log.i(TAG, CLASS + " File Length changed from " + mFileLength + " to " + fileLength);
                if (fileLength == -1) {
                    mPlayerEventListener.handlePlayerError(null, R.string.pberror_file_length_fail);
                }
                if (mIsPlayResumable) {
                    if (fileLength > mFileLength) {
                        mFileLength = fileLength;
                        mIsBounded = false;
                        mBookmark = 0;
                        mOffsetBytes = mDataSource.getCurrentPos();
                        mPlayerGlue.setOffsetMillis(mPlayerGlue.getCurrentPosition());
                        Log.i(TAG, CLASS + " Resuming Playback.");
                        play(mVideo);
                        hideControlsOverlay(false);
                    }
                    else Log.i(TAG, CLASS + " Playback ending at EOF.");
                }
                mFileLength = fileLength;
                mIsPlayResumable = false;
                break;
            case Video.ACTION_GET_STREAM_INFO:
                if (mVideo == taskRunner.getVideo())
                    play(mVideo);
                break;
        }
    }

    public void setDataSource(MythHttpDataSource mDataSource) {
        this.mDataSource = mDataSource;
    }

    public void tickle(boolean autohide, boolean showActions) {
        mPlayerGlue.setActions(showActions);
        setControlsOverlayAutoHideEnabled(false);
        showControlsOverlay(true);
        if (autohide)
            setControlsOverlayAutoHideEnabled(true);
    }

    public void setActions(boolean showActions) {
        mPlayerGlue.setActions(showActions);
    }


    @Override
    // Overridden because the default tickle disables the fade timer.
    public void tickle() {
        tickle(false, true);
    }

    public boolean isBounded() {
        return mIsBounded;
    }

    public long getOffsetBytes() {
        return mOffsetBytes;
    }

    public void resetSpeed() {
        mSpeedIndex = SPEED_1_INDEX;
        mPlaylistActionListener.onSpeed(0);
    }

    public boolean isSpeededUp() {
        return mSpeedIndex > SPEED_1_INDEX;
    }

    public PlaylistActionListener getPlaylistActionListener() {
        return mPlaylistActionListener;
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
        StringBuilder msg = new StringBuilder(getActivity().getString(R.string.playback_zoom_size))
                .append(" ").append(vertPerc).append("%");
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(getActivity(),
                msg, Toast.LENGTH_LONG);
        mToast.show();
    }

    private void setScale() {
        SurfaceView view = getSurfaceView();
        int height = view.getHeight();
        view.setPivotY(mPivotY * height);
        view.setScaleX(mScaleX * mAspect);
        view.setScaleY(mScaleY);
    }

    /**
     * Opens the video details page when a related video has been clicked.
     */
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

    /**
     * Loads a playlist with videos from a cursor and also updates the related videos cursor.
     */
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
            int rectype = args.getInt(VideoContract.VideoEntry.COLUMN_RECTYPE, -1);
            String recgroup = args.getString(VideoContract.VideoEntry.COLUMN_RECGROUP);
            String filename = args.getString(VideoContract.VideoEntry.COLUMN_FILENAME);
            if (rectype == VideoContract.VideoEntry.RECTYPE_VIDEO) {
                // Videos
                int pos = filename.lastIndexOf('/');
                String dirname = "";
                if (pos >= 0)
                    dirname = filename.substring(0, pos + 1);
                dirname = dirname + "%";

                String orderby = VideoContract.VideoEntry.COLUMN_FILENAME;
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry.COLUMN_FILENAME + " like ?",
                        new String[]{dirname},
                        orderby);
            } else {
                // Recordings or LiveTV
                String category;
                if (mRecordid < 0) // i.e. LiveTV
                    category = args.getString(VideoContract.VideoEntry.COLUMN_TITLE);
                else
                    category = "X\t";
                String orderby = VideoContract.VideoEntry.COLUMN_TITLE + ","
                        + VideoContract.VideoEntry.COLUMN_AIRDATE + ","
                        + VideoContract.VideoEntry.COLUMN_STARTTIME;
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry.COLUMN_TITLE + " = ?",
                        new String[]{category},
                        orderby);
            }
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
            skipToPrevious();
        }

        @Override
        public void onNext() {
            skipToNext();
        }

        @Override
        public void onPlayCompleted() {
            markWatched(true);
            if (mIsBounded) {
                Log.i(TAG, CLASS + " onPlayCompleted checking File Length.");
                mIsPlayResumable = true;
                getFileLength();
            }
        }

        @Override
        public void onZoom() {
            zoom(0);
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

        @Override
        public void onCaption() {
            mTextSelection = trackSelector(C.TRACK_TYPE_TEXT, mTextSelection,
                    R.string.msg_subtitle_on, R.string.msg_subtitle_off, true, true);
        }

        @Override
        public void onPivot() {
            if (++mPivotYIndex >= PIVOTY_VALUES.length)
                mPivotYIndex = 0;
            mPivotY = PIVOTY_VALUES[mPivotYIndex];
            setScale();
            String msg = getActivity().getResources().getStringArray(R.array.msg_pin_values)[mPivotYIndex];
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(),
                    msg, Toast.LENGTH_LONG);
            mToast.show();
        }

        @Override
        public void onSpeed(int increment) {
            int newix = mSpeedIndex + increment;
            if (newix >= SPEED_VALUES.length || newix < 0) {
                PlaybackParameters playbackParameters = mPlayer.getPlaybackParameters();
                int stretchPerc = Math.round(playbackParameters.speed * 100.0f);
                StringBuilder msg = new StringBuilder(getActivity().getString(R.string.playback_speed))
                        .append(" ").append(stretchPerc).append("%");
                if (mToast != null)
                    mToast.cancel();
                mToast = Toast.makeText(getActivity(),
                        msg, Toast.LENGTH_LONG);
                mToast.show();
                return;
            }
            mSpeedIndex = newix;
            mSpeed = SPEED_VALUES[mSpeedIndex];
            PlaybackParameters parms = new PlaybackParameters(mSpeed);
            mPlayer.setPlaybackParameters(parms);
            mIsSpeedChangeConfirmed = false;
            if (mSchedCheckSpeed != null && !mSchedCheckSpeed.isDone())
                mSchedCheckSpeed.cancel(false);
            if (MainFragment.executor != null) {
                mSchedCheckSpeed = MainFragment.executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                long duration = mPlayerGlue.myGetDuration();
                                if (!mIsSpeedChangeConfirmed &&
                                        (duration > 0 || !mIsBounded)) {
                                    // If we cannot change speed, switch to ffmpeg audio.
                                    if (!"ffmpeg".equals(mAudio)) {
                                        mAudio = "ffmpeg";
                                        mBookmark = mPlayerGlue.getCurrentPosition();
                                        mIsBounded = true;
                                        mOffsetBytes = 0;
                                        mPlayerGlue.setOffsetMillis(0);
                                        mPlayer.stop(true);
                                        initializePlayer();
                                        return;
                                    }
                                    PlaybackParameters playbackParameters = mPlayer.getPlaybackParameters();
                                    if (playbackParameters.speed != 1.0f)
                                        return;
                                    mSpeedIndex = SPEED_1_INDEX;
                                    mSpeed = SPEED_VALUES[mSpeedIndex];
                                    if (mToast != null)
                                        mToast.cancel();
                                    mToast = Toast.makeText(getActivity(),
                                            getActivity().getString(R.string.msg_unable_speed),
                                            Toast.LENGTH_LONG);
                                    mToast.show();
                                }
                            }
                        });
                    }
                } , 1000, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void onRewind() {
            rewind();
        }

        @Override
        public void onFastForward() {
            fastForward();
        }

        @Override
        // unused as we do not have OSD icons for this
        public void onJumpForward() {
            jumpForward();
        }

        @Override
        // unused as we do not have OSD icons for this
        public void onJumpBack() {
            jumpBack();
        }

        @Override
        public void onActionSelected(Action action) {
            actionSelected(action);
        }

        @Override
        public void onAudioTrack() {
            mAudioSelection = trackSelector(C.TRACK_TYPE_AUDIO, mAudioSelection,
                    R.string.msg_audio_track, R.string.msg_audio_track_off, true, true);
        }

    }

    class PlayerEventListener implements Player.EventListener {
        private int mDialogStatus = 0;
        private static final int DIALOG_NONE   = 0;
        private static final int DIALOG_ACTIVE = 1;
        private static final int DIALOG_EXIT   = 2;
        private static final int DIALOG_RETRY  = 3;
        private long mTimeLastError = 0;

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            mIsSpeedChangeConfirmed = true;
            int stretchPerc = Math.round(playbackParameters.speed * 100.0f);
            StringBuilder msg = new StringBuilder(getActivity().getString(R.string.playback_speed))
                    .append(" ").append(stretchPerc).append("%");
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(),
                    msg, Toast.LENGTH_LONG);
            mToast.show();
        }

        @Override
        public void onPlayerError(ExoPlaybackException ex) {
            handlePlayerError(ex, -1);
        }

        private void handlePlayerError(Exception ex, int msgNum) {
            Throwable cause = null;
            if (ex != null)
                Log.e(TAG, CLASS + " Player Error " + mVideo.title + " " + mVideo.videoUrl, ex);
            if (ex != null && ex instanceof ExoPlaybackException) {
                ExoPlaybackException error = (ExoPlaybackException)ex;
                switch (error.type) {
                    case ExoPlaybackException.TYPE_OUT_OF_MEMORY:
                        msgNum = R.string.pberror_out_of_memory;
                        cause = error.getOutOfMemoryError();
                        break;
                    case ExoPlaybackException.TYPE_REMOTE:
                        msgNum = R.string.pberror_remote;
                        cause = null;
                        break;
                    case ExoPlaybackException.TYPE_RENDERER:
                        msgNum = R.string.pberror_renderer;
                        cause = error.getRendererException();
                        break;
                    case ExoPlaybackException.TYPE_SOURCE:
                        msgNum = R.string.pberror_source;
                        cause = error.getSourceException();
                        break;
                    case ExoPlaybackException.TYPE_UNEXPECTED:
                        msgNum = R.string.pberror_unexpected;
                        cause = error.getUnexpectedException();
                        break;
                    default:
                        msgNum = R.string.pberror_default;
                        cause = null;
                        break;
                }
            }

            Context context = getContext();
            if (context != null) {
                StringBuilder msg = new StringBuilder();
                if (msgNum > -1)
                    msg.append(context.getString(msgNum));
                if (ex != null)
                    msg.append("\n").append(ex.getMessage());
                String alertMsg = msg.toString();
                if (cause != null)
                    msg.append("\n").append(cause.getMessage());
                Log.e(TAG, CLASS + " Player Error " + msg);
                // if we are near the end
                long currPos = mPlayerGlue.getSavedCurrentPosition();
                long duration = mPlayerGlue.getSavedDuration();
                boolean failAtStart = duration <= 0 || currPos <= 0;
                boolean failAtEnd = !failAtStart && Math.abs(duration - currPos) < 2000;
                if (mDialogStatus == DIALOG_NONE) {
                    long now = System.currentTimeMillis();
                    // If there has been over 10 seconds since last error report
                    // try to recover from error by playing on.
                    if (failAtEnd
                        || (mTimeLastError < now - 10000 && !failAtStart)) {
                        if (mToast != null)
                            mToast.cancel();
                        mToast = Toast.makeText(getActivity(),
                                getActivity().getString(msgNum),
                                Toast.LENGTH_LONG);
                        mToast.show();
                        // if we are at the end - just end playback
                        if (failAtEnd)
                            markWatched(true);
                        else {
                            // Try to continue playback
                            mBookmark = currPos;
                            play(mVideo);
                        }
                    }
                    else {
                        // More than 1 error per minute or fail at start
                        // Alert message for user
                        // to decide on continuing.
                        AlertDialogListener listener = new AlertDialogListener();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                                R.style.Theme_AppCompat_Dialog_Alert);
                        builder.setTitle(R.string.pberror_title);
                        builder.setMessage(alertMsg);
                        // add a button
                        builder.setPositiveButton(R.string.pberror_button_continue, listener);
                        builder.setNegativeButton(R.string.pberror_button_exit, listener);
                        builder.setOnDismissListener(
                                new DialogInterface.OnDismissListener() {
                                    public void onDismiss(DialogInterface dialog) {
                                        if (mDialogStatus != DIALOG_RETRY)
                                            getActivity().finish();
                                        mDialogStatus = DIALOG_NONE;
                                    }
                                });
                        builder.show();
                        mDialogStatus = DIALOG_ACTIVE;
                    }
                    mTimeLastError = now;
                }
            }
        }

        class AlertDialogListener implements DialogInterface.OnClickListener {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mDialogStatus = DIALOG_RETRY;
                        mBookmark = mPlayerGlue.getSavedCurrentPosition();
                        play(mVideo);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mDialogStatus = DIALOG_EXIT;
                        break;
                }
            }
        }

    }


}
