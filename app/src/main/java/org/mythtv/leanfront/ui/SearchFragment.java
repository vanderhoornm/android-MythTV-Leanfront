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

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SpeechRecognitionCallback;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.mythtv.leanfront.BuildConfig;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.presenter.CardPresenter;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "lfe";
    private static final String CLASS = "SearchFragment";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private static final int REQUEST_SPEECH = 0x00000010;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;
    private final CursorObjectAdapter mVideoCursorAdapter =
            new CursorObjectAdapter(new CardPresenter());

    private int mSearchLoaderId = 1;
    private boolean mResultsFound = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        setSearchResultProvider(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());
        if (DEBUG) {
            Log.d(TAG, CLASS + " User is initiating a search. Do we have RECORD_AUDIO permission? " +
                hasPermission(Manifest.permission.RECORD_AUDIO));
        }
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            if (DEBUG) {
                Log.d(TAG, CLASS + " Does not have RECORD_AUDIO, using SpeechRecognitionCallback");
            }
            // SpeechRecognitionCallback is not required and if not provided recognition will be
            // handled using internal speech recognizer, in which case you must have RECORD_AUDIO
            // permission
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    try {
                        startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, CLASS + " Cannot find activity for speech recognizer", e);
                    }
                }
            });
        } else if (DEBUG) {
            Log.d(TAG, CLASS + " We DO have RECORD_AUDIO");
        }
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    default:
                        // If recognizer is canceled or failed, keep focus on the search orb
                        if (FINISH_ON_RECOGNIZER_CANCELED) {
                            if (!hasResults()) {
                                if (DEBUG) Log.v(TAG, CLASS + " Voice search canceled");
                                getView().findViewById(R.id.lb_search_bar_speech_orb).requestFocus();
                            }
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        if (DEBUG) Log.i(TAG, CLASS + String.format(" Search text changed: %s", newQuery));
        loadQuery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (DEBUG) Log.i(TAG, CLASS + String.format(" Search text submitted: %s", query));
        loadQuery(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0 && mResultsFound;
    }

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }

    private void loadQuery(String query) {
        if (!TextUtils.isEmpty(query) && !"nil".equals(query)) {
            mQuery = query;
            getLoaderManager().initLoader(mSearchLoaderId++, null, this);
        }
    }

    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_bar).requestFocus();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = mQuery;
        return new CursorLoader(
                getActivity(),
                VideoContract.VideoEntry.CONTENT_URI,
                null, // Return all fields.
                VideoContract.VideoEntry.COLUMN_TITLE + " LIKE ? OR " +
                        VideoContract.VideoEntry.COLUMN_SUBTITLE + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                null // Default sort order
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int titleRes;
        if (cursor == null || cursor.isClosed())
            return;
        if (cursor.moveToFirst()) {
            mResultsFound = true;
            titleRes = R.string.search_results;
        } else {
            mResultsFound = false;
            titleRes = R.string.no_search_results;
        }
        mVideoCursorAdapter.changeCursor(cursor);
        HeaderItem header = new HeaderItem(getString(titleRes, mQuery));
        mRowsAdapter.clear();
        ListRow row = new ListRow(header, mVideoCursorAdapter);
        mRowsAdapter.add(row);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
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
            } else {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
