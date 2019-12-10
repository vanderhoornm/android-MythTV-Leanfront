package org.mythtv.leanfront.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Settings {

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private static Settings mSingleton;

    private Settings() {

    }

    public static void init(Context context) {
        if (mSingleton != null)
            return;
        mSingleton = new Settings();
        mSingleton.setDefaults(context);
    }

    public static SharedPreferences.Editor getEditor() {
        return mSingleton.mEditor;
    }

    public static String getString(String key) {
        return mSingleton.mPrefs.getString(key, "");
    }

    private void setDefaults(Context context) {
        mPrefs =  PreferenceManager.getDefaultSharedPreferences (context);
        mEditor = mPrefs.edit();
        String str;
        str = mPrefs.getString("pref_http_port", "6544");
        mEditor.putString("pref_http_port",str);
        str = mPrefs.getString("pref_bookmark", "mythtv");
        mEditor.putString("pref_bookmark",str);
        str = mPrefs.getString("pref_fps", "30");
        mEditor.putString("pref_fps",str);
        str = mPrefs.getString("pref_skip_fwd", "60");
        mEditor.putString("pref_skip_fwd",str);
        str = mPrefs.getString("pref_skip_back", "20");
        mEditor.putString("pref_skip_back",str);
        str = mPrefs.getString("pref_seq", "rectime");
        mEditor.putString("pref_seq",str);
        str = mPrefs.getString("pref_seq_ascdesc", "asc");
        mEditor.putString("pref_seq_ascdesc",str);
        mEditor.apply();
    }
}
