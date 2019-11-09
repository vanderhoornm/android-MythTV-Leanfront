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
        progflagsIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_PROGFLAGS);
    }

    @Override
    protected Object bind(Cursor cursor) {

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
                .progflags(progflags)
                .build();
    }
}
