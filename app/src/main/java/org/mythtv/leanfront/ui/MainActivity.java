/*
 * Copyright (c) 2014 The Android Open Source Project
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

package org.mythtv.leanfront.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;

import org.mythtv.leanfront.R;

/*
 * MainActivity class that loads MainFragment.
 */
public class MainActivity extends LeanbackActivity {

    static MainActivity context = null;
    MainFragment mainFragment = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (context == null)
            context = this;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getString("pref_backend",null) == null) {
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
        context = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    static public void startMainLoader() {
        MainActivity main = getContext();
        if (main != null) {
            main.runOnUiThread(new Runnable() {
                public void run() {
                    MainActivity main = getContext();
                    if (main != null)
                        main.getMainFragment().startLoader();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        if (mainFragment.getType() == MainFragment.TYPE_TOPLEVEL)
//            System.exit(0);
    }
}
