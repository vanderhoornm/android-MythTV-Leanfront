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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.mythtv.leanfront.data.VideoContract.VideoEntry;
import org.mythtv.leanfront.data.VideoContract.StatusEntry;


/**
 * VideoDbHelper manages the creation and upgrade of the database used in this sample.
 */
public class VideoDbHelper extends SQLiteOpenHelper {

    // Change this when you change the database schema.
    private static final int DATABASE_VERSION = 10;

    // The name of our database.
    private static final String DATABASE_NAME = "leanback.db";

    public VideoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db,0,DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // On any upgrade just recreate this table
            db.execSQL("DROP TABLE IF EXISTS " + VideoEntry.TABLE_NAME);
            // Create a table to hold videos.
            // This table gets deleted and recreated periodically
            final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE " + VideoEntry.TABLE_NAME + " (" +
                    VideoEntry._ID + " INTEGER PRIMARY KEY," +
                    VideoEntry.COLUMN_RECTYPE + " INTEGER, " +
                    VideoEntry.COLUMN_TITLE + " TEXT, " +
                    VideoEntry.COLUMN_SUBTITLE + " TEXT, " +
                    VideoEntry.COLUMN_VIDEO_URL + " TEXT, " +
                    VideoEntry.COLUMN_FILENAME + " TEXT, " +
                    VideoEntry.COLUMN_HOSTNAME + " TEXT, " +
                    VideoEntry.COLUMN_DESC + " TEXT, " +
                    VideoEntry.COLUMN_BG_IMAGE_URL + " TEXT, " +
                    VideoEntry.COLUMN_CHANNEL + " TEXT, " +
                    VideoEntry.COLUMN_CARD_IMG + " TEXT, " +
                    VideoEntry.COLUMN_CONTENT_TYPE + " TEXT, " +
                    VideoEntry.COLUMN_PRODUCTION_YEAR + " TEXT, " +
                    VideoEntry.COLUMN_DURATION + " TEXT, " +
                    VideoEntry.COLUMN_ACTION + " TEXT," +
                    VideoEntry.COLUMN_AIRDATE + " TEXT," +
                    VideoEntry.COLUMN_STARTTIME + " TEXT," +
                    VideoEntry.COLUMN_ENDTIME + " TEXT," +
                    VideoEntry.COLUMN_RECORDEDID + " TEXT," +
                    VideoEntry.COLUMN_STORAGEGROUP + " TEXT," +
                    VideoEntry.COLUMN_RECGROUP + " TEXT," +
                    VideoEntry.COLUMN_SEASON + " TEXT," +
                    VideoEntry.COLUMN_EPISODE + " TEXT," +
                    VideoEntry.COLUMN_PROGFLAGS + " TEXT," +
                    VideoEntry.COLUMN_CHANID   + " TEXT," +
                    VideoEntry.COLUMN_CHANNUM  + " TEXT," +
                    VideoEntry.COLUMN_CALLSIGN + " TEXT" +
                    " );";

            // Do the creating of the table.
            db.execSQL(SQL_CREATE_VIDEO_TABLE);
        }
        // This table needs to be preserved. Use alter rather than recreating
        if (oldVersion == 0) {
            db.execSQL("DROP TABLE IF EXISTS " + StatusEntry.TABLE_NAME);
            // videostatus table keeps bookmarks even when video table is reloaded.
            // LAST_USED column is datetime, used to delete entries older than a month.
            final String SQL_CREATE_VIDEOSTATUS_TABLE = "CREATE TABLE " + StatusEntry.TABLE_NAME + " (" +
                    StatusEntry._ID + " INTEGER PRIMARY KEY," +
                    StatusEntry.COLUMN_VIDEO_URL + " TEXT NOT NULL UNIQUE, " +
                    StatusEntry.COLUMN_LAST_USED + " INTEGER NOT NULL, " +
                    StatusEntry.COLUMN_BOOKMARK + " INTEGER);";
            db.execSQL(SQL_CREATE_VIDEOSTATUS_TABLE);
            oldVersion = 1;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}
