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
            setBackgroundColor(Color.DKGRAY);
        }
        else {
            cardText.setText(card.getCardText(getContext()));
            if (card.inactive)
                bgColor = COLOR_WONTRECORD;
            else
                bgColor = COLOR_WILLRECORD;
            setBackgroundColor(bgColor);
            statusText.setBackgroundColor(bgColor);
        }
    }
}
