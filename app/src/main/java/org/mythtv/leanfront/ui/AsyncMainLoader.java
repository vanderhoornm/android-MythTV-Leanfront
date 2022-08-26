/*
 * Copyright (c) 2014 The Android Open Source Project
 * Copyright (c) 2019-2021 Peter Bennett
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

import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_AIRDATE;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_CHANNUM;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_EPISODE;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_FILENAME;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_RECGROUP;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_RECORDEDID;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_RECTYPE;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_SEASON;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_STARTTIME;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_TITLE;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.COLUMN_TITLEMATCH;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.RECTYPE_CHANNEL;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.RECTYPE_RECORDING;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.RECTYPE_VIDEO;
import static org.mythtv.leanfront.data.VideoContract.VideoEntry.VIEW_NAME;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_CHANNEL;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_CHANNEL_ALL;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_EPISODE;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_RECENTS;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_RECGROUP;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_RECGROUP_ALL;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_SERIES;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_TOPLEVEL;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_TOP_ALL;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_VIDEO;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_VIDEODIR;
import static org.mythtv.leanfront.ui.MainFragment.TYPE_VIDEODIR_ALL;
import static org.mythtv.leanfront.ui.MainFragment.makeTitleSort;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import org.mythtv.leanfront.MyApplication;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.data.VideoDbHelper;
import org.mythtv.leanfront.model.ListItem;
import org.mythtv.leanfront.model.MyHeaderItem;
import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

// ArrayList return as follows
// Each entry is an ArrayList describing one row
// Each row arraylist has
//   [0] is a MyHeaderItem
//   [1] onwards are each a Video

public class AsyncMainLoader extends AsyncTask<MainFragment, Void, ArrayList<ArrayList<ListItem>>> {
    MainFragment mainFragment;
    int mType;
    String mBaseName;

    private static final String TAG = "lfe";
    private static final String CLASS = "AsyncMainLoader";

    @Override
    protected ArrayList<ArrayList<ListItem>> doInBackground(MainFragment... mainFragments) {
        if (mainFragments.length != 1) {
            Log.e(TAG, CLASS + " doInBackground called with wrong number "
                    + mainFragments.length + " of parameters.");
            return null;
        }
        try {
            mainFragment = mainFragments[0];
            mType = mainFragment.mType;
            mBaseName = mainFragment.mBaseName;
            Cursor csr = queryDb();
            ArrayList<ArrayList<ListItem>> list = buildRows(csr);
            csr.close();
            return list;
        } catch (Exception ex) {
            Log.e(TAG, CLASS + " doInBackground exception",ex);
            return null;
        }
    }

    // This replaces Loader<Cursor> onCreateLoader(int id, Bundle args)
    // Query the database for all videos on tyhe main fragment
    private Cursor queryDb() {
        String seq = Settings.getString("pref_seq");
        String ascdesc = Settings.getString("pref_seq_ascdesc");
        boolean mergevideos = "true".equals(Settings.getString("pref_merge_videos"));
        StringBuilder orderby = new StringBuilder();
        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = null;

        /*
        SQL "order by" is complicated. Below are examples for the various cases
        Note: RECTYPE_RECORDING = 1;  RECTYPE_VIDEO = 2;  RECTYPE_CHANNEL = 3;

        Top Level list or Videos list
            CASE WHEN rectype = 3 THEN 1 ELSE rectype END,
            CASE WHEN rectype = 2
             THEN REPLACE(REPLACE(REPLACE('/'||UPPER(filename),'/THE ','/'),'/A ','/'),'/AN ','/')
             ELSE NULL END,
            recgroup,
            titlematch, -- (WAS REPLACE(REPLACE(REPLACE('^'||UPPER(suggest_text_1),'^THE ','^'),'^A ','^'),'^AN ','^'), )
            starttime asc, airdate asc

        Recording Group list
            titlematch, -- (WAS REPLACE(REPLACE(REPLACE('^'||UPPER(suggest_text_1),'^THE ','^'),'^A ','^'),'^AN ','^'),)
            starttime asc, airdate asc

        LiveTV list
            CAST (channum as real), channum,
            titlematch, -- (WAS REPLACE(REPLACE(REPLACE('^'||UPPER(suggest_text_1),'^THE ','^'),'^A ','^'),'^AN ','^'),)
            starttime asc, airdate asc
         */

        if (mType == TYPE_TOPLEVEL || mType == TYPE_VIDEODIR) {
            // This case will sort channels together with videos
            orderby.append("CASE WHEN ");
            orderby.append(COLUMN_RECTYPE).append(" = ");
            orderby.append(RECTYPE_CHANNEL);
            orderby.append(" THEN ").append(RECTYPE_RECORDING);
            orderby.append(" ELSE ").append(COLUMN_RECTYPE).append(" END, ");
            orderby.append("CASE WHEN ");
            orderby.append(COLUMN_RECTYPE).append(" = ");
            orderby.append(RECTYPE_VIDEO).append(" THEN ");
            StringBuilder fnSort = makeTitleSort(COLUMN_FILENAME, '/');
            orderby.append(fnSort);
            orderby.append(" ELSE NULL END, ");
            orderby.append(COLUMN_RECGROUP).append(", ");
        }
        // for Recording Group page, limit selection to those recordings.
        if (mType == TYPE_RECGROUP) {
            // Only the "All" recgroup basename ends with \t
            if (mBaseName.endsWith("\t")) {
                //  select * from videoview where recgroup not in ('LiveTV','Deleted')
                //  OR recgroup is null AND titlematch in
                //    (select distinct titlematch from videoview where recgroup not in ('LiveTV','Deleted'))
                //  order by titlematch
                makeRecGroupSelection(selection," NOT IN ('LiveTV','Deleted') ", mergevideos);
            }
            else if (mBaseName.equals("LiveTV")) {
                selection.append(COLUMN_RECGROUP).append(" = 'LiveTV' ");
                orderby.append("CAST (").append(COLUMN_CHANNUM).append(" as real), ");
                orderby.append(COLUMN_CHANNUM).append(", ");
            }
            else if (mBaseName.equals("Deleted")) {
                selection.append(COLUMN_RECGROUP).append(" = 'Deleted' ");
            }
            // All other Recgroups
            else {
                //  select * from videoview where recgroup = 'Default'
                //  OR recgroup is null AND titlematch in
                //    (select distinct titlematch from videoview where recgroup = 'Default')
                //  order by titlematch
                makeRecGroupSelection(selection," = ? ", mergevideos);
                if (mergevideos) {
                    selectionArgs = new String[2];
                    selectionArgs[0] = mBaseName;
                    selectionArgs[1] = mBaseName;
                } else {
                    selectionArgs = new String[1];
                    selectionArgs[0] = mBaseName;
                }
            }
        }
        // for Video Directory page, limit selection to videos
        if (mType == TYPE_VIDEODIR) {
            selection.append(COLUMN_RECTYPE).append(" = ");
            selection.append(RECTYPE_VIDEO);
        }

