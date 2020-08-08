package org.mythtv.leanfront.model;

import android.util.Log;

import org.mythtv.leanfront.data.XmlNode;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Program {
    public int chanId;
    public Date startTime;      // Start time of show
    public Date endTime;        // End time of show
    public String title;
    public String subTitle;
    public int season;
    public int episode;
    public String recordingStatus;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");
    private static final String TAG = "lfe";
    private static final String CLASS = "Program";


    public Program(XmlNode programNode) {
        try {
            String chanIdStr = programNode.getNode("Channel").getString("ChanId");
            chanId = Integer.parseInt(chanIdStr);
            startTime = dateFormat.parse(programNode.getString("StartTime") + "+0000");
            endTime = dateFormat.parse(programNode.getString("EndTime") + "+0000");
            title = programNode.getString("Title");
            subTitle = programNode.getString("SubTitle");
            season = Integer.parseInt(programNode.getString("Season"));
            episode = Integer.parseInt(programNode.getString("Episode"));
            recordingStatus = programNode.getNode("Recording").getString("Status");
            if ("Unknown".equals(recordingStatus))
                recordingStatus = null; // save storage
        } catch (Exception e) {
            Log.e(TAG, CLASS + " Exception parsing program.", e);
        }
    }

}
