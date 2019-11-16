/*
 * Copyright (c) 2016 The Android Open Source Project
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
            // MythTV recording list URL: http://andromeda:6544/Dvr/GetRecordedList
            String url = mythApiUrl("/Dvr/GetRecordedList");
            if (url != null) {
                List<ContentValues> contentValuesList = builder.fetch(url);
                ContentValues[] downloadedVideoContentValues =
                        contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
                VideoDbHelper dbh = new VideoDbHelper(this);
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.execSQL("DELETE FROM " + VideoContract.VideoEntry.TABLE_NAME); //delete all rows in a table
                db.close();
                getApplicationContext().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                        downloadedVideoContentValues);
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }

        MainFragment.mLoadNeededTime = System.currentTimeMillis();
        MainFragment.mFetchTime = System.currentTimeMillis();
        MainActivity.startMainLoader();
    }
}
