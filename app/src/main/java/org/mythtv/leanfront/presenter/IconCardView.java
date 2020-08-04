package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;

import androidx.leanback.widget.BaseCardView;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.GuideSlot;

public class IconCardView extends BaseCardView {

    public IconCardView(Context context) {
        super(context);
        LayoutInflater.from(getContext()).inflate(R.layout.icon_card, this);
        setFocusable(true);
    }

    public void updateUi(GuideSlot card) {
        ImageView imageView = findViewById(R.id.card_image);
        if (card == null) {
            imageView.setImageDrawable(null);
        } else if (card.cellType == GuideSlot.CELL_LEFTARROW) {
            imageView.setImageResource(R.drawable.im_arrow_left);
        } else if (card.cellType == GuideSlot.CELL_RIGHTARROW) {
            imageView.setImageResource(R.drawable.im_arrow_right);
        }
    }

}
