/*
 * Copyright (c) 2014 The Android Open Source Project
 * Copyright (c) 2019-2020 Peter Bennett
 *
 * Incorporates code from "Android TV Samples"
 * <https://github.com/android/tv-samples>
 * Modified by Peter Bennett
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

package org.mythtv.leanfront.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Settings;

/*
 * MainActivity class that loads MainFragment.
 */
public class MainActivity extends LeanbackActivity {

    static MainActivity context = null;
    MainFragment mainFragment = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.init(this);
        setContentView(R.layout.main);
        if (context == null)
            context = this;
        if (Settings.getString("pref_backend").length() == 0) {
            // This is the first time running the app, let's go to onboarding
            startActivity(new Intent(this, SettingsActivity.class));
        }
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag("main");
        if (fragment instanceof MainFragment) {
            mainFragment = (MainFragment) fragment;
        }
    }
    static public MainActivity getContext(){
        return context;
    }

    public MainFragment getMainFragment(){
        return mainFragment;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (context == this)
            context = null;
    }

    static public void startMainLoader() {
        MainActivity main = getContext();
        if (main != null) {
            main.runOnUiThread(new Runnable() {
                public void run() {
                    MainFragment frag = MainFragment.getActiveFragment();
                    if (frag != null)
                        frag.startLoader();
                }
            });
        }
    }

}
