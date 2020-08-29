package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.mythtv.leanfront.model.GuideSlot;

public class TextCardPresenter extends Presenter {
    private int mType;

    public TextCardPresenter(int type) {
        super();
        mType = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new TextCardView(parent.getContext(), mType));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextCardView)viewHolder.view).updateUi((GuideSlot) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
//        ((TextCardView)viewHolder.view).updateUi(null);
    }
}
