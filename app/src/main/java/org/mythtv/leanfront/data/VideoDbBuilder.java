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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;

import org.mythtv.leanfront.R;

import org.mythtv.leanfront.model.Video;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * The VideoDbBuilder is used to grab a XML file from a server and parse the data
 * to be placed into a local database
 */
public class VideoDbBuilder {
    private static final String[] XMLTAGS_PROGRAM = {"Programs","Program"};
    private static final String[] XMLTAGS_ARTINFO = {"Artwork","ArtworkInfos","ArtworkInfo"};
    private static final String[] XMLTAGS_CHANNELNAME = {"Channel","ChannelName"};

    public static final String XMLTAG_RECORDING = "Recording";
    public static final String XMLTAG_TITLE = "Title";
    public static final String XMLTAG_DESCRIPTION = "Description";
    public static final String XMLTAG_STORAGEGROUP = "StorageGroup";
    public static final String XMLTAG_RECGROUP = "RecGroup";
    public static final String XMLTAG_RECORDEDID = "RecordedId";
    public static final String XMLTAG_SEASON = "Season";
    public static final String XMLTAG_EPISODE = "Episode";
    public static final String XMLTAG_FILENAME = "FileName";
    public static final String XMLTAG_ARTTYPE = "Type";
    public static final String XMLTAG_ARTURL = "URL";
    public static final String XMLTAG_SUBTITLE = "SubTitle";
    public static final String XMLTAG_STARTTIME = "StartTime";
    public static final String XMLTAG_AIRDATE = "Airdate";
    public static final String XMLTAG_STARTTS = "StartTs";
    public static final String XMLTAG_ENDTS = "EndTs";
    public static final String XMLTAG_PROGFLAGS = "ProgramFlags";
    public static final String XMLTAG_FILESIZE = "FileSize";

    // Specific to video list
    private static final String[] XMLTAGS_VIDEO = {"VideoMetadataInfos","VideoMetadataInfo"};
    public static final String XMLTAG_RELEASEDATE = "ReleaseDate";
    public static final String XMLTAG_ID = "Id";
    public static final String XMLTAG_WATCHED = "Watched";
    public static final String VALUE_WATCHED = (new Integer(Video.FL_WATCHED)).toString();


    private static final String TAG = "VideoDbBuilder";

    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */
    public VideoDbBuilder() {

    }

