/*
 * Copyright (c) 2015 The Android Open Source Project
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

package org.mythtv.leanfront.presenter;

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.ui.MainActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {
    private ViewHolder mViewHolder;
    private Video mVideo;

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        mVideo = (Video) item;
        mViewHolder = viewHolder;
        setupDescription();
    }
    
    @SuppressLint("SimpleDateFormat")
    public void setupDescription() {
        if (mVideo != null) {
            mViewHolder.getTitle().setText(mVideo.title);
            Context context = mViewHolder.getBody().getContext();
            StringBuilder subtitle = new StringBuilder();
            // possible characters for watched - "👁" "⏿" "👀"
            int progflags = Integer.parseInt(mVideo.progflags);
            if ((progflags & Video.FL_WATCHED) != 0)
                subtitle.append("\uD83D\uDC41");
            if (mVideo.season != null && mVideo.season.compareTo("0") > 0) {
                subtitle.append('S').append(mVideo.season).append('E').append(mVideo.episode)
                        .append(' ');
            }
            subtitle.append(mVideo.subtitle);
            mViewHolder.getSubtitle().setText(subtitle);
            StringBuilder description = new StringBuilder();

            // 2018-05-23T00:00:00Z
            try {
                // Date Recorded
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                java.text.DateFormat outFormat = android.text.format.DateFormat.getMediumDateFormat(MainActivity.getContext());
                String recDate = null;
                if (mVideo.starttime != null) {
                    Date date = dbFormat.parse(mVideo.starttime + "+0000");
                    long dateMS = date.getTime();
                    TimeZone tz = TimeZone.getDefault();
                    dateMS += tz.getOffset(date.getTime());
                    recDate = outFormat.format(new Date(dateMS));
                    description.append(recDate);
                }
                // Length of recording
                long duration = Long.parseLong(mVideo.duration, 10);
                duration = duration / 60000;
                if (duration > 0) {
                    if (description.length() > 0)
                        description.append(", ");
                    description.append(duration).append(" ").append(context.getString(R.string.video_minutes));
                }
                // Channel
                if (mVideo.channel != null && mVideo.channel.length()>0)
                    description.append("  ").append(mVideo.channel);
                // Original Air date
                dbFormat = new SimpleDateFormat("yyyy-MM-dd");
                if (mVideo.airdate != null) {
                    if ("01-01".equals(mVideo.airdate.substring(5)))
                        description.append("   [" + mVideo.airdate.substring(0, 4) + "]");
                    else {
                        Date date = dbFormat.parse(mVideo.airdate);
                        String origDate = outFormat.format(date);
                        if (!Objects.equals(origDate,recDate))
                            description.append("   [" + outFormat.format(date) + "]");
                    }
                }
                description.append('\n');
            } catch (Exception e) {
                e.printStackTrace();
            }
            description.append(mVideo.description);
            mViewHolder.getBody().setText(description);
        }
    }
}
