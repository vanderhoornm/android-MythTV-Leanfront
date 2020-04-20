/*
 * Copyright (c) 2016 The Android Open Source Project
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

package org.mythtv.leanfront.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.mythtv.leanfront.ui.MainActivity;
import org.mythtv.leanfront.ui.MainFragment;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import static org.mythtv.leanfront.data.XmlNode.mythApiUrl;

/**
 * FetchVideoService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchVideoService extends IntentService {
    private static final String TAG = "FetchVideoService";

    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public FetchVideoService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        VideoDbBuilder builder = new VideoDbBuilder(getApplicationContext());

        try {
            long fetchTime = 0;
            do {
                fetchTime = System.currentTimeMillis();

                VideoDbHelper dbh = new VideoDbHelper(this);
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.execSQL("DELETE FROM " + VideoContract.VideoEntry.TABLE_NAME); //delete all rows in a table
                db.close();

                // MythTV recording list URL: http://andromeda:6544/Dvr/GetRecordedList
                // MythTV video list URL: http://andromeda:6544/Video/GetVideoList
                String[] urls = {
                        mythApiUrl(null, "/Dvr/GetRecordedList"),
                        mythApiUrl(null, "/Video/GetVideoList"),
                        mythApiUrl(null, "/Channel/GetChannelInfoList?OnlyVisible=true")};
                for (int i = 0; i < urls.length; i++) {
                    String url = urls[i];
                    if (url != null) {
                        // This call expects recordings to be 0, videos to be 1, channels to be 2
                        List<ContentValues> contentValuesList = builder.fetch(url, i);
                        ContentValues[] downloadedVideoContentValues =
                                contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
                        getApplicationContext().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                                downloadedVideoContentValues);
                    }
                }
                // Repeat if another fetch request came in while we were fetching
            }  while(MainFragment.mFetchTime > fetchTime);
        } catch (IOException | XmlPullParserException e) {
            MainFragment.mFetchTime = 0;
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }

        MainFragment.mLoadNeededTime = System.currentTimeMillis();
        MainActivity.startMainLoader();
    }
}
