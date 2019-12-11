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
