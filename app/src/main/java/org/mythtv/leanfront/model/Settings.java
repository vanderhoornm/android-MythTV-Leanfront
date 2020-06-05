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

    public static int getInt(String key) {
        String str = mSingleton.mPrefs.getString(key, "").trim();
        if (str.length() == 0)
            return 0;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void setDefaults(Context context) {
        mPrefs =  PreferenceManager.getDefaultSharedPreferences (context);
        mEditor = mPrefs.edit();
        String str;
        str = mPrefs.getString("pref_http_port", "6544");
        mEditor.putString("pref_http_port",str);
        str = mPrefs.getString("pref_bookmark", "mythtv");
        mEditor.putString("pref_bookmark",str);
        str = mPrefs.getString("pref_skip_fwd", "60");
        mEditor.putString("pref_skip_fwd",str);
        str = mPrefs.getString("pref_skip_back", "20");
        mEditor.putString("pref_skip_back",str);
        str = mPrefs.getString("pref_seq", "rectime");
        mEditor.putString("pref_seq",str);
        str = mPrefs.getString("pref_seq_ascdesc", "asc");
        mEditor.putString("pref_seq_ascdesc",str);
        str = mPrefs.getString("pref_audio", "auto");
        mEditor.putString("pref_audio",str);
        str = mPrefs.getString("pref_arrow_jump", "false");
        mEditor.putString("pref_arrow_jump",str);
        str = mPrefs.getString("pref_jump", "5");
        mEditor.putString("pref_jump",str);
        str = mPrefs.getString("pref_livetv_duration", "60");
        mEditor.putString("pref_livetv_duration",str);
        if (android.os.Build.VERSION.SDK_INT >= 23)
            str = mPrefs.getString("pref_framerate_match", "false");
        else
            str = "false";
        mEditor.putString("pref_framerate_match",str);
        mEditor.apply();
    }
}