//        StringBuilder titleSort = makeTitleSort(VideoContract.VideoEntry.COLUMN_TITLE, '^');
//        orderby.append(titleSort).append(", ");
        orderby.append(COLUMN_TITLEMATCH).append(", ");
        if ("airdate".equals(seq)) {
            // +0 is used to convert the value to a number
            orderby.append(COLUMN_SEASON).append("+0 ")
                    .append(ascdesc).append(", ");
            orderby.append(COLUMN_EPISODE).append("+0 ")
                    .append(ascdesc).append(", ");
            orderby.append(COLUMN_AIRDATE).append(" ")
                    .append(ascdesc).append(", ");
            orderby.append(COLUMN_STARTTIME).append(" ")
                    .append(ascdesc);
        } else {
            orderby.append(COLUMN_STARTTIME).append(" ")
                    .append(ascdesc).append(", ");
            orderby.append(COLUMN_AIRDATE).append(" ")
                    .append(ascdesc);
        }

        // Add recordedid to sort for in case of duplicates or split recordings
        orderby.append(", ").append(COLUMN_RECORDEDID).append(" ")
                .append(ascdesc);

        Context context = MyApplication.getAppContext();
        VideoDbHelper dbh = VideoDbHelper.getInstance(context);
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor cursor = db.query(
                VIEW_NAME,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                selection.toString(),              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                orderby.toString()               // The sort order
        );
        return cursor;
    }

    static void makeRecGroupSelection(StringBuilder selection, String recgroups, boolean mergevideos) {
        //  select * from videoview where  recgroup not in ('LiveTV','Deleted')
        //  OR recgroup is null AND titlematch in
        //    (select distinct titlematch from videoview where recgroup not in ('LiveTV','Deleted'))
        //  order by titlematch
        selection.append(COLUMN_RECGROUP).append(recgroups);
        if (mergevideos) {
            selection.append(" OR ").append(COLUMN_RECGROUP).append(" IS NULL ")
                    .append(" AND ")
                    .append(COLUMN_TITLEMATCH).append(" IN (")
                    .append(" SELECT DISTINCT ").append(COLUMN_TITLEMATCH)
                    .append(" FROM ").append(VIEW_NAME).append(" WHERE ")
                    .append(COLUMN_RECGROUP).append(recgroups).append(")");
        }
    }

    // This replaces onLoadFinished(Loader<Cursor> loader, Cursor data)
    // Organize videos into rows for display.
    private ArrayList<ArrayList<ListItem>> buildRows(Cursor data) {
        Context context = MyApplication.getAppContext();
        VideoCursorMapper mapper = new VideoCursorMapper();
        ArrayList<ArrayList<ListItem>> categoryList = new ArrayList<>();
        String seq = Settings.getString("pref_seq");
        String ascdesc = Settings.getString("pref_seq_ascdesc");
        boolean showRecents = "true".equals(Settings.getString("pref_show_recents"));
        boolean recentsTrim = "true".equals(Settings.getString("pref_recents_trim"));
        boolean showRecentDeleted = "true".equals(Settings.getString("pref_recents_deleted"));
        boolean showRecentWatched = "true".equals(Settings.getString("pref_recents_watched"));
        recentsTrim = recentsTrim && (showRecentDeleted || showRecentWatched);

        int allType = TYPE_RECGROUP_ALL;
        String allTitle = null;
        if (mType == TYPE_TOPLEVEL) {
            allTitle = context.getString(R.string.all_title) + "\t";
            allType = TYPE_TOP_ALL;
        }
        if (mType == TYPE_RECGROUP) {
            if (!mBaseName.endsWith("\t"))
                allTitle = mBaseName + "\t";
            allType = TYPE_RECGROUP_ALL;
        }

        int rectypeIndex = data.getColumnIndex(COLUMN_RECTYPE);
        int recgroupIndex = data.getColumnIndex(COLUMN_RECGROUP);
        int titleIndex = data.getColumnIndex(COLUMN_TITLE);
        int titleMatchIndex = data.getColumnIndex(COLUMN_TITLEMATCH);
        int airdateIndex = data.getColumnIndex(COLUMN_AIRDATE);
        int starttimeIndex = data.getColumnIndex(COLUMN_STARTTIME);
        int filenameIndex = data.getColumnIndex(COLUMN_FILENAME);
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dbTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        int sortkey;
        SimpleDateFormat sortKeyFormat;
        if ("airdate".equals(seq)) {
            sortkey = airdateIndex;
            sortKeyFormat = dbDateFormat;
        } else {
            sortkey = starttimeIndex;
            sortKeyFormat = dbTimeFormat;
        }
        boolean cursorHasData = data.moveToFirst();

        // Every time we have to re-get the category loader, we must re-create the sidebar.
        categoryList.clear();
        ArrayList<ListItem> rowList = null;
        SparseArray<ListItem> allSparse = null;
        ArrayList<ListItem> allList = null;
        SparseArray<ListItem> recentsSparse = null;
        ArrayList<ListItem> recentsList = null;
        ArrayList<ListItem> rootList = null;
        mapper.changeCursor(data);

        String currentCategoryMatch = null;
        int currentRowType = -1;
        // For videos currentItem is itemname saved used in toplevel videos and videos page
        // For Recordings currentItem is title saved used in toplevel page --> change to titlematch
        String currentItem = null;
        int currentRowNum = -1;
        int allRowNum = -1;
        int recentsRowNum = -1;
        int rootRowNum = -1;
        MyHeaderItem header;


        // Create the recents row, only for top level
        if (mType == TYPE_TOPLEVEL && showRecents) {
            String title = context.getString(R.string.recents_title) + "\t";
            header = new MyHeaderItem(title, TYPE_RECENTS, mBaseName);
            recentsSparse = new SparseArray<>();
            recentsList = new ArrayList<>();
            categoryList.add(recentsList);
            recentsList.add(header);
            recentsRowNum = categoryList.size() - 1;
        }

        // Create "All" row (but not for videos)
        if (mType != TYPE_VIDEODIR) {
            header = new MyHeaderItem(allTitle,
                    allType, mBaseName);
            allSparse = new SparseArray<>();
            ;
            allList = new ArrayList<>();
            ;
            allList.add(header);
            categoryList.add(allList);
            allRowNum = categoryList.size() - 1;
        }

        // Create "Root" row
        if (mType == TYPE_VIDEODIR) {
            String rootTitle = "\t";
            header = new MyHeaderItem(rootTitle,
                    TYPE_VIDEODIR, mBaseName);
            rootList = new ArrayList<>();
            rootList.add(header);
            categoryList.add(rootList);
            rootRowNum = categoryList.size() - 1;
        }

        // Iterate through each category entry and add it to the ArrayAdapter.
        while (cursorHasData && !data.isAfterLast()) {

            boolean addToRow = true;
            int itemType = -1;
            int rowType = -1;

            String recgroup = data.getString(recgroupIndex);
            int rectype = data.getInt(rectypeIndex);

            String category = null;
            String categorymatch = null;
            Video video = (Video) mapper.get(data.getPosition());
            Video dbVideo = video;

            // For Rec Group type, only use recordings from that recording group.
            // categories are titles.
            if (mType == TYPE_RECGROUP) {
                category = data.getString(titleIndex);
                categorymatch = data.getString(titleMatchIndex);
//                if (recgroup != null
//                        && (context.getString(R.string.all_title) + "\t").equals(mBaseName)) {
//                    // Do not mix deleted episodes or LiveTV in the All group
//                    if ("Deleted".equals(recgroup) || "LiveTV".equals(recgroup)) {
//                        data.moveToNext();
//                        continue;
//                    }
//                } else {
//                    if (!Objects.equals(mBaseName, recgroup)) {
//                        data.moveToNext();
//                        continue;
//                    }
//                }
                if (rectype == RECTYPE_RECORDING || rectype == RECTYPE_VIDEO) {
                    rowType = TYPE_SERIES;
                    itemType = TYPE_EPISODE;
                } else if (rectype == RECTYPE_CHANNEL) {
                    rowType = TYPE_CHANNEL_ALL;
                    itemType = TYPE_CHANNEL;
                }
            }

            // For Top Level type, only use 1 recording from each title
            // categories are recgroups

            String filename = data.getString(filenameIndex);
            String[] fileparts;
            String dirname = null;
            // itemname is directory name or file name in video part of
            // top level or video page.
            String itemname = null;
            // Split file name and see if it is a directory
            if (rectype == RECTYPE_VIDEO && filename != null && mType != TYPE_RECGROUP) {
                String shortName = filename;
                // itemlevel 0 means there is only one row for all
                // videos so the first part of the name is the entry
                // in the row.
                int itemlevel = 1;
                if (mType == TYPE_VIDEODIR) {
                    // itemlevel 1 means there is one row for each
                    // directory so the second part of the name is the entry
                    // in the row.
                    itemlevel = 2;
                    if (mBaseName.length() == 0)
                        shortName = filename;
                    else if (shortName.startsWith(mBaseName + "/"))
                        shortName = filename.substring(mBaseName.length() + 1);
                    else {
                        data.moveToNext();
                        continue;
                    }
                }
                fileparts = shortName.split("/");
                if (fileparts.length == 1 || mType == TYPE_TOPLEVEL) {
                    itemname = fileparts[0];
                } else {
                    dirname = fileparts[0];
                    itemname = fileparts[1];
                }
                if ((fileparts.length <= 2 && mType == TYPE_VIDEODIR)
                        || fileparts.length == 1)
                    itemType = TYPE_VIDEO;
                else
                    itemType = TYPE_VIDEODIR;
                if (itemType == TYPE_VIDEODIR && Objects.equals(itemname, currentItem)) {
                    itemType = TYPE_VIDEO;
                    addToRow = false;
                } else
                    currentItem = itemname;
            }

            if (mType == TYPE_TOPLEVEL) {
                if (rectype == RECTYPE_VIDEO) {
                    category = context.getString(R.string.row_header_videos) + "\t";
                    categorymatch = category;
                    rowType = TYPE_VIDEODIR_ALL;
                } else if (rectype == RECTYPE_RECORDING)
                    itemType = TYPE_EPISODE;
                if (rectype == RECTYPE_RECORDING
                        || rectype == RECTYPE_CHANNEL) {
                    category = recgroup;
                    categorymatch = category;
                    String title;
                    if (rectype == RECTYPE_CHANNEL)
                        // TODO translate
                        title = "Channels\t";
                    else
                        title = data.getString(titleIndex);
                    if (Objects.equals(title, currentItem)) {
                        addToRow = false;
                    } else {
                        currentItem = title;
                        rowType = TYPE_RECGROUP;
                    }
                }
            }

            // For Video Directory type, only use videos (recgroup null)
            // category is full directory name.
            // Only one videos page
            // First is "all" row, then "root" row, then dir rows
            // mBaseName = "Videos" String
            // Display = "Videos" String
            if (mType == TYPE_VIDEODIR) {
                category = dirname;
                categorymatch = category;
                rowType = TYPE_VIDEODIR;
            }

            // Change of row
            if (addToRow && category != null && !Objects.equals(categorymatch, currentCategoryMatch)) {
                currentRowNum = categoryList.size();
                rowList = new ArrayList<>();
                header = new MyHeaderItem(category,
                        rowType, mBaseName);
                rowList.add(header);
                categoryList.add(rowList);
                currentCategoryMatch = categorymatch;
            }

            // If a directory, create a placeholder for directory name
            if (itemType == TYPE_VIDEODIR)
                video = new Video.VideoBuilder()
                        .id(-1).title(itemname)
                        .recordedid(itemname)
                        .subtitle("")
                        .bgImageUrl("android.resource://org.mythtv.leanfront/" + R.drawable.background)
                        .progflags("0")
                        .build();
            video.type = itemType;

            // Add video to row
            if (addToRow && category != null) {
                Video tVideo = video;
                if (mType == TYPE_TOPLEVEL && video.rectype == RECTYPE_CHANNEL) {
                    // Create dummy video for "All Channels"
                    tVideo = new Video.VideoBuilder()
                            .id(-1).channel(context.getString(R.string.row_header_channels))
                            .rectype(RECTYPE_CHANNEL)
                            .bgImageUrl("android.resource://org.mythtv.leanfront/" + R.drawable.background)
                            .progflags("0")
                            .build();
                    tVideo.type = TYPE_CHANNEL_ALL;
                }
                rowList.add(tVideo);
            }

            // Add video to "Root" row
            if (addToRow && rootList != null
                    && category == null) {
                rootList.add(video);
            }

            // Add video to "All" row
            if (addToRow && allSparse != null && rowType != TYPE_VIDEODIR_ALL
                    && rectype == RECTYPE_RECORDING
                    && !(mType == TYPE_TOPLEVEL && "Deleted".equals(recgroup))) {
                int position = 0;
                String sortKeyStr = data.getString(sortkey);
                if (sortKeyStr != null) {
                    try {
                        Date date = sortKeyFormat.parse(sortKeyStr);
                        // 525960 minutes in a year
                        // Get position as number of minutes since 1970
                        position = (int) (date.getTime() / 60000L);
                        // Add 70 years in case it is before 1970
                        position += 36817200;
                        if ("desc".equals(ascdesc))
                            position = Integer.MAX_VALUE - position;
                    } catch (ParseException | NullPointerException e) {
                        e.printStackTrace();
                        position = 0;
                    }
                }
                // Make sure we have an empty slot
                try {
                    while (allSparse.get(position) != null)
                        position++;
                } catch (ArrayIndexOutOfBoundsException e) {
                }
                allSparse.put(position, video);
            }

            // Add to recents row if applicable
            if (recentsSparse != null
                    && dbVideo.isRecentViewed()) {
                // 525960 minutes in a year
                // Get key as number of minutes since 1970
                // Will stop working in the year 5982
                int key = (int) (dbVideo.lastUsed / 60000L);
                // Add 70 years in case it is before 1970
                key += 36817200;
                // descending
                key = Integer.MAX_VALUE - key;
                // Make sure we have an empty slot
                try {
                    while (recentsSparse.get(key) != null)
                        key++;
                } catch (ArrayIndexOutOfBoundsException e) {
                }

                // Check if there is already an entry for that series / directory
                // If the user does not want duplicates of recent titles that were
                // watched or deleted

                boolean isDeleted = "Deleted".equals(dbVideo.recGroup);
                boolean isWatched = dbVideo.isWatched();
                if (recentsTrim) {

                    // If all recently viewed episodes of a series are watched/deleted, show the most
                    // recently viewed.
                    // If some recently viewed episodes of a series are watched/deleted and some are not,
                    // show only the ones not watched/deleted

                    String series = dbVideo.titlematch;
                    if (series != null) {
                        for (int fx = 0; fx < recentsSparse.size(); fx++) {
                            Video fvid = (Video) recentsSparse.get(recentsSparse.keyAt(fx));
                            boolean fisDeleted = "Deleted".equals(fvid.recGroup);
                            if (series.equals(fvid.titlematch)
                                    && (isDeleted || fisDeleted || Objects.equals(dbVideo.recGroup, fvid.recGroup))) {
                                int fkey = Integer.MAX_VALUE - ((int) (fvid.lastUsed / 60000L) + 36817200);
                                boolean fisWatched = fvid.isWatched();
                                if ((isDeleted || isWatched) && (fisDeleted || fisWatched)) {
                                    // If the episode we are processing is watched/deleted and the matched
                                    // episode in the list is also, keep the most recent
                                    if (key < fkey) {
                                        // position is closer to front, delete the other one
                                        recentsSparse.delete(fkey);
                                        break;
                                    } else {
                                        // position is later in list - drop this one
                                        key = -1;
                                        break;
                                    }
                                } else if (isDeleted || isWatched) {
                                    // If the episode we are processing is watched/deleted and the matched
                                    // episode in the list is not, keep the non-watched
                                    key = -1;
                                    break;
                                } else if (fisDeleted || fisWatched) {
                                    // If the episode we are processing is not watched/deleted and the matched
                                    // episode in the list is, keep the non-watched
                                    recentsSparse.delete(fkey);
                                    break;
                                }
                            }
                        }
                    }
                }

                // dbVideo.showRecent - if this is false the one we have selected has been
                // Removed from recent list, so do not show it.
                if (key != -1 && dbVideo.showRecent)
                    recentsSparse.put(key, dbVideo);
            }
            data.moveToNext();
        }

        if (recentsSparse != null) {
            // Add sparse entries to arraylist
            for (int ix = 0; ix < recentsSparse.size(); ix++) {
                recentsList.add(recentsSparse.get(recentsSparse.keyAt(ix)));
            }
            // Remove recents if empty
            if (recentsSparse.size() <= 0) {
                categoryList.remove(recentsRowNum);
            }
        }
        if (allSparse != null) {
            // Add sparse entries to arraylist
            for (int ix = 0; ix < allSparse.size(); ix++) {
                allList.add(allSparse.get(allSparse.keyAt(ix)));
            }
        }
        return categoryList;
    }

    // This runs in the UI thread
    @Override
    protected void onPostExecute(ArrayList<ArrayList<ListItem>> list) {
        mainFragment.onAsyncLoadFinished(this,list);
    }
}
