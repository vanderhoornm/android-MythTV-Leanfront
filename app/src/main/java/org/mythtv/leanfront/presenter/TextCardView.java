package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.GuideSlot;

public class TextCardView extends BaseCardView {
    public static final int COLOR_TIMESLOT = 0xff265990;
    public static final int COLOR_CHANNEL = 0xff202a49;
    public static final int COLOR_PROGRAM = 0xff673300;
    public static final int COLOR_WILLRECORD = 0xff005500;
    public static final int COLOR_WONTRECORD = 0xff671313;

    public TextCardView(Context context) {
        super(context);
        LayoutInflater.from(getContext()).inflate(R.layout.text_card, this);
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
            if (card.cellType == card.CELL_TIMESLOT)
                bgColor = COLOR_TIMESLOT;
            else if (card.cellType == card.CELL_CHANNEL)
                bgColor = COLOR_CHANNEL;
            else if (card.cellType == card.CELL_PROGRAM && card.program != null) {
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
            if (status != null)
                statusText.setBackgroundColor(bgColor);
        }
    }

}
