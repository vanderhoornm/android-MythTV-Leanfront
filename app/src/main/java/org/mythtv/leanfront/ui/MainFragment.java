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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.TitleViewAdapter;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.FetchVideoService;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.model.ListItem;
import org.mythtv.leanfront.model.MyHeaderItem;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.presenter.CardPresenter;
import org.mythtv.leanfront.presenter.GridItemPresenter;
import org.mythtv.leanfront.presenter.IconHeaderItemPresenter;
import org.mythtv.leanfront.recommendation.UpdateRecommendationsService;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mCategoryRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 123; // Unique ID for Category Loader.
    private CursorObjectAdapter videoCursorAdapter;
    private int mSelectedRow = -1;
    private int mSelectedItem = -1;
    private int mType;
    public static final String KEY_TYPE = "LEANFRONT_TYPE";
    // Type applicable to main screen
    public static final int TYPE_TOPLEVEL = 1;
    // Types applicable to main screen or row
    public static final int TYPE_RECGROUP = 2;
    // Types applicable to main screen row, or cell
    public static final int TYPE_VIDEODIR = 3;
    // Types applicable to row or cell
    public static final int TYPE_SHOW = 4;
    // Types applicable to cell
    public static final int TYPE_EPISODE = 5;
    public static final int TYPE_VIDEO = 6;
    // Special row type
    public static final int TYPE_SETTINGS = 20;
    public static final String KEY_BASENAME = "LEANFRONT_BASENAME";
    public static final String KEY_ROWNAME = "LEANFRONT_ROWNAME";
    public static final String KEY_ITEMNAME = "LEANFRONT_ITEMNAME";
    // mBase is the current recgroup or directory being displayed.
    private String mBaseName;
    private String mSelectedRowName;
    private int mSelectedRowType;
    private String mSelectedItemName;
    private int mSelectedItemType;

    // Maps a Loader Id to its CursorObjectAdapter.
//    private Map<Integer, CursorObjectAdapter> mVideoCursorAdapters;

    private boolean mFetchStarted = false;
    private boolean mLoadStarted = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
//        mVideoCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);
        mLoadStarted = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mType = intent.getIntExtra(KEY_TYPE,TYPE_TOPLEVEL);
        if (mType != TYPE_TOPLEVEL) {
            mBaseName = intent.getStringExtra(KEY_BASENAME);
        }
        if (savedInstanceState != null) {
            mSelectedRowName = savedInstanceState.getString(KEY_ROWNAME);
            mSelectedItemName = savedInstanceState.getString(KEY_ITEMNAME);
        }
        // TESTING TESTING 2 LINES
        mType = TYPE_RECGROUP;
        mBaseName = "All";
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();
        setupEventListeners();
        prepareEntranceTransition();

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.
        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);

        updateRecommendations();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        // failed attempt to make back button smoother
