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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.data.VideoDbHelper;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.presenter.CardPresenter;
import org.mythtv.leanfront.presenter.DetailsDescriptionPresenter;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


/*
 * VideoDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
public class VideoDetailsFragment extends DetailsSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int NO_NOTIFICATION = -1;
    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RESUME = 2;
    private static final int ACTION_DELETE = 3;
    private static final int ACTION_UNDELETE = 4;
    private static final int ACTION_REFRESH = 5;

    // ID for loader that loads related videos.
    private static final int RELATED_VIDEO_LOADER = 1;

    // Parsing results of GetRecorded
    private static final String[] XMLTAGS_RECGROUP = {"Recording","RecGroup"};

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
    private long mBookmark = -1;
    private SparseArrayObjectAdapter mActionsAdapter = null;

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
            new AsyncBackendCall().execute(ACTION_REFRESH);

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
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter(),
                        new MovieDetailsOverviewLogoPresenter());

        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.selected_background));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        // Hook up transition element.
        mHelper = new FullWidthDetailsOverviewSharedElementHelper();
        mHelper.setSharedElementEnterTransition(getActivity(),
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
                    case ACTION_RESUME:
                        bookmark = mBookmark;
                    case ACTION_PLAY:
                        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                        intent.putExtra(VideoDetailsActivity.VIDEO, mSelectedVideo);
                        intent.putExtra(VideoDetailsActivity.BOOKMARK, bookmark);
                        startActivityForResult(intent, ACTION_PLAY);
                        break;
                    case ACTION_DELETE:
                        new AsyncBackendCall().execute(ACTION_DELETE, ACTION_REFRESH);
                        break;
                    case ACTION_UNDELETE:
                        new AsyncBackendCall().execute(ACTION_UNDELETE, ACTION_REFRESH);
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
        if (requestCode == ACTION_PLAY)
            new AsyncBackendCall().execute(ACTION_REFRESH);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case RELATED_VIDEO_LOADER: {
                String category = args.getString(VideoContract.VideoEntry.COLUMN_TITLE);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry.COLUMN_TITLE + " = ?",
                        new String[]{category},
                        null
                );
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
                        null
                );
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
                    new AsyncBackendCall().execute(ACTION_REFRESH);

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
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedVideo);

        RequestOptions options = new RequestOptions()
                .error(R.drawable.default_background)
                .dontAnimate();

        Glide.with(this)
                .asBitmap()
                .load(mSelectedVideo.cardImageUrl +
                    "&time=" + String.valueOf(System.currentTimeMillis()))
                .apply(options)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        row.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }
                });

        mActionsAdapter = new SparseArrayObjectAdapter();
        row.setActionsAdapter(mActionsAdapter);

        mAdapter.add(row);
    }

    private void setupMovieListRow() {
        String subcategories[] = {getString(R.string.related_movies)};

        // Generating related video list.
        String category = mSelectedVideo.title;

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_TITLE, category);
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


    private class AsyncBackendCall extends AsyncTask<Integer, Void, Void> {

        protected Void doInBackground(Integer ... tasks) {
            for (int count = 0; count < tasks.length; count++) {
                int task = tasks[count];
                MainActivity main = MainActivity.getContext();
                switch (task) {
                    case ACTION_REFRESH:
                        mBookmark = 0;
                        try {
                            Context context = MainActivity.getContext();
                            SharedPreferences sharedPreferences
                                    = PreferenceManager.getDefaultSharedPreferences(context);
                            String pref = sharedPreferences.getString("pref_bookmark", "auto");
                            if ("auto".equals(pref) || "mythtv".equals(pref)) {
                                // look for a mythtv bookmark
                                String url = XmlNode.mythApiUrl(
                                        "/Dvr/GetSavedBookmark?OffsetType=duration&RecordedId="
                                                + mSelectedVideo.recordedid);
                                XmlNode bkmrkData = XmlNode.fetch(url, null);
                                mBookmark = Long.parseLong(bkmrkData.getString());
                                // sanity check bookmark - between 0 and 24 hrs.
                                // note -1 means a bookmark but no seek table
                                // older version of service returns garbage value when there is
                                // no seek table.
                                if (mBookmark > 24 * 60 * 60 * 1000 || mBookmark < 0)
                                    mBookmark = -1;
                            }
                            if (mBookmark <= 0 && "auto".equals(pref)
                                    || "local".equals(pref)) {
                                // default to none
                                mBookmark = 0;
                                // Look for a local bookmark
                                VideoDbHelper dbh = new VideoDbHelper(context);
                                SQLiteDatabase db = dbh.getReadableDatabase();

                                // Define a projection that specifies which columns from the database
                                // you will actually use after this query.
                                String[] projection = {
                                        VideoContract.StatusEntry._ID,
                                        VideoContract.StatusEntry.COLUMN_VIDEO_URL,
                                        VideoContract.StatusEntry.COLUMN_LAST_USED,
                                        VideoContract.StatusEntry.COLUMN_BOOKMARK
                                };

                                // Filter results
                                String selection = VideoContract.StatusEntry.COLUMN_VIDEO_URL + " = ?";
                                String[] selectionArgs = {mSelectedVideo.videoUrl};

                                Cursor cursor = db.query(
                                        VideoContract.StatusEntry.TABLE_NAME,   // The table to query
                                        projection,             // The array of columns to return (pass null to get all)
                                        selection,              // The columns for the WHERE clause
                                        selectionArgs,          // The values for the WHERE clause
                                        null,                   // don't group the rows
                                        null,                   // don't filter by row groups
                                        null               // The sort order
                                );

                                // We expect one or zero results, never more than one.
                                if (cursor.moveToNext()) {
                                    int colno = cursor.getColumnIndex(VideoContract.StatusEntry.COLUMN_BOOKMARK);
                                    if (colno >= 0) {
                                        mBookmark = cursor.getLong(colno);
                                    }
                                }
                                cursor.close();
                                db.close();

                            }
                            // Find out rec group
                            String url = XmlNode.mythApiUrl(
                                    "/Dvr/GetRecorded?RecordedId="
                                            + mSelectedVideo.recordedid);
                            XmlNode recorded = XmlNode.fetch(url, null);
                            mSelectedVideo.recGroup = recorded.getString(XMLTAGS_RECGROUP);

                        } catch (IOException | XmlPullParserException e) {
                            mBookmark = 0;
                            e.printStackTrace();
                        }
                        break;
                    case ACTION_DELETE:
                        // Delete recording
                        try {
                            String url = XmlNode.mythApiUrl(
                                    "/Dvr/DeleteRecording?RecordedId="
                                            + mSelectedVideo.recordedid);
                            XmlNode result = XmlNode.fetch(url, "POST");
                            if (main != null)
                                main.getMainFragment().startFetch();
                        } catch (IOException | XmlPullParserException e) {
                            e.printStackTrace();
                        }
                        break;
                    case ACTION_UNDELETE:
                        // UnDelete recording
                        try {
                            String url = XmlNode.mythApiUrl(
                                    "/Dvr/UnDeleteRecording?RecordedId="
                                            + mSelectedVideo.recordedid);
                            XmlNode result = XmlNode.fetch(url, "POST");
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

        protected void onPostExecute(Void result) {
            int i = 0;
            mActionsAdapter.clear();
            if (mBookmark > 0)
                mActionsAdapter.set(++i, new Action(ACTION_RESUME, getResources()
                        .getString(R.string.resume_1),
                        getResources().getString(R.string.resume_2)));
            mActionsAdapter.set(++i, new Action(ACTION_PLAY, getResources()
                    .getString(R.string.play_1),
                    getResources().getString(R.string.play_2)));
            if ("Deleted".equals(mSelectedVideo.recGroup))
                mActionsAdapter.set(++i, new Action(ACTION_UNDELETE, getResources()
                        .getString(R.string.undelete_1),
                        getResources().getString(R.string.undelete_2)));
            else
                mActionsAdapter.set(++i, new Action(ACTION_DELETE, getResources()
                        .getString(R.string.delete_1),
                        getResources().getString(R.string.delete_2)));
        }

    }

}
