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

import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;

import org.mythtv.leanfront.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
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
    List<ContentValues> fetch(String url)
            throws IOException, XmlPullParserException {
        XmlNode videoData = XmlNode.fetch(url,null);
        return buildMedia(videoData);
    }

    /**
     * Takes the contents of an XML object and populates the database
     * @param xmlFull The XML object of videos
     */
    public List<ContentValues> buildMedia(XmlNode xmlFull) throws IOException, XmlPullParserException {
        HashMap <String, HashSet<String>> filesOnServer = new HashMap <>();
        List<ContentValues> videosToInsert = new ArrayList<>();
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (mContext);
//        String backend = prefs.getString("pref_backend", null);
//        String port = prefs.getString("pref_http_port", "6544");
//        String baseUrl = "http://" + backend + ":" + port;
        String baseUrl = XmlNode.mythApiUrl(null);
        String defaultImage = "android.resource://org.mythtv.leanfront/" + R.drawable.background;
        XmlNode programNode = null;
        for (;;) {
            if (programNode == null)
                programNode = xmlFull.getNode(XMLTAGS_PROGRAM,0);
            else
                programNode = programNode.getNextSibling();
            if (programNode == null)
                break;
            XmlNode recordingNode = programNode.getNode(XMLTAG_RECORDING);
            String recGroup = recordingNode.getString(XMLTAG_RECGROUP);
//            if ("Deleted".equals(recGroup))
//                continue;
            String title = programNode.getString(XMLTAG_TITLE);
            String subtitle = programNode.getString(XMLTAG_SUBTITLE);
//            if (subtitle == null || subtitle.length()==0)
//                subtitle = title;
            String description = programNode.getString(XMLTAG_DESCRIPTION);
            String storageGroup = recordingNode.getString(XMLTAG_STORAGEGROUP);
            if (!filesOnServer.containsKey(storageGroup)) {
                String url =  baseUrl + "/Content/GetFileList?StorageGroup=" + storageGroup;
                XmlNode fileData = XmlNode.fetch(url,null);
                HashSet<String> sgFiles = new HashSet<>();
                filesOnServer.put(storageGroup,sgFiles);
                XmlNode fileNode = fileData.getNode("String",0);
                while (fileNode != null) {
                    String fileName = fileNode.getString();
                    if (fileName != null)
                        sgFiles.add(fileName);
                    fileNode = fileNode.getNextSibling();
                }
            }
            String videoFileName = recordingNode.getString(XMLTAG_FILENAME);
            String videoUrl = baseUrl +  "/Content/GetFile?StorageGroup="
                    + storageGroup + "&FileName=/" + videoFileName;
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
                if ("coverart".equals(artType))
                    coverArtUrl = artUrl;
                else if ("fanart".equals(artType))
                    fanArtUrl = artUrl;
            }
            String channel = programNode.getString(XMLTAGS_CHANNELNAME);
            String airdate = programNode.getString(XMLTAG_AIRDATE);
            String starttime = programNode.getString(XMLTAG_STARTTIME);

            // 2018-05-23T00:00:00Z
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");
            String startTS = recordingNode.getString(XMLTAG_STARTTS);
            String endTS = recordingNode.getString(XMLTAG_ENDTS);
            long duration = 0;
            try {
                Date dateStart = format.parse(startTS+"+0000");
                Date dateEnd = format.parse(endTS+"+0000");
                duration = (dateEnd.getTime() - dateStart.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String prodYear = null;
            if (airdate != null)
                prodYear = airdate.substring(0,4);
            else if (starttime != null)
                prodYear = starttime.substring(0,4);
            // card image video + .png
            HashSet<String> sgFiles = filesOnServer.get(storageGroup);
            String cardImageFile = videoFileName + ".png";
            boolean fileExists = false;
            if (sgFiles != null)
                fileExists = sgFiles.contains(cardImageFile);
            String cardImageURL;
            if (fileExists)
                cardImageURL = videoUrl + ".png";
            else
                cardImageURL = defaultImage;

            String recordedid = recordingNode.getString(XMLTAG_RECORDEDID);
            String season = programNode.getString(XMLTAG_SEASON);
            String episode = programNode.getString(XMLTAG_EPISODE);
            String progflags = programNode.getString(XMLTAG_PROGFLAGS);

            if (title == null || title.length() == 0)
                title = " ";
            if (subtitle == null || subtitle.length() == 0)
                subtitle = " ";
            if (description == null || description.length() == 0)
                description = " ";
            if (videoUrl == null || videoUrl.length() == 0)
                videoUrl = defaultImage;
            if (coverArtUrl == null || coverArtUrl.length() == 0)
                coverArtUrl = defaultImage;
            if (fanArtUrl == null || fanArtUrl.length() == 0)
                fanArtUrl = defaultImage;
            if (channel == null || channel.length() == 0)
                channel = " ";

            ContentValues videoValues = new ContentValues();
            videoValues.put(VideoContract.VideoEntry.COLUMN_TITLE, title);
            videoValues.put(VideoContract.VideoEntry.COLUMN_SUBTITLE, subtitle);
            videoValues.put(VideoContract.VideoEntry.COLUMN_DESC, description);
            videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
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

            // TODO: Sort these out
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
