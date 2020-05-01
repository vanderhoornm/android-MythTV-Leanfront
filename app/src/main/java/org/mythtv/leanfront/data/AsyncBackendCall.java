/*
 * Copyright (c) 2019-2020 Peter Bennett
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.ui.MainActivity;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;


public class AsyncBackendCall extends AsyncTask<Integer, Void, Void> {

    public interface OnBackendCallListener {
        default void onPostExecute(AsyncBackendCall taskRunner) {}
    }

    private Video mVideo;
    private long mValue; // Used for bookmark or recordid or file length
    private OnBackendCallListener mBackendCallListener;
    private boolean mWatched;
    private int [] mTasks;
    private long mFileLength = -1;
    private long mRecordId = -1;
    private long mRecordedId = -1;

    // Parsing results of GetRecorded
    private static final String[] XMLTAGS_RECGROUP = {"Recording","RecGroup"};
    private static final String[] XMLTAGS_PROGRAMFLAGS = {"ProgramFlags"};
    private static final String[] XMLTAGS_RECORDID = {"Recording", "RecordId"};
    private static final String[] XMLTAGS_RECORDEDID = {"Recording", "RecordedId"};
    private static final String[] XMLTAGS_ENDTIME = {"Recording", "EndTs"};
    private static final String XMLTAG_WATCHED = "Watched";
    private static final String VALUE_WATCHED = (new Integer(Video.FL_WATCHED)).toString();

    private static final String TAG = "lfe";
    private static final String CLASS = "AsyncBackendCall";

    public AsyncBackendCall(Video videoA, long valueA, boolean watched,
            OnBackendCallListener backendCallListener) {
        mVideo = videoA;
        mValue = valueA;
        mBackendCallListener = backendCallListener;
        mWatched = watched;
    }

    public long getBookmark() {
        return mValue;
    }

    public long getFileLength() {
        return mFileLength;
    }

    public Video getVideo() {
        return mVideo;
    }

    public int[] getTasks() {
        return mTasks;
    }

    public long getRecordId() {
        return mRecordId;
    }

    public long getRecordedId() {
        return mRecordedId;
    }

    protected Void doInBackground(Integer ... tasks) {
        mTasks = new int[tasks.length];
        boolean isRecording = (mVideo.recGroup != null);
        for (int count = 0; count < tasks.length; count++) {
            int task = tasks[count];
            mTasks[count] = task;
            MainActivity main = MainActivity.getContext();
            boolean found;
            XmlNode response;
            String urlString;
            Context context = MainActivity.getContext();
            switch (task) {
                case Video.ACTION_REFRESH:
                    mValue = 0;
                    found = false;
                    try {
                        if (context == null)
                            return null;
                        String pref = Settings.getString("pref_bookmark");
                        String fpsStr = Settings.getString("pref_fps");
                        int fps = 30;
                        try {
                            fps = Integer.parseInt(fpsStr, 10);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            fps = 30;
                        }
                        if (isRecording && ("mythtv".equals(pref) || "auto".equals(pref))) {
                            // look for a mythtv bookmark
                            urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                    "/Dvr/GetSavedBookmark?OffsetType=duration&RecordedId="
                                            + mVideo.recordedid);
                            XmlNode bkmrkData = XmlNode.fetch(urlString, null);
                            try {
                                mValue = Long.parseLong(bkmrkData.getString());
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                mValue = -1;
                            }
                            // sanity check bookmark - between 0 and 24 hrs.
                            // note -1 means a bookmark but no seek table
                            // older version of service returns garbage value when there is
                            // no seek table.
                            if (mValue > 24 * 60 * 60 * 1000 || mValue < 0)
                                mValue = -1;
                            else
                                found = true;
                            if (mValue == -1) {
                                // look for a position bookmark (for recording with no seek table)
                                urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                        "/Dvr/GetSavedBookmark?OffsetType=position&RecordedId="
                                                + mVideo.recordedid);
                                bkmrkData = XmlNode.fetch(urlString, null);
                                long pos = 0;
                                try {
                                    pos = Long.parseLong(bkmrkData.getString());
                                    if (pos > 24 * 60 * 60 * 1000 || pos < 0)
                                        pos = 0;
                                    else
                                        found = true;
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    pos = 0;
                                }
                                mValue = pos * 1000 / fps;
                            }
                        }
                        if (!isRecording || "local".equals(pref) || !found) {
                            // default to none
                            mValue = 0;
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
                            String[] selectionArgs = {mVideo.videoUrl};

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
                                    mValue = cursor.getLong(colno);
                            }
                            cursor.close();
                            db.close();

                        }
                        if (isRecording) {
                            // Find out rec group
                            urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                    "/Dvr/GetRecorded?RecordedId="
                                            + mVideo.recordedid);
                            XmlNode recorded = XmlNode.fetch(urlString, null);
                            mVideo.recGroup = recorded.getString(XMLTAGS_RECGROUP);
                            mVideo.progflags = recorded.getString(XMLTAGS_PROGRAMFLAGS);
                            String newEndtime = recorded.getString(XMLTAGS_ENDTIME);

                            if (main != null && !Objects.equals(mVideo.endtime, newEndtime)) {
                                mVideo.endtime = recorded.getString(XMLTAGS_ENDTIME);
                                main.getMainFragment().startFetch();
                            }
                        }
                        else {
                            urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                    "/Video/GetVideo?Id="
                                            + mVideo.recordedid);
                            XmlNode resp = XmlNode.fetch(urlString, null);
                            String watched = resp.getString(XMLTAG_WATCHED);
                            if ("true".equals(watched))
                                watched = VALUE_WATCHED;
                            else
                                watched = "0";
                            mVideo.progflags = watched;
                        }
                    } catch(IOException | XmlPullParserException e){
                        mValue = 0;
                        e.printStackTrace();
                    }
                    break;
                case Video.ACTION_DELETE:
                    // Delete recording
                    // If already deleted do not delete again.
                    if (!isRecording || "Deleted".equals(mVideo.recGroup))
                        break;
                    try {
                        urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                "/Dvr/DeleteRecording?RecordedId="
                                        + mVideo.recordedid);
                        response = XmlNode.fetch(urlString, "POST");
                        if (main != null)
                            main.getMainFragment().startFetch();
                    } catch (IOException | XmlPullParserException e) {
                        e.printStackTrace();
                    }
                    break;
                case Video.ACTION_UNDELETE:
                    // UnDelete recording
                    if (!isRecording)
                        break;
                    try {
                        urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                "/Dvr/UnDeleteRecording?RecordedId="
                                        + mVideo.recordedid);
                        response = XmlNode.fetch(urlString, "POST");
                        if (main != null)
                            main.getMainFragment().startFetch();
                    } catch (IOException | XmlPullParserException e) {
                        e.printStackTrace();
                    }
                    break;
                case Video.ACTION_SET_BOOKMARK:
                    try {
                        found = false;
                        String pref = Settings.getString("pref_bookmark");
                        String fpsStr = Settings.getString("pref_fps");
                        int fps = 30;
                        try {
                            fps = Integer.parseInt(fpsStr,10);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            fps = 30;
                        }
                        if (isRecording && ("mythtv".equals(pref)||"auto".equals(pref))) {
                            // store a mythtv bookmark
                            urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                    "/Dvr/SetSavedBookmark?OffsetType=duration&RecordedId="
                                            + mVideo.recordedid + "&Offset=" + mValue);
                            response = XmlNode.fetch(urlString, "POST");
                            String result = response.getString();
                            if ("true".equals(result))
                                found = true;
                            else {
                                // store a mythtv position bookmark (in case there is no seek table)
                                long posBkmark = mValue * fps / 1000;
                                urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                        "/Dvr/SetSavedBookmark?RecordedId="
                                                + mVideo.recordedid + "&Offset=" + posBkmark);
                                response = XmlNode.fetch(urlString, "POST");
                                result = response.getString();
                            }
                        }
                        if (!isRecording || "local".equals(pref) || !found) {
                            // Use local bookmark

                            // Gets the data repository in write mode
                            VideoDbHelper dbh = new VideoDbHelper(main);
                            SQLiteDatabase db = dbh.getWritableDatabase();

                            // Create a new map of values, where column names are the keys
                            ContentValues values = new ContentValues();
                            Date now = new Date();
                            values.put(VideoContract.StatusEntry.COLUMN_LAST_USED, now.getTime());
                            values.put(VideoContract.StatusEntry.COLUMN_BOOKMARK, mValue);

                            // First try an update
                            String selection = VideoContract.StatusEntry.COLUMN_VIDEO_URL + " = ?";
                            String[] selectionArgs = {mVideo.videoUrl};

                            int sqlCount = db.update(
                                    VideoContract.StatusEntry.TABLE_NAME,
                                    values,
                                    selection,
                                    selectionArgs);

                            if (sqlCount == 0) {
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
                case Video.ACTION_SET_WATCHED:
                    try {
                        if (isRecording)
                            // set recording watched
                            urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                "/Dvr/UpdateRecordedWatchedStatus?RecordedId="
                                        + mVideo.recordedid + "&Watched=" + mWatched);
                        else
                            // set video watched
                            urlString = XmlNode.mythApiUrl(mVideo.hostname,
                                    "/Video/UpdateVideoWatchedStatus?Id="
                                            + mVideo.recordedid + "&Watched=" + mWatched);
                        response = XmlNode.fetch(urlString, "POST");
                        String result = response.getString();
                        if (main != null)
                            main.getMainFragment().startFetch();
                    } catch (IOException | XmlPullParserException e) {
                        e.printStackTrace();
                    }
                    break;
                case Video.ACTION_FILELENGTH:
                    // mValue is prior file length to be checked against
                    // Try 10 times until file length increases.
                    urlString = mVideo.videoUrl;
                    HttpURLConnection urlConnection = null;
                    mFileLength = -1;
                    for (int counter = 0 ; counter < 5 ; counter++) {
                        try {
                            // pause 1 second between attempts
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        try {
                            URL url = new URL(urlString);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.addRequestProperty("Cache-Control", "no-cache");
                            urlConnection.setConnectTimeout(5000);
                            urlConnection.setReadTimeout(30000);
                            urlConnection.setRequestMethod("HEAD");
                            mFileLength = urlConnection.getContentLength();
                            if (mFileLength > mValue)
                                break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (urlConnection != null)
                                urlConnection.disconnect();
                        }
                    }
                    break;
                case Video.ACTION_LIVETV:
                    // Schedule a recording for 3 hours starting now
                    // Wait for recording to be ready
                    // Play recording, passing in the info needed for cancelling it on exit.

                    // Replace the video (channel dummy video) in this object with the recording
                    Video channel = mVideo;
                    mVideo = null;
                    try {
                        // Get values needed to set up recording
                        Date startTime = new Date();
                        // 3 hours
                        String pref = Settings.getString("pref_livetv_duration");
                        int duration = 60;
                        try {
                            duration = Integer.parseInt(pref, 10);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            duration = 60;
                        }
                        if (duration < 15)
                            duration = 15;
                        else if (duration > 360)
                            duration = 360;
                        Date endTime = new Date(startTime.getTime()+duration*60*1000);
                        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
                        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
                        String recDate = sdfDate.format(startTime);
                        String recTime = sdfTime.format(startTime);
                        String title = context.getString(R.string.title_livetv_recording)
                                + " " + recDate;
                        String subtitle = recTime + " ch " + channel.channum;
                        SimpleDateFormat sdfUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
                        urlString = XmlNode.mythApiUrl(null,
                                "/Dvr/AddRecordSchedule?Title="
                                        + URLEncoder.encode(title, "UTF-8")
                                        + "&Subtitle=" + URLEncoder.encode(subtitle, "UTF-8")
                                        + "&Chanid=" + channel.chanid
                                        + "&Station=" + channel.callsign
                                        + "&StartTime=" + URLEncoder.encode(sdfUTC.format(startTime), "UTF-8")
                                        + "&EndTime=" + URLEncoder.encode(sdfUTC.format(endTime), "UTF-8")
                                        + "&Type=Single+Record"
                                        // Use a nonsense FindDay and FindTime because they are required by the API
                                        // but not used for this type of recording.
                                        + "&FindDay=1&FindTime=21%3A30%3A00"
                                        + "&SearchType=Manual+Search&AutoExpire=true&RecPriority=-99"
                                        + "&RecGroup=LiveTV&StorageGroup=LiveTV"
                                        );
                        response = XmlNode.fetch(urlString, "POST");
                        String result = response.getString();
                        Log.i(TAG, CLASS + " Live TV scheduled, RecordId:" + result);
                        mRecordId = Integer.parseInt(result);
                        // Now try to find the recording
                        urlString = XmlNode.mythApiUrl(null,
                                "/Dvr/GetRecordedList?RecGroup=LiveTV"
                                + "&TitleRegEx=" + URLEncoder.encode("^"+title+"$", "UTF-8"));
                        found = false;
                        int ixFound = -1;
                        for (int icount = 0; icount < 15 && !found; icount ++) {
                            response = XmlNode.fetch(urlString,null);
                            Log.d(TAG, CLASS + " Found " + response.getString("Count") +" recordings");
                            XmlNode programNode = null;
                            for (ixFound = 0; ; ixFound++ ) {
                                if (programNode == null)
                                    programNode = response.getNode(VideoDbBuilder.XMLTAGS_PROGRAM, 0);
                                else
                                    programNode = programNode.getNextSibling();
                                if (programNode == null)
                                    break;
                                String tmpRecordId = programNode.getString(XMLTAGS_RECORDID);
                                if (tmpRecordId != null && Integer.parseInt(tmpRecordId) == mRecordId) {
                                    String fileSizeStr = programNode.getString(VideoDbBuilder.XMLTAG_FILESIZE);
                                    String tmpRecordedId = programNode.getString(XMLTAGS_RECORDEDID);
                                    if (tmpRecordedId != null)
                                        mRecordedId = Integer.parseInt(tmpRecordedId);
                                    long fileSize = 0;
                                    if (fileSizeStr != null)
                                        fileSize = Long.parseLong(fileSizeStr);
                                    // Skip dummy LiveTV entry
                                    if (fileSize > 1000) {
                                        Log.d(TAG, CLASS + " Found matching recording " + ixFound + ". RecordedId:" + tmpRecordedId);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            Thread.sleep(1000);
                        }
                        if (!found || ixFound < 0) {
                            Log.e(TAG, CLASS + " Failed to find matching recording.");
                            return null;
                        }
                        VideoDbBuilder builder = new VideoDbBuilder(context);
                        List<ContentValues> contentValuesList = new ArrayList<>();
                        builder.buildMedia(response, 0, ixFound, contentValuesList);
                        ContentValues[] downloadedVideoContentValues =
                                contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
                        context.getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                                downloadedVideoContentValues);

                        // Get recording from DB
                        VideoDbHelper dbh = new VideoDbHelper(context);
                        SQLiteDatabase db = dbh.getReadableDatabase();

                        // Filter results
                        String selection = VideoContract.VideoEntry.COLUMN_RECORDEDID + " = " + mRecordedId;

                        Cursor cursor = db.query(
                                VideoContract.VideoEntry.TABLE_NAME,   // The table to query
                                null,             // The array of columns to return (pass null to get all)
                                selection,              // The columns for the WHERE clause
                                null,          // The values for the WHERE clause
                                null,                   // don't group the rows
                                null,                   // don't filter by row groups
                                null               // The sort order
                        );

                        // We expect one or zero results, never more than one.
                        if (cursor.moveToNext()) {
                            VideoCursorMapper mapper = new VideoCursorMapper();
                            mVideo = (Video) mapper.convert(cursor);
                        }
                        else
                            Log.e(TAG, CLASS + " Failed to find recording on SQLite.");

                        cursor.close();
                        db.close();
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        Log.e(TAG, CLASS + " Exception setting up Live TV.", e);
                    }
                    break;
                case Video.ACTION_STOP_RECORDING:
                    // Stop recording
                    if ("-1".equals(mVideo.recordedid))
                        break;
                    try {
                        urlString = XmlNode.mythApiUrl(null,
                                "/Dvr/StopRecording?RecordedId=" + mVideo.recordedid);
                        response = XmlNode.fetch(urlString, null);
                        String result = response.getString();
                        if ("true".equals(result))
                            Log.i(TAG, CLASS + " Recording Stopped. RecordedId:" + mVideo.recordedid);
                        else
                            Log.e(TAG, CLASS + " Stop Recording Failed.");
                    } catch (Exception e) {
                        Log.e(TAG, CLASS + " Exception Stopping Recording.", e);
                    }
                    break;

                case Video.ACTION_REMOVE_RECORD_RULE:
                    try {
                        urlString = XmlNode.mythApiUrl(null,
                                "/Dvr/RemoveRecordSchedule?RecordId=" + mValue);
                        response = XmlNode.fetch(urlString, "POST");
                        String result = response.getString();
                        if ("true".equals(result))
                            Log.i(TAG, CLASS + " Record Rule Removed. recordId:" + mValue);
                        else
                            Log.e(TAG, CLASS + " Remove Record Rule Failed.");
                    } catch (Exception e) {
                        Log.e(TAG, CLASS + " Exception Removing Record Rule.", e);
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    protected void onPostExecute(Void result) {

        if (mBackendCallListener != null)
            mBackendCallListener.onPostExecute(this);
    }

}
