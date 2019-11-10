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

import org.mythtv.leanfront.model.Video;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Video video = (Video) item;

        if (video != null) {
            viewHolder.getTitle().setText(video.title);
            StringBuilder subtitle = new StringBuilder();
            // possible characters for watched - "👁" "⏿" "👀"
            int progflags = Integer.parseInt(video.progflags);
            if ((progflags & video.FL_WATCHED) != 0)
                subtitle.append("\uD83D\uDC41");
            if (video.season != null && video.season.compareTo("0") > 0) {
                subtitle.append('S').append(video.season).append('E').append(video.episode)
                        .append(' ');
            }
            subtitle.append(video.subtitle);
            viewHolder.getSubtitle().setText(subtitle);
            StringBuilder description = new StringBuilder();

            // 2018-05-23T00:00:00Z
            try {
                // Date Recorded
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Date date = dbFormat.parse(video.starttime+"+0000");
                DateFormat outFormat = DateFormat.getDateInstance();
                String recDate = outFormat.format(date);
                description.append(recDate);
                // Length of recording
                long duration = Long.parseLong(video.duration, 10);
                duration = duration / 60000;
                description.append(", " + duration + " minutes");
                // Original Air date
                dbFormat = new SimpleDateFormat("yyyy-MM-dd");
                date = dbFormat.parse(video.airdate);
                String origDate = outFormat.format(date);
                if (!origDate.equals(recDate))
                    description.append("   [" + outFormat.format(date) + "]");
                description.append('\n');
            } catch (Exception e) {
                e.printStackTrace();
            }
            description.append(video.description);
            viewHolder.getBody().setText(description);
        }
    }
}
