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

import android.util.Log;
import android.util.Xml;

import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.ui.MainActivity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;


public class XmlNode {
    private static final String TAG = "XmlNode";

    private String name;
    private HashMap<String, XmlNode> childMap = new HashMap<String, XmlNode>();
    private String text = null;
    private XmlNode nextSibling;
    private static HashMap<String, String> sHostMap;
    private static String sBackendIP;

    private static String getIpAndPort(String hostname) throws IOException, XmlPullParserException {
        String backendIP = Settings.getString("pref_backend");
        String port = Settings.getString("pref_http_port");
        if (backendIP == null || port == null) {
            Log.e(TAG, "Backend port or IP address not specified");
            return null;
        }
        if (!backendIP.equals(sBackendIP)) {
            sBackendIP = backendIP;
            sHostMap = new HashMap<>();
        }
        if (hostname == null)
            return sBackendIP + ":" + port;
        String hostIpAndPort = sHostMap.get(hostname);
        if (hostIpAndPort == null) {
            String urlString = XmlNode.mythApiUrl(null,
                    "/Myth/GetSetting?Key=BackendServerAddr&HostName="
                            + hostname);
            XmlNode response = XmlNode.fetch(urlString, "POST");
            String hostIp = response.getString();
            urlString = XmlNode.mythApiUrl(null,
                    "/Myth/GetSetting?Key=BackendStatusPort&HostName="
                            + hostname);
            response = XmlNode.fetch(urlString, "POST");
            port = response.getString();
            hostIpAndPort = hostIp + ":" + port;
            sHostMap.put(hostname, hostIpAndPort);
        }
        return hostIpAndPort;
    }

    public static XmlNode parseStream(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        XmlNode ret = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG)
                ret = parseNode(parser);
            eventType = parser.next();
        }
        return ret;
    }

    // called on a START_TAG
    private static XmlNode parseNode(XmlPullParser parser) throws IOException, XmlPullParserException {
        XmlNode ret = new XmlNode();
        ret.name = parser.getName();
        int eventType = XmlPullParser.START_TAG;
        while (eventType != XmlPullParser.END_TAG) {
            eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                XmlNode child = parseNode(parser);
                XmlNode priorChild = ret.childMap.get(child.name);
                if (priorChild == null)
                    ret.childMap.put(child.name, child);
                else {
                    while (priorChild.nextSibling != null)
                        priorChild = priorChild.nextSibling;
                    priorChild.nextSibling = child;
                }
            } else if (eventType == XmlPullParser.TEXT) {
                ret.text = parser.getText();
            }
        }
        return ret;
    }

    /**
     * Fetch XML object from a given URL.
     *
     * @return the XmlNode representation of the response
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static XmlNode fetch(String urlString, String requestMethod)
            throws XmlPullParserException, IOException {
        XmlNode ret = null;
        URL url = null;
        HttpURLConnection urlConnection = null;
        InputStream is = null;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Cache-Control", "no-cache");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(30000);
            if (requestMethod != null)
                urlConnection.setRequestMethod(requestMethod);
            is = urlConnection.getInputStream();
            ret = XmlNode.parseStream(is);
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "XML feed closed: " + urlString, e);
                }
            }
        }
        return ret;
    }

    public XmlNode getNode(String[] tag, int index) {
        XmlNode node = this;
        for (String item : tag) {
            node = node.childMap.get(item);
            if (node == null)
                return null;
        }
        for (int ix = 0; ix<index; ix++) {
            node = node.nextSibling;
            if (node == null)
                return null;
        }
        return node;
    }

    public XmlNode getNode(String tag, int index) {
        String[] tags = {tag};
        return getNode(tags,index);
    }

    public XmlNode getNode(String tag) {
        String[] tags = {tag};
        return getNode(tags,0);
    }

    public XmlNode getNextSibling() {
        return nextSibling;
    }

    public String getString(String[] tag, int index) {
        XmlNode node = getNode(tag, index);
        if (node == null)
            return null;
        return node.text;
    }

    public String getString(String[] tag) {
        return getString(tag, 0);
    }

    public String getString(String tag) {
        String[] tags = {tag};
        return getString(tags, 0);
    }

    public String getString() { return text; }

    public static String mythApiUrl(String hostName, String params) throws IOException, XmlPullParserException {
        MainActivity main = MainActivity.getContext();
        if (main == null)
            return null;
        String ipAndPort = getIpAndPort(hostName);
        String url = "http://" + ipAndPort;
        if (params != null)
            url = url + params;
        return url;
    }

}