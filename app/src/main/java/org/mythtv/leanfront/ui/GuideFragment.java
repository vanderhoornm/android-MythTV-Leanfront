package org.mythtv.leanfront.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.AsyncBackendCall;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.data.VideoDbHelper;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.GuideSlot;
import org.mythtv.leanfront.model.Program;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.presenter.GuidePresenterSelector;
import org.mythtv.leanfront.presenter.TextCardPresenter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class GuideFragment extends GridFragment implements AsyncBackendCall.OnBackendCallListener {

    public static final int TIMESLOTS = 8;
    public static final int TIMESLOT_SIZE = 30; //minutes
    public static final int TIME_ROW_INTERVAL = 8;
    public static final int DATE_RANGE = 21;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    private ArrayObjectAdapter mGuideAdapter;
    private Date mGridStartTime;
    // map chanid to position in object adapter
    private SparseIntArray mChanArray = new SparseIntArray();
    private GuideSlot [] mTimeRow;
    private static DateFormat mTimeFormatter;
    private static DateFormat mDateFormatter;
    private static DateFormat mDayFormatter;
    private GregorianCalendar mTimeSelectCalendar;
    private AlertDialog mDialog;
    private boolean mLoadInProgress;
    private int mSelectedPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long now = System.currentTimeMillis();
        // round down to 30 minute interval
        long startTime = now / (TIMESLOT_SIZE * 60000);
        startTime = startTime * TIMESLOT_SIZE * 60000;
        mGridStartTime = new Date(startTime);
        setupAdapter();
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        setupGridData();
    }

    private void setupAdapter() {
        VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
        // 1 cell per timeslot plus 1 for channel and two for arrows
        presenter.setNumberOfColumns(TIMESLOTS+3);
        setGridPresenter(presenter);

        mGuideAdapter = new ArrayObjectAdapter(new GuidePresenterSelector(getContext()));
        setAdapter(mGuideAdapter);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(
                    Presenter.ViewHolder itemViewHolder,
                    Object item,
                    RowPresenter.ViewHolder rowViewHolder,
                    Row row) {
                if (mLoadInProgress)
                    return;
                GuideSlot card = (GuideSlot)item;
                if (card.cellType == card.CELL_TIMESELECTOR) {
                    mSelectedPosition = mGridViewHolder.getGridView().getSelectedPosition();
                    showTimeSelector();
                }
                else if (card.cellType == card.CELL_LEFTARROW) {
                    mGridStartTime.setTime(mGridStartTime.getTime() - TIMESLOTS * TIMESLOT_SIZE * 60000);
                    mSelectedPosition = mGridViewHolder.getGridView().getSelectedPosition();
                    setupGridData();
                } else if (card.cellType == card.CELL_RIGHTARROW) {
                        mGridStartTime.setTime(mGridStartTime.getTime() + TIMESLOTS * TIMESLOT_SIZE * 60000);
                        mSelectedPosition = mGridViewHolder.getGridView().getSelectedPosition();
                        setupGridData();
                } else
                    Toast.makeText(getActivity(),
                        "Clicked on something",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTimeSelector() {
        Context context = getContext();
        if (mTimeFormatter == null) {
            mTimeFormatter = android.text.format.DateFormat.getTimeFormat(context);
            mDateFormatter = android.text.format.DateFormat.getLongDateFormat(context);
            mDayFormatter = new SimpleDateFormat("EEE ");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                R.style.Theme_AppCompat_Dialog_Alert);
        AlertDialogListener listener = new AlertDialogListener();
        builder.setTitle(R.string.title_select_timeslot)
                .setView(R.layout.time_select_layout);
        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);
        mDialog = builder.create();
        mDialog.show();
        Spinner dateSpin=mDialog.findViewById(R.id.date_select);
        ArrayAdapter<String> adapter = new ArrayAdapter(context,android.R.layout.simple_spinner_item );
        mTimeSelectCalendar = new GregorianCalendar();
        mTimeSelectCalendar.set(GregorianCalendar.HOUR_OF_DAY,0);
        mTimeSelectCalendar.set(GregorianCalendar.MINUTE,0);
        mTimeSelectCalendar.set(GregorianCalendar.SECOND,0);
        mTimeSelectCalendar.set(GregorianCalendar.MILLISECOND,0);
        // 1 second after midnight this morning real time.
        long millis = mTimeSelectCalendar.getTimeInMillis() + 1000;
        long startTimeMillis = mGridStartTime.getTime();
        int dateSelection = -1;
        for (int i = 0; i < DATE_RANGE; i++) {
            Date date = new Date(millis);
            adapter.add(mDayFormatter.format(date) + mDateFormatter.format(date));
            long timediff = startTimeMillis - millis;
            if (dateSelection == -1 && timediff < 24*60*60000)
                dateSelection = i;
            millis += (long)(24*60*60*1000);
        }
        dateSpin.setAdapter(adapter);
        Spinner timeSpin=mDialog.findViewById(R.id.time_select);
        adapter = new ArrayAdapter(context,android.R.layout.simple_spinner_item );
        millis = mTimeSelectCalendar.getTimeInMillis(); // midnight last night real time
        GregorianCalendar cal2 = new GregorianCalendar();
        cal2.setTime(mGridStartTime);
        int hour = cal2.get(Calendar.HOUR_OF_DAY);
        int min = cal2.get(Calendar.MINUTE);
        cal2.setTimeInMillis(mTimeSelectCalendar.getTimeInMillis());
        cal2.set(Calendar.HOUR_OF_DAY,hour);
        cal2.set(Calendar.MINUTE,min);
        startTimeMillis = cal2.getTimeInMillis();
        int timeSelection = -1;
        for (int i = 0; i < 24*60/TIMESLOT_SIZE; i++) {
            adapter.add(mTimeFormatter.format(new Date(millis)));
            long timediff = startTimeMillis - millis;
            if (timeSelection == -1 && timediff < TIMESLOT_SIZE*60000)
                timeSelection = i;
            millis += (long)(TIMESLOT_SIZE*60*1000);
        }
        timeSpin.setAdapter(adapter);
        dateSpin.setSelection(dateSelection);
        timeSpin.setSelection(timeSelection);
    }


    class AlertDialogListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Spinner dateSpin = mDialog.findViewById(R.id.date_select);
                    Spinner timeSpin = mDialog.findViewById(R.id.time_select);
                    long newStartTime = mTimeSelectCalendar.getTimeInMillis()
                        + dateSpin.getSelectedItemPosition() * 24*60*60*1000
                        + timeSpin.getSelectedItemPosition() * TIMESLOT_SIZE * 60 * 1000;
                    mGridStartTime = new Date(newStartTime);
                    setupGridData();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    }

    private void setupGridData() {
        if (mLoadInProgress)
            return;
        mLoadInProgress = true;
        Date gridEndTime = new Date(mGridStartTime.getTime() + TIMESLOT_SIZE * TIMESLOTS * 60000);
        loadCells();
        AsyncBackendCall call = new AsyncBackendCall(null, 0L, false,
                this);
        call.setStartTime(mGridStartTime);
        call.setEndTime(gridEndTime);
        call.execute(Video.ACTION_GUIDE);
    }


    /**
     * Preload the grid with timeslots for each channel.
     * TIMESLOT_SIZE minutes for each cell.
     * 1 cell per timeslot plus 1 for channel and two for arrows
     */
    private void loadCells() {
        mGuideAdapter.clear();
        VideoDbHelper dbh = new VideoDbHelper(getContext());
        SQLiteDatabase db = dbh.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                VideoContract.VideoEntry.COLUMN_SUBTITLE, // This is channel details
                VideoContract.VideoEntry.COLUMN_CHANID,
        };

        // Filter results
        String selection = VideoContract.VideoEntry.COLUMN_RECTYPE + " = "
                 + VideoContract.VideoEntry.RECTYPE_CHANNEL;

        StringBuilder orderby = new StringBuilder();
        orderby.append("CAST (").append(VideoContract.VideoEntry.COLUMN_CHANNUM).append(" as real), ");
        orderby.append(VideoContract.VideoEntry.COLUMN_CHANNUM).append(", ");
        orderby.append(VideoContract.VideoEntry.COLUMN_SUBTITLE).append(", ");
        orderby.append(VideoContract.VideoEntry.COLUMN_CHANID);

        Cursor cursor = db.query(
                VideoContract.VideoEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                orderby.toString()               // The sort order
        );

        // arrow slots
        GuideSlot leftArrowSlot = new GuideSlot(GuideSlot.CELL_LEFTARROW);
        GuideSlot rightArrowSlot = new GuideSlot(GuideSlot.CELL_RIGHTARROW);

        // Counter to ensure new time row every few rows.
        int tsRowCount = 0;
        setupTimeRow(leftArrowSlot, rightArrowSlot);


        int colSubt = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_SUBTITLE);
        int colChId = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CHANID);
        while (cursor.moveToNext()) {
            if (tsRowCount == 0)
                addTimeRow();
            if (++tsRowCount >= TIME_ROW_INTERVAL)
                tsRowCount = 0;
            String chanDetails = cursor.getString(colSubt);
            int chanId = cursor.getInt(colChId);
            // channel slot at front
            GuideSlot slot = new GuideSlot(chanId, chanDetails);
            mGuideAdapter.add(slot);
            mGuideAdapter.add(leftArrowSlot);
            mChanArray.put(chanId,mGuideAdapter.size());
            for (int i = 0; i< TIMESLOTS; i++) {
                int position;
                switch (i) {
                    case 0:
                        position = GuideSlot.POS_LEFT;
                        break;
                    case TIMESLOTS-1:
                        position = GuideSlot.POS_RIGHT;
                        break;
                    default:
                        position = GuideSlot.POS_MIDDLE;
                }
                slot = new GuideSlot(GuideSlot.CELL_PROGRAM, position, mTimeRow[i+2].timeSlot);
                mGuideAdapter.add(slot);
            }
            mGuideAdapter.add(rightArrowSlot);
        }
        cursor.close();
        db.close();
    }

    private void setupTimeRow(GuideSlot leftArrowSlot, GuideSlot rightArrowSlot) {
        mTimeRow = new GuideSlot[TIMESLOTS + 3];
        // time selector slot at front
        mTimeRow[0] = new GuideSlot(GuideSlot.CELL_TIMESELECTOR);
        mTimeRow[0].timeSlot = mGridStartTime;
        mTimeRow[1] = leftArrowSlot;
        for (int ix = 0; ix< TIMESLOTS; ix++) {
            mTimeRow[ix+2] = new GuideSlot(GuideSlot.CELL_TIMESLOT,0,
                    new Date( mGridStartTime.getTime() + ix * TIMESLOT_SIZE * 60000));
        }
        mTimeRow[TIMESLOTS+2] = rightArrowSlot;
    }

    private void addTimeRow() {
        for (int i = 0; i < mTimeRow.length; i++)
            mGuideAdapter.add(mTimeRow[i]);
    }


    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        int [] tasks = taskRunner.getTasks();
        switch (tasks[0]) {
            case Video.ACTION_GUIDE:
                loadGuideData(taskRunner.getXmlResult());
        }
    }

    void loadGuideData(XmlNode result) {
        mLoadInProgress = false;
        if (result == null)
            return;
        XmlNode programNode = null;
        for (; ; ) {
            if (programNode == null)
                programNode = result.getNode("Programs").getNode("Program");
            else
                programNode = programNode.getNextSibling();
            if (programNode == null)
                break;
            Program program = new Program(programNode);
            int adapterPos = mChanArray.get(program.chanId,-1);
            if (adapterPos == -1)
                continue;
            if (program.startTime == null || program.endTime == null)
                continue;

            long lPos = (program.startTime.getTime() - mGridStartTime.getTime())
                    / (TIMESLOT_SIZE*60);
            float fPos = (float)lPos / 1000.0f;
            // Start position is the slot wherein the show starts.
            int startPos = (int)(fPos);
            if (startPos >= TIMESLOTS)
                continue;
            if (startPos < 0)
                startPos = 0;

            lPos = (program.endTime.getTime() - mGridStartTime.getTime())
                    / (TIMESLOT_SIZE*60);
            fPos = (float)lPos / 1000.0f;
            // End position is the slot before the one where the show ends
            // unless it ends in the same slot as it starts.
            int endPos = (int)(fPos);
            if (endPos <= 0)
                continue;
            if (endPos >= TIMESLOTS)
                endPos = TIMESLOTS;
            if (endPos == startPos)
                ++endPos;

            for (int ix = adapterPos+startPos; ix < adapterPos+endPos; ix++) {
                GuideSlot slot = (GuideSlot)mGuideAdapter.get(ix);
                if (slot.program == null)
                    slot.program = program;
                else if (slot.program2 == null) {
                    if (program.startTime.after(slot.program.startTime))
                        slot.program2 = program;
                    else {
                        slot.program2 = slot.program;
                        slot.program = program;
                    }
                }
            }
            mGuideAdapter.notifyArrayItemRangeChanged(adapterPos+startPos, endPos-startPos);
        }
        mGridViewHolder.getGridView().setSelectedPosition(mSelectedPosition);
        mSelectedPosition = 0;
    }


}
