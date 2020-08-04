package org.mythtv.leanfront.presenter;

import android.content.Context;

import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import org.mythtv.leanfront.model.GuideSlot;

import java.util.HashMap;

public class CardPresenterSelector extends PresenterSelector {

    private final Context mContext;
    public CardPresenterSelector(Context context) {
        mContext = context;
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (item instanceof GuideSlot) {
            GuideSlot slot = (GuideSlot) item;
            switch (slot.cellType) {
                case GuideSlot.CELL_TIMESELECTOR:
                    return new TextCardPresenter(mContext);
                default:
                    return new TextCardPresenter(mContext);
            }
        }

        return null;
    }
}
