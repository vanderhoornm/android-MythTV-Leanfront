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
import android.content.SharedPreferences;
import android.media.Rating;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import android.util.Log;

import org.mythtv.leanfront.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The VideoDbBuilder is used to grab a JSON file from a server and parse the data
 * to be placed into a local database
 */
public class VideoDbBuilder {
    public static final String TAG_MEDIA = "videos";
    public static final String TAG_GOOGLE_VIDEOS = "googlevideos";
    public static final String TAG_CATEGORY = "category";
    public static final String TAG_STUDIO = "studio";
    public static final String TAG_SOURCES = "sources";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_CARD_THUMB = "card";
    public static final String TAG_BACKGROUND = "background";
    public static final String TAG_TITLE = "title";

    private static final String[] XMLTAGS_PROGRAM = {"Programs","Program"};
    private static final String[] XMLTAGS_ARTINFO = {"Artwork","ArtworkInfos","ArtworkInfo"};
    private static final String[] XMLTAGS_CHANNELNAME = {"Channel","ChannelName"};

    public static final String XMLTAG_RECORDING = "Recording";
    public static final String XMLTAG_CATEGORY = "Category";
    public static final String XMLTAG_TITLE = "Title";
    public static final String XMLTAG_DESCRIPTION = "Description";
    public static final String XMLTAG_STORAGEGROUP = "StorageGroup";
    public static final String XMLTAG_FILENAME = "FileName";
    public static final String XMLTAG_ARTTYPE = "Type";
    public static final String XMLTAG_ARTURL = "URL";
    public static final String XMLTAG_SUBTITLE = "SubTitle";
    public static final String XMLTAG_STARTTIME = "StartTime";
    public static final String XMLTAG_AIRDATE = "Airdate";

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
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull List<ContentValues> fetch(String url)
            throws IOException, JSONException, XmlPullParserException {
        // JSONObject videoData = fetchJSON(url);
        XmlNode videoData = fetchXML(url);
        return buildMedia(videoData);
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
/*
    public List<ContentValues> buildMedia(JSONObject jsonObj) throws JSONException {

        JSONArray categoryArray = jsonObj.getJSONArray(TAG_GOOGLE_VIDEOS);
        List<ContentValues> videosToInsert = new ArrayList<>();

        for (int i = 0; i < categoryArray.length(); i++) {
            JSONArray videoArray;

            JSONObject category = categoryArray.getJSONObject(i);
            String categoryName = category.getString(TAG_CATEGORY);
            videoArray = category.getJSONArray(TAG_MEDIA);

            for (int j = 0; j < videoArray.length(); j++) {
                JSONObject video = videoArray.getJSONObject(j);

                // If there are no URLs, skip this video entry.
                JSONArray urls = video.optJSONArray(TAG_SOURCES);
                if (urls == null || urls.length() == 0) {
                    continue;
                }

                String title = video.optString(TAG_TITLE);
                String description = video.optString(TAG_DESCRIPTION);
                String videoUrl = (String) urls.get(0); // Get the first video only.
                String bgImageUrl = video.optString(TAG_BACKGROUND);
                String cardImageUrl = video.optString(TAG_CARD_THUMB);
                String studio = video.optString(TAG_STUDIO);

                ContentValues videoValues = new ContentValues();
                videoValues.put(VideoContract.VideoEntry.COLUMN_CATEGORY, categoryName);
                videoValues.put(VideoContract.VideoEntry.COLUMN_NAME, title);
                videoValues.put(VideoContract.VideoEntry.COLUMN_DESC, description);
                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_CARD_IMG, cardImageUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL, bgImageUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_STUDIO, studio);

                // Fixed defaults.
                videoValues.put(VideoContract.VideoEntry.COLUMN_CONTENT_TYPE, "video/mp4");
//                videoValues.put(VideoContract.VideoEntry.COLUMN_IS_LIVE, false);
//                videoValues.put(VideoContract.VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG, "2.0");
                videoValues.put(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR, 2014);
                videoValues.put(VideoContract.VideoEntry.COLUMN_DURATION, 0);
//                videoValues.put(VideoContract.VideoEntry.COLUMN_RATING_STYLE,
//                        Rating.RATING_5_STARS);
//                videoValues.put(VideoContract.VideoEntry.COLUMN_RATING_SCORE, 3.5f);
                if (mContext != null) {
//                    videoValues.put(VideoContract.VideoEntry.COLUMN_PURCHASE_PRICE,
//                            mContext.getResources().getString(R.string.buy_2));
//                    videoValues.put(VideoContract.VideoEntry.COLUMN_RENTAL_PRICE,
//                            mContext.getResources().getString(R.string.rent_2));
                    videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
                            mContext.getResources().getString(R.string.global_search));
                }

                // TODO: Get these dimensions.
//                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_WIDTH, 1280);
//                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_HEIGHT, 720);

                videosToInsert.add(videoValues);
            }
        }
        return videosToInsert;
    }
*/
    /**
     * Takes the contents of an XML object and populates the database
     * @param xmlFull The XML object of videos
     */
    public List<ContentValues> buildMedia(XmlNode xmlFull) {

        List<ContentValues> videosToInsert = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (mContext);
        String backend = prefs.getString("pref_backend", null);
        String port = prefs.getString("pref_http_port", "6544");
        String baseUrl = "http://" + backend + ":" + port;
        String baseVideoUrl = baseUrl + "/Content/GetFile?StorageGroup=";
        String defaultImage = "android.resource://org.mythtv.leanfront/" + R.drawable.movie;
        XmlNode programNode = null;
        for (;;) {
            if (programNode == null)
                programNode = xmlFull.getNode(XMLTAGS_PROGRAM,0);
            else
                programNode = programNode.getNextSibling();
            if (programNode == null)
                break;
            XmlNode recordingNode = programNode.getNode(XMLTAG_RECORDING);
//            String categoryName = programNode.getString(XMLTAG_CATEGORY);
            String title = programNode.getString(XMLTAG_TITLE);
            String subtitle = programNode.getString(XMLTAG_SUBTITLE);
            if (subtitle == null || subtitle.length()==0)
                subtitle = title;
            String description = programNode.getString(XMLTAG_DESCRIPTION);
            String storageGroup = recordingNode.getString(XMLTAG_STORAGEGROUP);
            String filename = recordingNode.getString(XMLTAG_FILENAME);
            String videoUrl = baseVideoUrl + storageGroup + "&FileName=/" + filename;
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
            String prodYear = null;
            if (airdate != null)
                prodYear = airdate.substring(0,4);
            else if (starttime != null)
                starttime = starttime.substring(0,4);
            // card image video + .png
            String cardImageURL = videoUrl + ".png";

            if (title == null || title.length() == 0)
                title = "X";
            if (subtitle == null || subtitle.length() == 0)
                subtitle = "X";
            if (description == null || description.length() == 0)
                description = "X";
            if (videoUrl == null || videoUrl.length() == 0)
                videoUrl = "X";
            if (coverArtUrl == null || coverArtUrl.length() == 0)
                coverArtUrl = defaultImage;
            if (fanArtUrl == null || fanArtUrl.length() == 0)
                fanArtUrl = defaultImage;
            if (channel == null || channel.length() == 0)
                channel = "X";

            ContentValues videoValues = new ContentValues();
            videoValues.put(VideoContract.VideoEntry.COLUMN_TITLE, title);
            videoValues.put(VideoContract.VideoEntry.COLUMN_SUBTITLE, subtitle);
            videoValues.put(VideoContract.VideoEntry.COLUMN_DESC, description);
            videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
            videoValues.put(VideoContract.VideoEntry.COLUMN_CARD_IMG, cardImageURL);
            videoValues.put(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL, fanArtUrl);
            videoValues.put(VideoContract.VideoEntry.COLUMN_STUDIO, channel);
            videoValues.put(VideoContract.VideoEntry.COLUMN_AIRDATE, airdate);

            videoValues.put(VideoContract.VideoEntry.COLUMN_STARTTIME, starttime);
            videoValues.put(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR, prodYear);

            // TODO: Sort these out
            videoValues.put(VideoContract.VideoEntry.COLUMN_CONTENT_TYPE, "video/mp4");
//            videoValues.put(VideoContract.VideoEntry.COLUMN_IS_LIVE, false);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG, "2.0");
            videoValues.put(VideoContract.VideoEntry.COLUMN_DURATION, 0);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_RATING_STYLE,
//                    Rating.RATING_5_STARS);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_RATING_SCORE, 3.5f);
            if (mContext != null) {
//                videoValues.put(VideoContract.VideoEntry.COLUMN_PURCHASE_PRICE,
//                        mContext.getResources().getString(R.string.buy_2));
//                videoValues.put(VideoContract.VideoEntry.COLUMN_RENTAL_PRICE,
//                        mContext.getResources().getString(R.string.rent_2));
                videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
                        mContext.getResources().getString(R.string.global_search));
            }

            // TODO: Get these dimensions.
//            videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_WIDTH, 1280);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_HEIGHT, 720);

            videosToInsert.add(videoValues);
        }
        return videosToInsert;
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject fetchJSON(String urlString) throws JSONException, IOException {
        BufferedReader reader = null;
        java.net.URL url = new java.net.URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
                    "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } finally {
            urlConnection.disconnect();
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "JSON feed closed", e);
                }
            }
        }
    }

    /**
     * Fetch XML object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws XmlPullParserException
     * @throws IOException
     */
    private XmlNode fetchXML(String urlString) throws XmlPullParserException, IOException {
        XmlNode ret = null;
        URL url = null;
        HttpURLConnection urlConnection = null;
        InputStream is = null;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            is = urlConnection.getInputStream();
            ret = XmlNode.parseStream(is);
        } finally {
            urlConnection.disconnect();
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "XML feed closed", e);
                }
            }
        }
        return ret;
    }

}
