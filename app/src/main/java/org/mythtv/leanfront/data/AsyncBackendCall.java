package org.mythtv.leanfront.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;

import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.ui.MainActivity;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;


public class AsyncBackendCall extends AsyncTask<Integer, Void, Void> {

    public interface OnBackendCallListener {
        default void onPostExecute(AsyncBackendCall taskRunner) {}
    }

    private Video mVideo;
    private long mBookmark;
    private OnBackendCallListener mBackendCallListener;
    private boolean mWatched;
    private int [] mTasks;
    private long mFileLength = -1;

    // Parsing results of GetRecorded
    private static final String[] XMLTAGS_RECGROUP = {"Recording","RecGroup"};
    private static final String[] XMLTAGS_PROGRAMFLAGS = {"ProgramFlags"};
    private static final String XMLTAG_WATCHED = "Watched";
    private static final String VALUE_WATCHED = (new Integer(Video.FL_WATCHED)).toString();

    public AsyncBackendCall(Video videoA, long bookmarkA, boolean watched,
            OnBackendCallListener backendCallListener) {
        mVideo = videoA;
        mBookmark = bookmarkA;
        mBackendCallListener = backendCallListener;
        mWatched = watched;
    }

    public long getBookmark() {
        return mBookmark;
    }

    public long getFileLength() {
        return mFileLength;
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
            switch (task) {
                case Video.ACTION_REFRESH:
                    mBookmark = 0;
                    found = false;
                    try {
                        Context context = MainActivity.getContext();
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
                            urlString = XmlNode.mythApiUrl(
                                    "/Dvr/GetSavedBookmark?OffsetType=duration&RecordedId="
                                            + mVideo.recordedid);
                            XmlNode bkmrkData = XmlNode.fetch(urlString, null);
                            try {
                                mBookmark = Long.parseLong(bkmrkData.getString());
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                mBookmark = -1;
                            }
                            // sanity check bookmark - between 0 and 24 hrs.
                            // note -1 means a bookmark but no seek table
                            // older version of service returns garbage value when there is
                            // no seek table.
                            if (mBookmark > 24 * 60 * 60 * 1000 || mBookmark < 0)
                                mBookmark = -1;
                            else
                                found = true;
                            if (mBookmark == -1) {
                                // look for a position bookmark (for recording with no seek table)
                                urlString = XmlNode.mythApiUrl(
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
                                mBookmark = pos * 1000 / fps;
                            }
                        }
                        if (!isRecording || "local".equals(pref) || !found) {
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
                                if (colno >= 0) {
                                    mBookmark = cursor.getLong(colno);
                                }
                            }
                            cursor.close();
                            db.close();

                        }
                        if (isRecording) {
                            // Find out rec group
                            urlString = XmlNode.mythApiUrl(
                                    "/Dvr/GetRecorded?RecordedId="
                                            + mVideo.recordedid);
                            XmlNode recorded = XmlNode.fetch(urlString, null);
                            mVideo.recGroup = recorded.getString(XMLTAGS_RECGROUP);
                            mVideo.progflags = recorded.getString(XMLTAGS_PROGRAMFLAGS);
                        }
                        else {
                            urlString = XmlNode.mythApiUrl(
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
                        mBookmark = 0;
                        e.printStackTrace();
                    }
                    break;
                case Video.ACTION_DELETE:
                    // Delete recording
                    // If already deleted do not delete again.
                    if (!isRecording || "Deleted".equals(mVideo.recGroup))
                        break;
                    try {
                        urlString = XmlNode.mythApiUrl(
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
                        urlString = XmlNode.mythApiUrl(
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
                            urlString = XmlNode.mythApiUrl(
                                    "/Dvr/SetSavedBookmark?OffsetType=duration&RecordedId="
                                            + mVideo.recordedid + "&Offset=" + mBookmark);
                            response = XmlNode.fetch(urlString, "POST");
                            String result = response.getString();
                            if ("true".equals(result))
                                found = true;
                            else {
                                // store a mythtv position bookmark (in case there is no seek table)
                                long posBkmark = mBookmark * fps / 1000;
                                urlString = XmlNode.mythApiUrl(
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
                            values.put(VideoContract.StatusEntry.COLUMN_BOOKMARK, mBookmark);

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
                            urlString = XmlNode.mythApiUrl(
                                "/Dvr/UpdateRecordedWatchedStatus?RecordedId="
                                        + mVideo.recordedid + "&Watched=" + mWatched);
                        else
                            // set video watched
                            urlString = XmlNode.mythApiUrl(
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
                    urlString = mVideo.videoUrl;
                    HttpURLConnection urlConnection = null;
                    mFileLength = -1;
                    try {
                        URL url = new URL(urlString);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.addRequestProperty("Cache-Control", "no-cache");
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.setReadTimeout(30000);
                        urlConnection.setRequestMethod("HEAD");
                        mFileLength = urlConnection.getContentLength();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (urlConnection != null)
                            urlConnection.disconnect();
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
