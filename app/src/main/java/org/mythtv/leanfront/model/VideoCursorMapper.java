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

package org.mythtv.leanfront.model;

import android.database.Cursor;
import androidx.leanback.database.CursorMapper;

import org.mythtv.leanfront.data.VideoContract;

/**
 * VideoCursorMapper maps a database Cursor to a Video object.
 */
public final class VideoCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int titleIndex;
    private static int nameIndex;
    private static int descIndex;
    private static int videoUrlIndex;
    private static int bgImageUrlIndex;
    private static int cardImageUrlIndex;
    private static int channelIndex;
    private static int recordedidIndex;
    private static int recGroupIndex;
    private static int seasonIndex;
    private static int episodeIndex;
    private static int airdateIndex;
    private static int starttimeIndex;
    private static int durationIndex;
    private static int prodyearIndex;
    private static int filenameIndex;
    private static int progflagsIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(VideoContract.VideoEntry._ID);
        titleIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_TITLE);
        nameIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_SUBTITLE);
        descIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_DESC);
        videoUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_VIDEO_URL);
        bgImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL);
        cardImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CARD_IMG);
        channelIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CHANNEL);
        recordedidIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_RECORDEDID);
        recGroupIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_RECGROUP);
        seasonIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_SEASON);
        episodeIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_EPISODE);
        airdateIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_AIRDATE);
        starttimeIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_STARTTIME);
        durationIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_DURATION);
        prodyearIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR);
        filenameIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_FILENAME);
        progflagsIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_PROGFLAGS);
    }

    @Override
    protected Object bind(Cursor cursor) {

        // One time it failed with cursor closed. I don't know why
        // but maybe this will catch it.
        if (cursor.isClosed())
            return new Video.VideoBuilder()
                    .title("ERROR - CURSOR CLOSED")
                    .build();
        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        String title = cursor.getString(titleIndex);
        String subtitle = cursor.getString(nameIndex);
        String desc = cursor.getString(descIndex);
        String videoUrl = cursor.getString(videoUrlIndex);
        String bgImageUrl = cursor.getString(bgImageUrlIndex);
        String cardImageUrl = cursor.getString(cardImageUrlIndex);
        String channel = cursor.getString(channelIndex);
        String recordedid = cursor.getString(recordedidIndex);
        String recGroup = cursor.getString(recGroupIndex);

        String season = cursor.getString(seasonIndex);
        String episode = cursor.getString(episodeIndex);
        String airdate = cursor.getString(airdateIndex);
        String starttime = cursor.getString(starttimeIndex);
        String duration = cursor.getString(durationIndex);
        String prodyear = cursor.getString(prodyearIndex);
        String filename = cursor.getString(filenameIndex);
        String progflags = cursor.getString(progflagsIndex);

        // Build a Video object to be processed.
        return new Video.VideoBuilder()
                .id(id)
                .title(title)
                .subtitle(subtitle)
                .description(desc)
                .videoUrl(videoUrl)
                .bgImageUrl(bgImageUrl)
                .cardImageUrl(cardImageUrl)
                .channel(channel)
                .recordedid(recordedid)
                .recGroup(recGroup)
                .season(season)
                .episode(episode)
                .airdate(airdate)
                .starttime(starttime)
                .duration(duration)
                .prodyear(prodyear)
                .filename(filename)
                .progflags(progflags)
                .build();
    }
}
