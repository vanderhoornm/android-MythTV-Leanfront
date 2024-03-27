/*
 * Copyright (c) 2020 Peter Bennett
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


package androidx.leanback.widget;

// This class is a kludge / bodge / workaround.
// Class ControlBarPresenter in the android libraries is declared with
// default permissions so the application cannot access it or its interfaces.
// Also class PlaybackTransportRowPresenter provides no way to access or
// update its OnControlSelectedListener.
// These problems mean that the app cannot access the OnControlSelectedListener for
// the purpose of displaying a help message when a control is selected.
// This kludge runs in the android.widget package and thus can access both
// the ControlBarPresenter class and the variables mPlaybackControlsPresenter
// and mSecondaryControlsPresenter in class PlaybackTransportRowPresenter.
// It facilitates the help message displayed when a control is selected.
// To do this without the kludge would require reimplementing several android
// classes in my code.

public class WidgetAccess {

    ControlBarPresenter.OnControlSelectedListener mParentListener;
    OverrideListener mOverrideListener = new OverrideListener();
    MySelectedListener mPlayerListener;

    public interface MySelectedListener {
        void onControlSelected(Presenter.ViewHolder controlViewHolder, Object item);
    }

    public void setMySelectedListener(
            PlaybackTransportRowPresenter presenter,
            MySelectedListener listener) {
        mPlayerListener = listener;
        mParentListener = presenter.mPlaybackControlsPresenter.getOnItemControlListener();
        presenter.mPlaybackControlsPresenter.setOnControlSelectedListener(mOverrideListener);
        presenter.mSecondaryControlsPresenter.setOnControlSelectedListener(mOverrideListener);
    }

    public class  OverrideListener implements ControlBarPresenter.OnControlSelectedListener {
        @Override
        public void onControlSelected(Presenter.ViewHolder controlViewHolder, Object item, ControlBarPresenter.BoundData data) {
            if (mParentListener != null)
                mParentListener.onControlSelected(controlViewHolder, item, data);
            if (mPlayerListener != null)
                mPlayerListener.onControlSelected(controlViewHolder, item);
        }
    }
}
