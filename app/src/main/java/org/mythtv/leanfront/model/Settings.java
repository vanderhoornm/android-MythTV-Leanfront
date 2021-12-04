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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.mythtv.leanfront.MyApplication;

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
        mSingleton.mPrefs =  PreferenceManager.getDefaultSharedPreferences (context);
        mSingleton.mEditor = mSingleton.mPrefs.edit();
    }

    public static SharedPreferences.Editor getEditor() {
        return mSingleton.mEditor;
    }

    // Omit the "pref_" prefix when calling this
    // This adds prefxxxx_ to string where xxxx is the group
    // null group or "Default" = empty string
    // If group value not found return default value.
    public static String getString(String key, @Nullable String group) {
        if (key.startsWith("pref_"))
            key = key.substring(5);
        if (mSingleton != null && mSingleton.mPrefs != null) {
            if (group == null || group.equals("Default"))
                group = "";
            String actualKey = "pref" + group + "_" + key;
            String result = mSingleton.mPrefs.getString(actualKey, null);
            if (result == null) {
                actualKey = "sdef" + "_" + key;
                Context context = MyApplication.getAppContext();
                Resources res = context.getResources();
                int resId = res.getIdentifier(actualKey, "string", context.getPackageName());
                if (resId != 0)
                    result = res.getString(resId);
            }
            if (result == null)
                result = "";
            return result;
        }
        else
            return "";
    }

    public static String getString(String key) {
        return getString(key,null);
    }

    public static int getInt(String key, @Nullable String group) {
        String str = getString(key, group).trim();
        if (str.length() == 0)
            return 0;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getInt(String key) {
        return getInt(key,null);
    }

    public static void putString(String key, @Nullable String group, String value) {
        if (key.startsWith("pref_"))
            key = key.substring(5);
        if (mSingleton.mEditor != null) {
            if (group == null || group.equals("Default"))
                group = "";
            String actualKey = "pref" + group + "_" + key;
            mSingleton.mEditor.putString(actualKey,value);
        }
    }

    public static void putString(String key, String value) {
        putString(key, null, value);
    }
}
