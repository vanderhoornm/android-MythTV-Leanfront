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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.AsyncBackendCall;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.presenter.CardPresenter;
import org.mythtv.leanfront.presenter.DetailsDescriptionPresenter;

import java.util.ArrayList;


/*
 * VideoDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
public class VideoDetailsFragment extends DetailsSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        AsyncBackendCall.OnBackendCallListener {

    private static final int NO_NOTIFICATION = -1;

    // ID for loader that loads related videos.
    private static final int RELATED_VIDEO_LOADER = 1;

    // Parsing results of GetRecorded
    private static final String[] XMLTAGS_RECGROUP = {"Recording","RecGroup"};
    private static final String[] XMLTAGS_PROGRAMFLAGS = {"ProgramFlags"};

    // ID for loader that loads the video from global search.
    private int mGlobalSearchVideoId = 2;

    private Video mSelectedVideo;
    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;
    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private CursorObjectAdapter mVideoCursorAdapter;
    private FullWidthDetailsOverviewSharedElementHelper mHelper;
    private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();
    // Bookmark is in millisedonds
    private long mBookmark = 0;
    private SparseArrayObjectAdapter mActionsAdapter = null;
    private DetailsOverviewRow mDetailsOverviewRow = null;
    private boolean mWatched;
    private DetailsDescriptionPresenter mDetailsDescriptionPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();
        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        mVideoCursorAdapter.setMapper(mVideoCursorMapper);

        mSelectedVideo = (Video) getActivity().getIntent()
                .getParcelableExtra(VideoDetailsActivity.VIDEO);

        if (savedInstanceState != null)
            mBookmark = savedInstanceState.getLong("mBookmark");

        if (mSelectedVideo != null) {
            removeNotification(getActivity().getIntent()
                    .getIntExtra(VideoDetailsActivity.NOTIFICATION_ID, NO_NOTIFICATION));
            setupAdapter();
            setupDetailsOverviewRow();
            setupMovieListRow();
            updateBackground(mSelectedVideo.bgImageUrl);
            int progflags = Integer.parseInt(mSelectedVideo.progflags);
            mWatched = ((progflags & Video.FL_WATCHED) != 0);
            new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                    this).execute(Video.ACTION_REFRESH);

            // When a Related Video item is clicked.
            setOnItemViewClickedListener(new ItemViewClickedListener());
        }
    }

    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("mBookmark", mBookmark);
    }

    private void removeNotification(int notificationId) {
        if (notificationId != NO_NOTIFICATION) {
            NotificationManager notificationManager = (NotificationManager) getActivity()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    /**
     * Check if there is a global search intent. If there is, load that video.
     */
    private boolean hasGlobalSearchIntent() {
        Intent intent = getActivity().getIntent();
        String intentAction = intent.getAction();
        String globalSearch = getString(R.string.global_search);

        if (globalSearch.equalsIgnoreCase(intentAction)) {
            Uri intentData = intent.getData();
            String videoId = intentData.getLastPathSegment();

            Bundle args = new Bundle();
            args.putString(VideoContract.VideoEntry._ID, videoId);
            getLoaderManager().initLoader(mGlobalSearchVideoId++, args, this);
            return true;
        }
        return false;
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {
        if (uri == null)
            return;
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(mMetrics.widthPixels, mMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void setupAdapter() {
        mDetailsDescriptionPresenter = new DetailsDescriptionPresenter();
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(mDetailsDescriptionPresenter,
                        new MovieDetailsOverviewLogoPresenter());
        Activity activity = getActivity();
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(activity, R.color.selected_background));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        // Hook up transition element.
        mHelper = new FullWidthDetailsOverviewSharedElementHelper();
        mHelper.setSharedElementEnterTransition(activity,
                VideoDetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(mHelper);
        detailsPresenter.setParticipatingEntranceTransition(false);
        prepareEntranceTransition();

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                int id = (int) action.getId();
                long bookmark = 0;
                switch (id) {
                    case Video.ACTION_PLAY_FROM_BOOKMARK:
                        bookmark = mBookmark;
                    case Video.ACTION_PLAY:
                        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                        intent.putExtra(VideoDetailsActivity.VIDEO, mSelectedVideo);
                        intent.putExtra(VideoDetailsActivity.BOOKMARK, bookmark);
                        startActivityForResult(intent, Video.ACTION_PLAY);
                        break;
                    case Video.ACTION_DELETE:
                        new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                                VideoDetailsFragment.this)
                                .execute(Video.ACTION_REFRESH, Video.ACTION_DELETE, Video.ACTION_REFRESH);
                        break;
                    case Video.ACTION_UNDELETE:
                        new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                                VideoDetailsFragment.this)
                                .execute(Video.ACTION_UNDELETE, Video.ACTION_REFRESH);
                        break;
                    case Video.ACTION_WATCHED:
                    case Video.ACTION_UNWATCHED:
                        mWatched = (id == Video.ACTION_WATCHED);
                        new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                                VideoDetailsFragment.this)
                                .execute(Video.ACTION_SET_WATCHED, Video.ACTION_REFRESH);
                        break;
                    case Video.ACTION_REMOVE_BOOKMARK:
                        mBookmark = 0;
                        new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                                VideoDetailsFragment.this)
                                .execute(Video.ACTION_SET_BOOKMARK, Video.ACTION_REFRESH);
                        break;
                    case Video.ACTION_OTHER:
                        ArrayList<String> prompts = new ArrayList<String>();
                        ArrayList<Action> actions = new ArrayList<Action>();
                        if (mSelectedVideo.recGroup != null) {
                            if ("Deleted".equals(mSelectedVideo.recGroup)) {
                                prompts.add(getString(R.string.menu_undelete));
                                actions.add(new Action(Video.ACTION_UNDELETE));
                            } else {
                                prompts.add(getString(R.string.menu_delete));
                                actions.add(new Action(Video.ACTION_DELETE));
                            }
                        }

                        if (mWatched) {
                            prompts.add(getString(R.string.maneu_mark_unwatched));
                            actions.add(new Action(Video.ACTION_UNWATCHED));
                        } else {
                            prompts.add(getString(R.string.menu_mark_watched));
                            actions.add(new Action(Video.ACTION_WATCHED));
                        }

                        if (mBookmark > 0) {
                            prompts.add(getString(R.string.menu_remove_bookmark));
                            actions.add(new Action(Video.ACTION_REMOVE_BOOKMARK));
                        }

                        // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                                R.style.Theme_AppCompat_Dialog_Alert);
                        OnActionClickedListener parent = this;
                        builder  // If you want a title - .setTitle("Other Actions")
                            .setTitle(mSelectedVideo.title)
                            .setItems(prompts.toArray(new String[0]),
                                new DialogInterface.OnClickListener() {
                                    ArrayList<Action> mActions = actions;
                                    OnActionClickedListener mParent = parent;
                                    public void onClick(DialogInterface dialog, int which) {
                                        // The 'which' argument contains the index position
                                        // of the selected item
                                        if (which < mActions.size()) {
                                            parent.onActionClicked(mActions.get(which));
                                        }
                                    }
                                });
                        builder.show();
                        break;
                    default:
                        Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPresenterSelector = new ClassPresenterSelector();
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }


    public void onActivityResult (int requestCode,
                                  int resultCode,
                                  Intent intent) {
        if (requestCode == Video.ACTION_PLAY)
            new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                    this).execute(Video.ACTION_REFRESH);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case RELATED_VIDEO_LOADER: {
                // When loading related videos or videos for the playlist, query by category.
                String recgroup = args.getString(VideoContract.VideoEntry.COLUMN_RECGROUP);
                String filename = args.getString(VideoContract.VideoEntry.COLUMN_FILENAME);
                if (recgroup == null && filename != null) {
                    // Videos
                    int pos = filename.lastIndexOf('/');
                    String dirname = "";
                    if (pos >= 0)
                        dirname = filename.substring(0,pos+1);
                    dirname = dirname + "%";

                    String orderby = VideoContract.VideoEntry.COLUMN_FILENAME;
                    return new CursorLoader(
                            getActivity(),
                            VideoContract.VideoEntry.CONTENT_URI,
                            null,
                            VideoContract.VideoEntry.COLUMN_FILENAME + " like ?",
                            new String[]{dirname},
                            orderby);
                }
                else {
                    // Recordings
                    String category = args.getString(VideoContract.VideoEntry.COLUMN_TITLE);
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
            default: {
                // Loading video from global search.
                String videoId = args.getString(VideoContract.VideoEntry._ID);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry._ID + " = ?",
                        new String[]{videoId},
                        null);
            }
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToNext()) {
            switch (loader.getId()) {
                case RELATED_VIDEO_LOADER: {
                    mVideoCursorAdapter.changeCursor(cursor);
                    break;
                }
                default: {
                    // Loading video from global search.
                    mSelectedVideo = (Video) mVideoCursorMapper.convert(cursor);

                    setupAdapter();
                    setupDetailsOverviewRow();
                    setupMovieListRow();
                    updateBackground(mSelectedVideo.bgImageUrl);
                    new AsyncBackendCall(mSelectedVideo, mBookmark, mWatched,
                            this).execute(Video.ACTION_REFRESH);
                    // When a Related Video item is clicked.
                    setOnItemViewClickedListener(new ItemViewClickedListener());
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    static class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

        static class ViewHolder extends DetailsOverviewLogoPresenter.ViewHolder {
            public ViewHolder(View view) {
                super(view);
            }

            public FullWidthDetailsOverviewRowPresenter getParentPresenter() {
                return mParentPresenter;
            }

            public FullWidthDetailsOverviewRowPresenter.ViewHolder getParentViewHolder() {
                return mParentViewHolder;
            }
        }

        @Override
        public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);

            Resources res = parent.getResources();
            int width = res.getDimensionPixelSize(R.dimen.detail_thumb_width);
            int height = res.getDimensionPixelSize(R.dimen.detail_thumb_height);
            imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            DetailsOverviewRow row = (DetailsOverviewRow) item;
            ImageView imageView = ((ImageView) viewHolder.view);
            imageView.setImageDrawable(row.getImageDrawable());
            if (isBoundToImage((ViewHolder) viewHolder, row)) {
                MovieDetailsOverviewLogoPresenter.ViewHolder vh =
                        (MovieDetailsOverviewLogoPresenter.ViewHolder) viewHolder;
                vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
            }
        }
    }

    private void setupDetailsOverviewRow() {
        mDetailsOverviewRow = new DetailsOverviewRow(mSelectedVideo);
        Drawable defaultImage = getResources().getDrawable(R.drawable.im_movie, null);

        RequestOptions options = new RequestOptions()
                .error(R.drawable.im_movie)
                .fallback(R.drawable.im_movie)
                .dontAnimate();

        CustomTarget<Bitmap> target = new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mDetailsOverviewRow.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        mDetailsOverviewRow.setImageDrawable(defaultImage);
                    }
                };
        if (mSelectedVideo.cardImageUrl == null)
            Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.im_movie)
                    .apply(options)
                    .into(target);
        else
            Glide.with(this)
                    .asBitmap()
                    .load(mSelectedVideo.cardImageUrl +
                            "&time=" + String.valueOf(System.currentTimeMillis()))
                    .apply(options)
                    .into(target);

        mActionsAdapter = new SparseArrayObjectAdapter();
        mDetailsOverviewRow.setActionsAdapter(mActionsAdapter);

        mAdapter.add(mDetailsOverviewRow);
    }

    private void setupMovieListRow() {
        String subcategories[] = {getString(R.string.related_movies)};

        // Generating related video list.
        String category = mSelectedVideo.title;

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_TITLE, category);
        args.putString(VideoContract.VideoEntry.COLUMN_RECGROUP, mSelectedVideo.recGroup);
        args.putString(VideoContract.VideoEntry.COLUMN_FILENAME, mSelectedVideo.filename);
        getLoaderManager().initLoader(RELATED_VIDEO_LOADER, args, this);

        HeaderItem header = new HeaderItem(0, subcategories[0]);
        mAdapter.add(new ListRow(header, mVideoCursorAdapter));
    }


    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        if (taskRunner == null)
            return;
        mBookmark = taskRunner.getBookmark();
        int progflags = Integer.parseInt(mSelectedVideo.progflags);
        mWatched = ((progflags & Video.FL_WATCHED) != 0);
        mDetailsDescriptionPresenter.setupDescription();
        int i = 0;
        mActionsAdapter.clear();
        if (mBookmark > 0)
            mActionsAdapter.set(++i, new Action(Video.ACTION_PLAY_FROM_BOOKMARK, getResources()
                .getString(R.string.resume_1),
                 getResources().getString(R.string.resume_2)));
        mActionsAdapter.set(++i, new Action(Video.ACTION_PLAY, getResources()
            .getString(R.string.play_1),
             getResources().getString(R.string.play_2)));
        // These add extra if needed buttons
//        if ("Deleted".equals(mSelectedVideo.recGroup))
//            mActionsAdapter.set(++i, new Action(Video.ACTION_UNDELETE, getResources()
//                 .getString(R.string.undelete_1),
//                  getResources().getString(R.string.undelete_2)));
//        else
//            mActionsAdapter.set(++i, new Action(Video.ACTION_DELETE, getResources()
//                   .getString(R.string.delete_1),
//                    getResources().getString(R.string.delete_2)));
        mActionsAdapter.set(++i, new Action(Video.ACTION_OTHER, getResources()
                .getString(R.string.button_other_1),
                getResources().getString(R.string.button_other_2)));
    }
}
