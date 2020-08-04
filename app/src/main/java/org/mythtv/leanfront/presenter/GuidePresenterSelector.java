package org.mythtv.leanfront.presenter;

import android.content.Context;

import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import org.mythtv.leanfront.model.GuideSlot;

public class GuidePresenterSelector extends PresenterSelector {

    private final Context mContext;
    public GuidePresenterSelector(Context context) {
        mContext = context;
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (item instanceof GuideSlot) {
            GuideSlot slot = (GuideSlot) item;
            switch (slot.cellType) {
                case GuideSlot.CELL_LEFTARROW:
                case GuideSlot.CELL_RIGHTARROW:
                    return new IconCardPresenter(mContext);
                default:
                    return new TextCardPresenter(mContext);
            }
        }

        return null;
    }
}
