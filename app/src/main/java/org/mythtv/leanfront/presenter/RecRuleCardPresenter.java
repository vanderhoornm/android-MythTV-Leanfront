package org.mythtv.leanfront.presenter;

import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.mythtv.leanfront.model.RecordRule;

public class RecRuleCardPresenter extends Presenter {
    private int mType;

    public RecRuleCardPresenter(int type) {
        super();
        mType = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new RecRuleCardView(parent.getContext(), mType));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((RecRuleCardView)viewHolder.view).updateUi((RecordRule) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
