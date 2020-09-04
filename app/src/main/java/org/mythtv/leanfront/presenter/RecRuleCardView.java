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


package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.RecordRule;

public class RecRuleCardView extends BaseCardView {
    public static final int COLOR_TIMESLOT = 0xff265990;
    public static final int COLOR_CHANNEL = 0xff202a49;
    public static final int COLOR_PROGRAM = 0xff673300;
    public static final int COLOR_WILLRECORD = 0xff005500;
    public static final int COLOR_WONTRECORD = 0xff671313;

    private int mType;
    public static final int TYPE_SMALL = 1;
    public static final int TYPE_LARGE = 2;

    public RecRuleCardView(Context context, int type) {
        super(context);
        mType = type;
        int layout;
        if (mType == TYPE_LARGE)
            layout = R.layout.text_card_large;
        else
            layout = R.layout.text_card;
        LayoutInflater.from(getContext()).inflate(layout, this);
        setFocusable(true);
    }

    public void updateUi(RecordRule card) {
        TextView cardText = findViewById(R.id.card_text);
        TextView statusText = findViewById(R.id.card_text_status);
        int bgColor = Color.DKGRAY;
        if (card == null) {
            cardText.setText(null);
            statusText.setText(null);
        }
        else {
            cardText.setText(card.getCardText(getContext()));
            statusText.setText(card.recordingStatus);
            if (card.recordingStatus == null) {
                if (card.inactive)
                    bgColor = COLOR_WONTRECORD;
                else
                    bgColor = COLOR_WILLRECORD;
            }
            else {
                if ("WillRecord".equals(card.recordingStatus)
                    || "Recording".equals(card.recordingStatus))
                    bgColor = COLOR_WILLRECORD;
                else
                    bgColor = COLOR_WONTRECORD;
            }
        }
        setBackgroundColor(bgColor);
        statusText.setBackgroundColor(bgColor);
    }
}
