package org.mythtv.leanfront.presenter;

import android.content.Context;

import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import org.mythtv.leanfront.model.GuideSlot;

public class GuidePresenterSelector extends PresenterSelector {

    private final Context mContext;
    private IconCardPresenter mIconCardPresenter;
    private GuideCardPresenter mGuideCardPresenter;

    public GuidePresenterSelector(Context context)
    {
        mContext = context;
        mIconCardPresenter = new IconCardPresenter(context);
        mGuideCardPresenter = new GuideCardPresenter(GuideCardView.TYPE_SMALL);
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (item instanceof GuideSlot) {
            GuideSlot slot = (GuideSlot) item;
            switch (slot.cellType) {
                case GuideSlot.CELL_LEFTARROW:
                case GuideSlot.CELL_RIGHTARROW:
                    return mIconCardPresenter;
                default:
                    return mGuideCardPresenter;
            }
        }

        return null;
    }
}
