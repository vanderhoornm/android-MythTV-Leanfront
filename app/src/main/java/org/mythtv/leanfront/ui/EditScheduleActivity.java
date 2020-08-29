/*
 * Copyright (c) 2016 The Android Open Source Project
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

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;

import org.mythtv.leanfront.data.AsyncBackendCall;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.Video;

import java.util.ArrayList;
import java.util.Date;


public class EditScheduleActivity extends FragmentActivity implements AsyncBackendCall.OnBackendCallListener {

    private EditScheduleFragment mEditFragment;

    public static final String CHANID = "CHANID";
    public static final String STARTTIME = "STARTTIME";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int chanId = getIntent().getIntExtra(EditScheduleActivity.CHANID, 0);
        Date startTime = (Date) getIntent().getSerializableExtra(EditScheduleActivity.STARTTIME);
        AsyncBackendCall call = new AsyncBackendCall(null, 0L, false,
                this);
        call.setId(chanId);
        call.setStartTime(startTime);
        call.execute(
                Video.ACTION_GETPROGRAMDETAILS,
                Video.ACTION_GETRECORDSCHEDULELIST,
                Video.ACTION_GETPLAYGROUPLIST,
                Video.ACTION_GETRECGROUPLIST,
                Video.ACTION_GETRECSTORAGEGROUPLIST,
                Video.ACTION_GETINPUTLIST,
                Video.ACTION_GETRECRULEFILTERLIST);
    }

    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        int [] tasks = taskRunner.getTasks();
        switch (tasks[0]) {
            case Video.ACTION_GETPROGRAMDETAILS:
                ArrayList<XmlNode> resultsList = taskRunner.getXmlResults();
                GuidedStepSupportFragment.addAsRoot(this,
                     mEditFragment = new EditScheduleFragment(resultsList), android.R.id.content);
                break;

        }

    }

    @Override
    public void onBackPressed() {
        if (mEditFragment.canEnd())
            super.onBackPressed();
    }

}
