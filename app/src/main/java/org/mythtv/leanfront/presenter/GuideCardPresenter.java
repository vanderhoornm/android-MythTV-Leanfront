package org.mythtv.leanfront.presenter;

import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.mythtv.leanfront.model.GuideSlot;

public class GuideCardPresenter extends Presenter {
    private int mType;

    public GuideCardPresenter(int type) {
        super();
        mType = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new GuideCardView(parent.getContext(), mType));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((GuideCardView)viewHolder.view).updateUi((GuideSlot) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
