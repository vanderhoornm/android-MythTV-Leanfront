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

package org.mythtv.leanfront.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.XmlNode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class EditScheduleFragment extends GuidedStepSupportFragment {

    private int mChanId;
    private Date mStartTime;
    private XmlNode mProgDetails;
    private XmlNode mRecordSchedule;
    private static DateFormat timeFormatter;
    private static DateFormat dateFormatter;
    private static DateFormat dayFormatter;


    public EditScheduleFragment(XmlNode progDetails, XmlNode recordSchedule) {
        mProgDetails = progDetails;
        mRecordSchedule = recordSchedule;
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        if (timeFormatter == null) {
            timeFormatter = android.text.format.DateFormat.getTimeFormat(getContext());
            dateFormatter = android.text.format.DateFormat.getLongDateFormat(getContext());
            dayFormatter = new SimpleDateFormat("EEE ");
        }
        Activity activity = getActivity();
        String title = mProgDetails.getString("Title");
        StringBuilder dateTime = new StringBuilder();
        dateTime.append(dayFormatter.format(mStartTime))
            .append(dateFormatter.format(mStartTime)).append(' ')
            .append(timeFormatter.format(mStartTime));
        StringBuilder details = new StringBuilder();
        String subTitle = mProgDetails.getString("SubTitle");
        if (subTitle != null)
            details.append(subTitle).append("\n");
        String desc = mProgDetails.getString("Description");
        if (desc != null)
            details.append(desc);
        Drawable icon = activity.getDrawable(R.drawable.ic_voicemail);
        return new GuidanceStylist.Guidance(title, details.toString(), dateTime.toString(), icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        mChanId = getActivity().getIntent().getIntExtra(EditScheduleActivity.CHANID, 0);
        mStartTime = (Date)getActivity().getIntent().getSerializableExtra(EditScheduleActivity.STARTTIME);
    }
}
