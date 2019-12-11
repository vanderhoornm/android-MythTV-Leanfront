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

import android.content.res.Resources;

import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;

import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.ui.MainFragment;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.default_background);
        mSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.selected_background);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Video video = (Video) item;

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(video.title);
        int defaultIcon;
        switch (video.type) {
            case MainFragment.TYPE_VIDEODIR:
                defaultIcon = R.drawable.im_folder;
                break;
            case MainFragment.TYPE_REFRESH:
                defaultIcon = R.drawable.im_refresh;
                break;
            default:
                defaultIcon = R.drawable.im_movie;
        }
        StringBuilder subtitle = new StringBuilder();
        int progflags = Integer.parseInt(video.progflags);
        // possible characters for watched - "👁" "⏿" "👀"
        if ((progflags & Video.FL_WATCHED) != 0)
            subtitle.append("\uD83D\uDC41");
        if (video.season != null && video.season.compareTo("0") > 0) {
            subtitle.append('S').append(video.season).append('E').append(video.episode)
                    .append(' ');
        }
        subtitle.append(video.subtitle);
        cardView.setContentText(subtitle);
        // Set card size from dimension resources.
        Resources res = cardView.getResources();
        int width = res.getDimensionPixelSize(R.dimen.card_width);
        int height = res.getDimensionPixelSize(R.dimen.card_height);
        cardView.setMainImageDimensions(width, height);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(defaultIcon);

        if (video.cardImageUrl == null) {
            Glide.with(cardView.getContext())
                    .load(defaultIcon)
                    .apply(options)
                    .into(cardView.getMainImageView());
        }
        else
            Glide.with(cardView.getContext())
                    .load(video.cardImageUrl + "&time=" + System.currentTimeMillis())
                    .apply(options)
                    .into(cardView.getMainImageView());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
