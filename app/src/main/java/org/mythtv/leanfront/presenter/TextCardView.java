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
    public static final int COLOR_PROGRAM = 0xff906020;

    public TextCardView(Context context) {
        super(context);
        LayoutInflater.from(getContext()).inflate(R.layout.text_card, this);
        setFocusable(true);
    }

    public void updateUi(GuideSlot card) {
        TextView cardText = findViewById(R.id.card_text);
        if (card == null) {
            cardText.setText(null);
            setBackgroundColor(Color.DKGRAY);
        }
        else {
            cardText.setText(card.getGuideText(getContext()));
            if (card.cellType == card.CELL_TIMESLOT)
                setBackgroundColor(COLOR_TIMESLOT);
            else if (card.cellType == card.CELL_CHANNEL)
                setBackgroundColor(COLOR_CHANNEL);
            else if (card.cellType == card.CELL_PROGRAM && card.program != null)
                setBackgroundColor(COLOR_PROGRAM);
            else
                setBackgroundColor(Color.DKGRAY);
        }
    }

}
