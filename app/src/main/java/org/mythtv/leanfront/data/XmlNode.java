package org.mythtv.leanfront.data;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
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
}