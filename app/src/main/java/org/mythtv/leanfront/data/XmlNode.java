package org.mythtv.leanfront.data;

import android.content.SharedPreferences;
import android.util.Log;
import android.util.Xml;

import androidx.preference.PreferenceManager;

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
                    Log.e(TAG, "XML feed closed", e);
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

    public static String mythApiUrl(String params) throws IOException, XmlPullParserException {
        MainActivity main = MainActivity.getContext();
        if (main == null)
            return null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (main);
        String backend = prefs.getString("pref_backend", null);
        String port = prefs.getString("pref_http_port", "6544");
        String url = "http://" + backend + ":" + port;
        if (params != null)
            url = url + params;
        return url;
    }


}