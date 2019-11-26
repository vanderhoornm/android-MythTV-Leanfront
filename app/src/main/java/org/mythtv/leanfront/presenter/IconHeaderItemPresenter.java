/*
 * Copyright (c) 2016 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.ui.LeanbackActivity;
import org.mythtv.leanfront.ui.MainActivity;
import org.mythtv.leanfront.ui.MainFragment;
import org.mythtv.leanfront.model.MyHeaderItem;
import org.mythtv.leanfront.ui.SettingsActivity;

;

public class IconHeaderItemPresenter extends RowHeaderPresenter {

    private float mUnselectedAlpha;
    private MyHeaderItem headerItem;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        mUnselectedAlpha = viewGroup.getResources()
                .getFraction(R.fraction.lb_browse_header_unselect_alpha, 1, 1);
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.icon_header_item, null);
        view.setAlpha(mUnselectedAlpha); // Initialize icons to be at half-opacity.

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        headerItem = (MyHeaderItem) ((ListRow) item).getHeaderItem();
        View rootView = viewHolder.view;
        rootView.setFocusable(true);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        Drawable icon;
        switch (headerItem.getItemType()) {
            case MainFragment.TYPE_SETTINGS:
                icon = rootView.getResources().getDrawable(R.drawable.ic_settings, null);
                break;
            case MainFragment.TYPE_VIDEODIR:
            case MainFragment.TYPE_VIDEODIR_ALL:
                icon = rootView.getResources().getDrawable(R.drawable.ic_folder, null);
                break;
            default:
                icon = rootView.getResources().getDrawable(R.drawable.ic_voicemail, null);
        }
        MyListener listener = new MyListener();
        setOnClickListener(viewHolder,listener);
        iconView.setImageDrawable(icon);

        TextView label = (TextView) rootView.findViewById(R.id.header_label);
        label.setText(headerItem.getName());
        label.setTextColor(rootView.getResources().getColor(R.color.header_text));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // no op
    }

    // TODO: This is a temporary fix. Remove me when leanback onCreateViewHolder no longer sets the
    // mUnselectAlpha, and also assumes the xml inflation will return a RowHeaderView.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        holder.view.setAlpha(mUnselectedAlpha + holder.getSelectLevel() *
                (1.0f - mUnselectedAlpha));
    }

    private class MyListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            Intent intent;
            int type = headerItem.getItemType();
            switch (type) {
                case MainFragment.TYPE_SETTINGS:
                    intent = new Intent(context, SettingsActivity.class);
                    break;
                case MainFragment.TYPE_RECGROUP:
                case MainFragment.TYPE_TOP_ALL:
                    intent = new Intent(context, MainActivity.class);
                    intent.putExtra(MainFragment.KEY_TYPE,MainFragment.TYPE_RECGROUP);
                    intent.putExtra(MainFragment.KEY_BASENAME,headerItem.getName());
                    break;
                case MainFragment.TYPE_VIDEODIR_ALL:
                    intent = new Intent(context, MainActivity.class);
                    intent.putExtra(MainFragment.KEY_TYPE,MainFragment.TYPE_VIDEODIR);
                    intent.putExtra(MainFragment.KEY_BASENAME,headerItem.getName());
                    break;
                default:
                    return;
            }
            Bundle bundle =
                    ActivityOptionsCompat.makeSceneTransitionAnimation((Activity)context)
                            .toBundle();
            context.startActivity(intent, bundle);
        }
    }
}
