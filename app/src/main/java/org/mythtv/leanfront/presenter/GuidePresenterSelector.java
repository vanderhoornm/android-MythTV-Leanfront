/*
 * Copyright (c) 2019-2020 Peter Bennett
 *
 * This file is part of MythTV-leanfront.
 *
 * MythTV-leanfront is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * MythTV-leanfront is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with MythTV-leanfront.  If not, see <https://www.gnu.org/licenses/>.
 */


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