//        if (isVisible()) {
//            FragmentTransaction ft = getFragmentManager().beginTransaction();
//            ft.hide(this);
//            ft.commit();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (mSelectedRow != -1 && mSelectedItem != -1) {
//            getRowsSupportFragment().setSelectedPosition(mSelectedRow, false,
//                    new ListRowPresenter.SelectItemViewHolderTask(mSelectedItem));
//        }
        startBackgroundTimer();
    }

    @Override
    public void onPause() {
        mSelectedRow = getSelectedPosition();
        ListRow selectedRow = (ListRow)mCategoryRowAdapter.get(mSelectedRow);
        ListItem headerItem = (ListItem)selectedRow.getHeaderItem();
        mSelectedRowName = headerItem.getName();
        mSelectedRowType = headerItem.getItemType();
        ListRowPresenter.ViewHolder selectedViewHolder
                = (ListRowPresenter.ViewHolder) getRowsSupportFragment()
                .getRowViewHolder(mSelectedRow);
        mSelectedItem = selectedViewHolder.getSelectedPosition();
        if (mSelectedItem >= 0) {
            ObjectAdapter itemAdapter = selectedRow.getAdapter();
            mSelectedItemName = ((ListItem) itemAdapter.get(mSelectedItem)).getName();
            mSelectedItemType = ((ListItem) itemAdapter.get(mSelectedItem)).getItemType();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        if (mType == TYPE_TOPLEVEL)
            setBadgeDrawable(
                    getActivity().getResources().getDrawable(R.drawable.mythtv_320x180_icon, null));
//        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
        setTitle(mBaseName);
        showTitle(TitleViewAdapter.FULL_VIEW_VISIBLE);
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void startBackgroundTimer() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY);
    }

    private void updateRecommendations() {
        Intent recommendationIntent = new Intent(getActivity(), UpdateRecommendationsService.class);
        getActivity().startService(recommendationIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        if (id == CATEGORY_LOADER) {
//            return new CursorLoader(
//                    getContext(),
//                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
//                    new String[]{"DISTINCT " + VideoContract.VideoEntry.COLUMN_CATEGORY},
//                    // Only categories
//                    null, // No selection clause
//                    null, // No selection arguments
//                    VideoContract.VideoEntry.COLUMN_CATEGORY + " collate nocase" // Default sort order
//            );
//        } else {
//            // Assume it is for a video.
//            String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);
//
//            // This just creates a CursorLoader that gets all videos.
//            return new CursorLoader(
//                    getContext(),
//                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
//                    null, // Projection to return - null means return all fields
//                    VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?", // Selection clause
//                    new String[]{category},  // Select based on the category id.
//                    VideoContract.VideoEntry.COLUMN_AIRDATE+","+VideoContract.VideoEntry.COLUMN_STARTTIME // Sort order
//            );
        Loader ret = new CursorLoader(
                getContext(),
                VideoContract.VideoEntry.CONTENT_URI, // Table to query
                null, // Projection to return - null means return all fields
                null, // Selection clause
                null,  // Select based on the category id.
                VideoContract.VideoEntry.COLUMN_TITLE + ","
                        +VideoContract.VideoEntry.COLUMN_AIRDATE  + ","
                        +VideoContract.VideoEntry.COLUMN_STARTTIME);
        // Map video results from the database to Video objects.
        videoCursorAdapter =
                new CursorObjectAdapter(new CardPresenter());
        videoCursorAdapter.setMapper(new VideoCursorMapper());
        return ret;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && mLoadStarted) {
            final int loaderId = loader.getId();
            boolean cursorHasData = data.moveToFirst();
            String currentCategory = null;
            if (loaderId == CATEGORY_LOADER) {
                // Every time we have to re-get the category loader, we must re-create the sidebar.
                mCategoryRowAdapter.clear();
                ArrayObjectAdapter objectAdapter = null;
                videoCursorAdapter.changeCursor(data);

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (cursorHasData && !data.isAfterLast()) {

                    int categoryIndex = -1;
                    if (mType == TYPE_RECGROUP) {
                        categoryIndex =
                                data.getColumnIndex(VideoContract.VideoEntry.COLUMN_TITLE);
                        if (!"All".equals(mBaseName)) {
                            int recgroupIndex =
                                    data.getColumnIndex(VideoContract.VideoEntry.COLUMN_RECGROUP);
                            String recgroup = data.getString(recgroupIndex);
                            if (!mBaseName.equals(recgroup)) {
                                data.moveToNext();
                                continue;
                            }
                        }
                    }
                    String category = data.getString(categoryIndex);

                    if (!category.equals(currentCategory)) {
                        // Finish off prior row
                        if (objectAdapter != null) {
                            // Create header for this category.
                            MyHeaderItem header = new MyHeaderItem(currentCategory,
                                    TYPE_SHOW);
                            ListRow row = new ListRow(header, objectAdapter);
                            row.setContentDescription(currentCategory);
                            mCategoryRowAdapter.add(row);
                        }
                        objectAdapter = new ArrayObjectAdapter(new CardPresenter());
                        currentCategory = category;
                        // Create header for this category.
//                        HeaderItem header = new HeaderItem(category);
//                    int videoLoaderId = category.hashCode(); // Create unique int from category.
//                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
//                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
//                        CursorObjectAdapter videoCursorAdapter =
//                                new CursorObjectAdapter(new CardPresenter());
//                        videoCursorAdapter.setMapper(new VideoCursorMapper());
//                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

//                        ListRow row = new ListRow(header, videoCursorAdapter);
//                        mCategoryRowAdapter.add(row);
                    }
                    objectAdapter.add(videoCursorAdapter.get(data.getPosition()));
//                    videoCursorAdapter.changeCursor(data);

//                        // Start loading the videos from the database for a particular category.
//                        Bundle args = new Bundle();
//                        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, category);
//                        mLoaderManager.initLoader(videoLoaderId, args, this);
//                    } else {
//                        ListRow row = new ListRow(header, existingAdapter);
//                        mCategoryRowAdapter.add(row);
//                    }

                    data.moveToNext();
                }
                // Finish off prior row
                if (objectAdapter != null) {
                    // Create header for this category.
                    MyHeaderItem header = new MyHeaderItem(currentCategory,
                            TYPE_RECGROUP);
                    ListRow row = new ListRow(header, objectAdapter);
                    mCategoryRowAdapter.add(row);
                }

                // Create a row for this special case with more samples.
                MyHeaderItem gridHeader = new MyHeaderItem(getString(R.string.personal_settings),
                        TYPE_SETTINGS);
                GridItemPresenter gridPresenter = new GridItemPresenter(this);
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
//                if (cursorHasData)
//                    gridRowAdapter.add(getString(R.string.grid_view));
//                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
//                gridRowAdapter.add(getString(R.string.error_fragment));
//                gridRowAdapter.add(getString(R.string.personal_settings));
                ListRow row = new ListRow(gridHeader, gridRowAdapter);
                mCategoryRowAdapter.add(row);

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.
                mLoadStarted = false;
            }
//            else {
//                // The CursorAdapter contains a Cursor pointing to all videos.
//                mVideoCursorAdapters.get(loaderId).changeCursor(data);
//            }
        }
//        else {
//            // Every time we have to re-get the category loader, we must re-create the sidebar.
//            mCategoryRowAdapter.clear();
//
//        }
//        else {
        if (!mFetchStarted) {
            // Start an Intent to fetch the videos.
            Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
            getActivity().startService(serviceIntent);
            mFetchStarted = true;
            mLoadStarted = false;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int loaderId = loader.getId();
//        if (loaderId != CATEGORY_LOADER) {
//            mVideoCursorAdapters.get(loaderId).changeCursor(null);
//        } else {
        if (loaderId == CATEGORY_LOADER)
            mCategoryRowAdapter.clear();
    }

    private class UpdateBackgroundTask implements Runnable {

        @Override
        public void run() {
            if (mBackgroundURI != null) {
                updateBackground(mBackgroundURI.toString());
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

//            mSelectedRow = getSelectedPosition();
//            ListRowPresenter.ViewHolder selectedRow
//                    = (ListRowPresenter.ViewHolder) getRowsSupportFragment()
//                    .getRowViewHolder(mSelectedRow);
//            mSelectedItem = selectedRow.getSelectedPosition();

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.grid_view))) {
                    Intent intent = new Intent(getActivity(), VerticalGridActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
                    Intent intent = new Intent(getActivity(), GuidedStepActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.error_fragment))) {
                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment)
                            .addToBackStack(null).commit();
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }

        }
    }
}
