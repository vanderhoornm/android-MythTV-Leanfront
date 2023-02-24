package org.mythtv.leanfront.ui;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Row;

import org.mythtv.leanfront.R;

public class ManageRecordingsFragment extends BrowseSupportFragment {

    private static int HEADER_ID_GUIDE = 1;
    private static int HEADER_ID_RECRULES = 2;
    private static int HEADER_ID_UPCOMING = 3;

    private ArrayObjectAdapter mRowsAdapter;
    private BackgroundManager mBackgroundManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUi();
        loadData();
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        getMainFragmentRegistry().registerFragment(PageRow.class,
                new PageRowFragmentFactory(mBackgroundManager));
    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));
        setTitle(getString(R.string.title_manage_recordings));
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        prepareEntranceTransition();
    }

    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
        createRows();
        startEntranceTransition();
    }

    private void createRows() {
        HeaderItem headerItem3 = new HeaderItem(HEADER_ID_UPCOMING, getString(R.string.title_upcoming));
        PageRow pageRow3 = new PageRow(headerItem3);
        mRowsAdapter.add(pageRow3);
        HeaderItem headerItem2 = new HeaderItem(HEADER_ID_RECRULES, getString(R.string.title_rec_rules));
        PageRow pageRow2 = new PageRow(headerItem2);
        mRowsAdapter.add(pageRow2);
        HeaderItem headerItem1 = new HeaderItem(HEADER_ID_GUIDE, getString(R.string.title_program_guide));
        PageRow pageRow1 = new PageRow(headerItem1);
        mRowsAdapter.add(pageRow1);
    }

    private static class PageRowFragmentFactory extends BrowseSupportFragment.FragmentFactory {
        private final BackgroundManager mBackgroundManager;

        PageRowFragmentFactory(BackgroundManager backgroundManager) {
            this.mBackgroundManager = backgroundManager;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            mBackgroundManager.setDrawable(null);
            if (row.getHeaderItem().getId() == HEADER_ID_GUIDE) {
                return new GuideFragment();
            }
            if (row.getHeaderItem().getId() == HEADER_ID_RECRULES) {
                return new RecRulesFragment();
            }
            if (row.getHeaderItem().getId() == HEADER_ID_UPCOMING) {
                return new UpcomingFragment();
            }
            return null;
        }
    }

}
