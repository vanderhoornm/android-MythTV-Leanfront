package org.mythtv.leanfront.presenter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.mythtv.leanfront.model.GuideSlot;

public class IconCardPresenter extends Presenter {
    private Context mContext;

    public IconCardPresenter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new IconCardView(mContext));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((IconCardView)viewHolder.view).updateUi((GuideSlot) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ((IconCardView)viewHolder.view).updateUi(null);
    }
}
