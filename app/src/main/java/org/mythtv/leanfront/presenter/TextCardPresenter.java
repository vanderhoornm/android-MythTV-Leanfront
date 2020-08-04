package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.mythtv.leanfront.model.GuideSlot;


/**
 * The Presenter displays a card consisting of text as a replacement for a big image. The footer is
 * also quite unique since it does contain two images rather than one or non.
 */
public class TextCardPresenter extends Presenter {
    private Context mContext;

    public TextCardPresenter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new TextCardView(mContext));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextCardView)viewHolder.view).updateUi((GuideSlot) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ((TextCardView)viewHolder.view).updateUi(null);
    }
}
