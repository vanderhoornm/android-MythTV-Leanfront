package org.mythtv.leanfront.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import org.mythtv.leanfront.data.AsyncBackendCall;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.GuideSlot;
import org.mythtv.leanfront.model.RecordRule;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.presenter.RecRuleCardPresenter;
import org.mythtv.leanfront.presenter.RecRuleCardView;

import java.util.Date;

public class RecRulesFragment  extends GridFragment implements AsyncBackendCall.OnBackendCallListener {

    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_XSMALL;
    private final int NUMBER_COLUMNS = 3;

    private ArrayObjectAdapter mGridAdapter;
    private boolean mLoadInProgress;
    private boolean isResumed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        setupGridData();
    }

    private void setupAdapter() {
        VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
        presenter.setNumberOfColumns(NUMBER_COLUMNS);
        setGridPresenter(presenter);

        mGridAdapter = new ArrayObjectAdapter(new RecRuleCardPresenter(RecRuleCardView.TYPE_SMALL));
        setAdapter(mGridAdapter);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder,
                                      Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (mLoadInProgress)
                    return;
                RecordRule card = (RecordRule)item;
                        recRuleClicked(card);
            }
        });
    }

    private void recRuleClicked(RecordRule card) {
        Intent intent = new Intent(getContext(), EditScheduleActivity.class);
        intent.putExtra(EditScheduleActivity.RECORDID, card.recordId);
        startActivity(intent);
    }

    private void setupGridData() {
        if (mLoadInProgress)
            return;
        mLoadInProgress = true;
        AsyncBackendCall call = new AsyncBackendCall(this);
        call.execute(Video.ACTION_GETRECORDSCHEDULELIST);
    }

    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        int [] tasks = taskRunner.getTasks();
        switch (tasks[0]) {
            case Video.ACTION_GETRECORDSCHEDULELIST:
                loadData(taskRunner.getXmlResult());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;
    }

    void loadData(XmlNode result) {
        mLoadInProgress = false;
        if (result == null)
            return;
        if (!isResumed)
            return;
        XmlNode recRuleNode = null;
        for (; ; ) {
            if (recRuleNode == null)
                recRuleNode = result.getNode("RecRules").getNode("RecRule");
            else
                recRuleNode = recRuleNode.getNextSibling();
            if (recRuleNode == null)
                break;
            RecordRule rule = new RecordRule().fromSchedule(recRuleNode);
            mGridAdapter.add(rule);
        }
    }

}
