package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.GuideSlot;

public class GuideCardView extends BaseCardView {
    public static final int COLOR_TIMESLOT = 0xff265990;
    public static final int COLOR_CHANNEL = 0xff202a49;
    public static final int COLOR_PROGRAM = 0xff673300;
    public static final int COLOR_WILLRECORD = 0xff005500;
    public static final int COLOR_WONTRECORD = 0xff671313;

    private int mType;
    public static final int TYPE_SMALL = 1;
    public static final int TYPE_LARGE = 2;

    public GuideCardView(Context context, int type) {
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

    public void updateUi(GuideSlot card) {
        TextView cardText = findViewById(R.id.card_text);
        TextView statusText = findViewById(R.id.card_text_status);
        int bgColor = Color.DKGRAY;
        if (card == null) {
            cardText.setText(null);
            setBackgroundColor(Color.DKGRAY);
        }
        else {
            cardText.setText(card.getGuideText(getContext()));
            String status = null;
            if (card.program != null && card.program.recordingStatus != null)
                status = card.program.recordingStatus;
            if (card.program2 != null && card.program2.recordingStatus != null)
                status = (status == null ? "(2)" : status + '/') + card.program2.recordingStatus;
            statusText.setText(status);
            if (card.cellType == GuideSlot.CELL_TIMESLOT)
                bgColor = COLOR_TIMESLOT;
            else if (card.cellType == GuideSlot.CELL_CHANNEL)
                bgColor = COLOR_CHANNEL;
            else if (card.program != null
                && (card.cellType == GuideSlot.CELL_PROGRAM
                    || card.cellType == GuideSlot.CELL_SEARCHRESULT)) {
                if ("WillRecord".equals(card.program.recordingStatus)
                    || card.program2 != null
                        && "WillRecord".equals(card.program2.recordingStatus))
                    bgColor = COLOR_WILLRECORD;
                else if (card.program.recordingStatus == null
                        && (card.program2 == null
                            || card.program2.recordingStatus == null))
                    bgColor = COLOR_PROGRAM;
                else
                    bgColor = COLOR_WONTRECORD;
            }
            setBackgroundColor(bgColor);
            statusText.setBackgroundColor(bgColor);
        }
    }

}