    public VideoDbBuilder(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull
    List<ContentValues> fetch(String url, int phase)
            throws IOException, XmlPullParserException {
        XmlNode videoData = XmlNode.fetch(url,null);
        return buildMedia(videoData, phase);
    }

    /**
     * Takes the contents of an XML object and populates the database
     * @param xmlFull The XML object of videos
     * @param phase 0 for recordings, 1 for videos
     */
    public List<ContentValues> buildMedia(XmlNode xmlFull, int phase) throws IOException, XmlPullParserException {
        List<ContentValues> videosToInsert = new ArrayList<>();
        String baseUrl = XmlNode.mythApiUrl(null);
        String [] tagsProgram;
        String tagRecordedId;
        if (phase == 0) {  //Recordings
            tagsProgram = XMLTAGS_PROGRAM;
            tagRecordedId = XMLTAG_RECORDEDID;
        }
        else {  //Videos
            tagsProgram = XMLTAGS_VIDEO;
            tagRecordedId = XMLTAG_ID;
        }
        XmlNode programNode = null;
        for (;;) {
            if (programNode == null)
                programNode = xmlFull.getNode(tagsProgram,0);
            else
                programNode = programNode.getNextSibling();
            if (programNode == null)
                break;
            XmlNode recordingNode;
            String recGroup;
            String storageGroup;
            String channel;
            String airdate;
            String starttime;
            long duration = 0;
            String progflags;
            String recordedid = null;
            if (phase == 0) { // Recordings
                String fileSize = programNode.getString(XMLTAG_FILESIZE);
                // Skip dummy LiveTV entry
                if ("0".equals(fileSize))
                    continue;
                recordingNode = programNode.getNode(XMLTAG_RECORDING);
                recordedid = recordingNode.getString(tagRecordedId);
                recGroup = recordingNode.getString(XMLTAG_RECGROUP);
                if (recGroup == null || recGroup.length() == 0)
                    recGroup = "Default";
                storageGroup = recordingNode.getString(XMLTAG_STORAGEGROUP);
                channel = programNode.getString(XMLTAGS_CHANNELNAME);
                airdate = programNode.getString(XMLTAG_AIRDATE);
                starttime = programNode.getString(XMLTAG_STARTTIME);
                // 2018-05-23T00:00:00Z
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");

                String startTS = recordingNode.getString(XMLTAG_STARTTS);
                String endTS = recordingNode.getString(XMLTAG_ENDTS);
                long startTimeSecs = 0;
                try {
                    Date dateStart = format.parse(startTS+"+0000");
                    Date dateEnd = format.parse(endTS+"+0000");
                    startTimeSecs = dateStart.getTime();
                    duration = (dateEnd.getTime() - startTimeSecs);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // if airdate missing default it to starttime.
                if (starttime != null && airdate == null
                    && startTimeSecs != 0) {
                    TimeZone tz = TimeZone.getDefault();
                    startTimeSecs += tz.getOffset(startTimeSecs);
                    airdate = dbFormat.format(new Date(startTimeSecs));
                }
                progflags = programNode.getString(XMLTAG_PROGFLAGS);
            }
            else { // Videos
                recordingNode = programNode;
                recGroup = null;
                storageGroup = "Videos";
                channel = null;
                airdate = programNode.getString(XMLTAG_RELEASEDATE);
                if (airdate != null && airdate.length() > 10)
                    airdate = programNode.getString(XMLTAG_RELEASEDATE).substring(0,10);
                starttime = null;
                String watched = programNode.getString(XMLTAG_WATCHED);
                if ("true".equals(watched))
                    progflags = VALUE_WATCHED;
                else
                    progflags = "0";
            }
            String title = programNode.getString(XMLTAG_TITLE);
            String subtitle = programNode.getString(XMLTAG_SUBTITLE);
            String description = programNode.getString(XMLTAG_DESCRIPTION);
            String videoFileName = recordingNode.getString(XMLTAG_FILENAME);
            String videoUrl = baseUrl +  "/Content/GetFile?StorageGroup="
                    + storageGroup + "&FileName=/" + URLEncoder.encode(videoFileName,"UTF-8");
            String coverArtUrl = null;
            String fanArtUrl = null;
            XmlNode artInfoNode = null;
            for (;;) {
                if (artInfoNode == null)
                    artInfoNode = programNode.getNode(XMLTAGS_ARTINFO,0);
                else
                    artInfoNode = artInfoNode.getNextSibling();
                if (artInfoNode == null)
                    break;
                String artType = artInfoNode.getString(XMLTAG_ARTTYPE);
                String artUrl = baseUrl + artInfoNode.getString(XMLTAG_ARTURL);
                int equ = artUrl.lastIndexOf('=');
                if (equ > 0) {
                    String fileName = artUrl.substring(equ + 1);
                    if (fileName.length() > 0 && fileName.charAt(0) == '/')
                        artUrl = artUrl.substring(0, equ + 1) + URLEncoder.encode(fileName, "UTF-8");
                }
                if ("coverart".equals(artType))
                    coverArtUrl = artUrl;
                else if ("fanart".equals(artType))
                    fanArtUrl = artUrl;
            }

            String prodYear = null;
            if (airdate != null)
                prodYear = airdate.substring(0,4);
            else if (starttime != null)
                prodYear = starttime.substring(0,4);

            String cardImageURL;
            String dbFileName = null;
            if (phase==0) { // Recordings
                cardImageURL =   baseUrl + "/Content/GetPreviewImage?Format=png&RecordedId=" + recordedid;
            }
            else { // Videos
                dbFileName = videoFileName;
                cardImageURL = coverArtUrl;
            }
            String season = programNode.getString(XMLTAG_SEASON);
            String episode = programNode.getString(XMLTAG_EPISODE);

            if (title == null || title.length() == 0)
                title = " ";
            if (subtitle == null || subtitle.length() == 0)
                subtitle = " ";
            if (description == null || description.length() == 0)
                description = " ";

            ContentValues videoValues = new ContentValues();
            videoValues.put(VideoContract.VideoEntry.COLUMN_TITLE, title);
            videoValues.put(VideoContract.VideoEntry.COLUMN_SUBTITLE, subtitle);
            videoValues.put(VideoContract.VideoEntry.COLUMN_DESC, description);
            videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
            videoValues.put(VideoContract.VideoEntry.COLUMN_FILENAME, dbFileName);
            videoValues.put(VideoContract.VideoEntry.COLUMN_CARD_IMG, cardImageURL);
            videoValues.put(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL, fanArtUrl);
            videoValues.put(VideoContract.VideoEntry.COLUMN_CHANNEL, channel);
            videoValues.put(VideoContract.VideoEntry.COLUMN_AIRDATE, airdate);

            videoValues.put(VideoContract.VideoEntry.COLUMN_STARTTIME, starttime);
            videoValues.put(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR, prodYear);
            videoValues.put(VideoContract.VideoEntry.COLUMN_RECORDEDID, recordedid);
            videoValues.put(VideoContract.VideoEntry.COLUMN_STORAGEGROUP, storageGroup);
            videoValues.put(VideoContract.VideoEntry.COLUMN_RECGROUP, recGroup);
            videoValues.put(VideoContract.VideoEntry.COLUMN_SEASON, season);
            videoValues.put(VideoContract.VideoEntry.COLUMN_EPISODE, episode);

            videoValues.put(VideoContract.VideoEntry.COLUMN_CONTENT_TYPE, "video/mp4");
            videoValues.put(VideoContract.VideoEntry.COLUMN_DURATION, duration);
            if (mContext != null) {
                videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
                        mContext.getResources().getString(R.string.global_search));
            }
            videoValues.put(VideoContract.VideoEntry.COLUMN_PROGFLAGS, progflags);

            videosToInsert.add(videoValues);
        }
        return videosToInsert;
    }

}
