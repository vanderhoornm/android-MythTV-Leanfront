package org.mythtv.leanfront.data;

import org.mythtv.leanfront.model.Settings;
import org.mythtv.leanfront.model.Video;

import java.util.HashMap;

// Singleton class to cache frequently used backend data
public class BackendCache implements AsyncBackendCall.OnBackendCallListener {
    private static BackendCache singleton;

    // Values from settings
    public String sBackendIP = "";
    public String sMainPort = "";

    // Values from wsdl
    public boolean canUpdateRecGroup = false;

    // Value from AsyncBackendCall
    public long mTimeAdjustment = 0;
    public int mythTvVersion = 0;
    // This flag will be set true during refresh if it is found that we are on a
    // backend that supports the LastPlayPos APIs (V32 or later).
    public boolean supportLastPlayPos;

    // Values from XmlNode
    public HashMap<String, String> sHostMap = new HashMap<>();
    public boolean isConnected = false;


    private BackendCache() {
        sBackendIP = Settings.getString("pref_backend");
        sMainPort = Settings.getString("pref_http_port");

        canUpdateRecGroup = false;
        AsyncBackendCall call = new AsyncBackendCall(null, this);
        call.execute(Video.ACTION_DVR_WSDL, Video.ACTION_BACKEND_INFO);
    }

    public static BackendCache getInstance() {
        if (singleton == null)
            singleton = new BackendCache();
        return singleton;
    }

    public static BackendCache flush() {
        singleton = new BackendCache();
        return singleton;
    }

    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        if (taskRunner == null)
            return;
        int [] tasks = taskRunner.getTasks();
        XmlNode xml = taskRunner.getXmlResult();
        switch (tasks[0]) {
            case Video.ACTION_DVR_WSDL:
                // Check if the UpdateRecordedMetadata method takes the RecGroup parameter
                XmlNode recGroupNode = xml.getNode(new String[]{"types", "schema"}, 1);
                if (recGroupNode != null)
                    recGroupNode = recGroupNode.getNode
                            (new String[]{"UpdateRecordedMetadata", "complexType", "sequence", "RecGroup"}, 0);
                if (recGroupNode != null)
                    canUpdateRecGroup = true;
                else
                    canUpdateRecGroup = false;
                break;
        }
    }
}
