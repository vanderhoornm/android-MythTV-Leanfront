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

package org.mythtv.leanfront.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.util.SparseIntArray;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

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
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.presenter.GuidePresenterSelector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class GuideFragment extends GridFragment implements AsyncBackendCall.OnBackendCallListener{

    public static final int TIMESLOTS = 8;
    // 1 cell per timeslot plus 1 for channel and two for arrows
    public static final int COLUMNS = TIMESLOTS+3;
    public static final int TIMESLOT_SIZE = 30; //minutes
    public static final int TIME_ROW_INTERVAL = 8;
    public static final int DATE_RANGE = 21;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_XSMALL;
    private ArrayObjectAdapter mGridAdapter;
    private Date mGridStartTime;
    private long mPriorGridStartTime;
    // map chanid to position in object adapter
    private SparseIntArray mChanArray = new SparseIntArray();
    private GuideSlot [] mTimeRow;
    private static DateFormat mTimeFormatter;
    private static DateFormat mDateFormatter;
    private static DateFormat mDayFormatter;
    private GregorianCalendar mTimeSelectCalendar;
    private AlertDialog mDialog;
    private boolean mLoadInProgress;
    private boolean mDoingUpdate;

    private static final int ACTION_EDIT_1 = 1;
    private static final int ACTION_EDIT_2 = 2;

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
    }

    private void setupAdapter() {
        VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
        // 1 cell per timeslot plus 1 for channel and two for arrows
        presenter.setNumberOfColumns(COLUMNS);
        setGridPresenter(presenter);

        mGridAdapter = new ArrayObjectAdapter(new GuidePresenterSelector(getContext()));
        setAdapter(mGridAdapter);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder,
                    Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (mLoadInProgress)
                    return;
                GuideSlot card = (GuideSlot)item;
                if (card == null)
                    return;
                switch (card.cellType) {
                    case GuideSlot.CELL_TIMESELECTOR:
                    case GuideSlot.CELL_TIMESLOT:
                        showTimeSelector();
                        break;
                    case GuideSlot.CELL_CHANNEL:
                        showChannelSelector();
                        break;
                    case GuideSlot.CELL_LEFTARROW:
                        mGridStartTime = new Date(mGridStartTime.getTime() - TIMESLOTS * TIMESLOT_SIZE * 60000);
                        setupGridData();
                        break;
                    case GuideSlot.CELL_RIGHTARROW:
                        mGridStartTime = new Date(mGridStartTime.getTime() + TIMESLOTS * TIMESLOT_SIZE * 60000);
                        setupGridData();
                        break;
                    case GuideSlot.CELL_PROGRAM:
                        programClicked(card);
                        break;
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setupGridData();
    }

    private void programClicked(GuideSlot card) {
        String[] prompts = new String[10];
        int[] actions = new int[10];
        int counter = 0;
        if (card.program != null) {
            prompts[counter] = getContext().getString(R.string.msg_edit_schedule, card.program.title);
            actions[counter] = ACTION_EDIT_1;
            ++counter;
        }
        if (card.program2 != null) {
            prompts[counter] = getContext().getString(R.string.msg_edit_schedule2, card.program2.title);
            actions[counter] = ACTION_EDIT_2;
            ++counter;
        }

        if (counter == 1) {
            actionRequest(card,actions[0]);
        } else if (counter > 1) {
            final String[] finalPrompts = new String[counter];
            final int[] finalActions = new int[counter];
            for (int i = 0; i < counter; i++) {
                finalPrompts[i] = prompts[i];
                finalActions[i] = actions[i];
            }
            String alertTitle = getContext().getString(R.string.title_program_guide);
            // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                    R.style.Theme_AppCompat_Dialog_Alert);
            builder .setTitle(alertTitle)
                    .setItems(finalPrompts,
                            (dialog, which) -> {
                                // The 'which' argument contains the index position
                                // of the selected item
                                if (which < finalActions.length) {
                                    actionRequest(card,finalActions[which]);
                                }
                            });
            builder.show();
        }
    }

    private void actionRequest(GuideSlot card, int action) {
        Intent intent;
        mDoingUpdate = true;
        switch (action) {
            case ACTION_EDIT_1:
                intent = new Intent(getContext(), EditScheduleActivity.class);
                intent.putExtra(EditScheduleActivity.CHANID, card.program.chanId);
                intent.putExtra(EditScheduleActivity.STARTTIME, card.program.startTime);
                startActivity(intent);
                break;
            case ACTION_EDIT_2:
                intent = new Intent(getContext(), EditScheduleActivity.class);
                intent.putExtra(EditScheduleActivity.CHANID, card.program2.chanId);
                intent.putExtra(EditScheduleActivity.STARTTIME, card.program2.startTime);
                startActivity(intent);
                break;
        }
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
        builder.setTitle(R.string.title_select_timeslot)
                .setView(R.layout.time_select_layout);
        builder.setPositiveButton(android.R.string.ok,
            (dialog, which) -> {
                Spinner dateSpin = mDialog.findViewById(R.id.date_select);
                Spinner timeSpin = mDialog.findViewById(R.id.time_select);
                long newStartTime = mTimeSelectCalendar.getTimeInMillis()
                        + dateSpin.getSelectedItemPosition() * 24*60*60*1000
                        + timeSpin.getSelectedItemPosition() * TIMESLOT_SIZE * 60 * 1000;
                mGridStartTime = new Date(newStartTime);
                setupGridData();
            } );
        builder.setNegativeButton(android.R.string.cancel, null);
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

    private void showChannelSelector() {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                R.style.Theme_AppCompat_Dialog_Alert);
        builder.setTitle(R.string.guide_jump_to_channel);

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            goToChannel(input);
        });
        builder.show();
    }

    private void goToChannel(EditText input) {
        String text = input.getText().toString();
        int chanNum = 0;
        try {
            chanNum = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return;
        }
        int size = mGridAdapter.size();
        boolean found = false;
        for (int ix = 0; ix < size; ix += COLUMNS) {
            GuideSlot slot = (GuideSlot) mGridAdapter.get(ix);
            if (slot.chanNum >= chanNum) {
                setSelectedPosition(ix, false);
                found = true;
                break;
            }
        }
        if (!found)
            setSelectedPosition(size - COLUMNS,false);
    }

    private void setupGridData() {
        if (mLoadInProgress)
            return;
        mLoadInProgress = true;
        Date gridEndTime = new Date(mGridStartTime.getTime() + TIMESLOT_SIZE * TIMESLOTS * 60000);
        loadCells();
        AsyncBackendCall call = new AsyncBackendCall(this);
        call.setStartTime(mGridStartTime);
        call.setEndTime(gridEndTime);
        if (mDoingUpdate)
            call.execute(Video.ACTION_PAUSE, Video.ACTION_GUIDE);
        else
            call.execute(Video.ACTION_GUIDE);
        mDoingUpdate = false;
    }

    /**
     * Preload the grid with timeslots for each channel.
     * TIMESLOT_SIZE minutes for each cell.
     * 1 cell per timeslot plus 1 for channel and two for arrows
     */
    private void loadCells() {
        if (mPriorGridStartTime > 0) {
            updateCells();
            return;
        }
        mPriorGridStartTime = mGridStartTime.getTime();
        VideoDbHelper dbh = VideoDbHelper.getInstance(getContext());
        SQLiteDatabase db = dbh.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                VideoContract.VideoEntry.COLUMN_SUBTITLE, // This is channel details
                VideoContract.VideoEntry.COLUMN_CHANID,
                VideoContract.VideoEntry.COLUMN_CHANNUM,
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
        int colChNum = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CHANNUM);
        while (cursor.moveToNext()) {
            if (tsRowCount == 0)
                addTimeRow();
            if (++tsRowCount >= TIME_ROW_INTERVAL)
                tsRowCount = 0;
            String chanDetails = cursor.getString(colSubt);
            int chanId = cursor.getInt(colChId);
            String chanNumStr = cursor.getString(colChNum);
            int chanNum = -1;
            if (chanNumStr != null) {
                String[] chanNumSplit = chanNumStr.split("[^0123456789]");
                if (chanNumSplit.length > 0 && chanNumSplit[0] != null)
                    chanNum = Integer.parseInt(chanNumSplit[0]);
            }
            // channel slot at front
            GuideSlot slot = new GuideSlot(chanId, chanNum, chanDetails);
            mGridAdapter.add(slot);
            mGridAdapter.add(leftArrowSlot);
            mChanArray.put(chanId,mGridAdapter.size());
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
                mGridAdapter.add(slot);
            }
            mGridAdapter.add(rightArrowSlot);
        }
        cursor.close();
        db.close();
    }

    private void updateCells() {
        // Update Time row
        mTimeRow[0].timeSlot = mGridStartTime;
        for (int ix = 0; ix< TIMESLOTS; ix++) {
            mTimeRow[ix+2].timeSlot =
                    new Date( mGridStartTime.getTime() + ix * TIMESLOT_SIZE * 60000);
        }
        // Clear out program cells
        for (int ix = 0 ; ix < mGridAdapter.size(); ix++) {
            GuideSlot slot = (GuideSlot)mGridAdapter.get(ix);
            int iPos = ix % (TIMESLOTS+3);
            if (slot.cellType == GuideSlot.CELL_PROGRAM) {
                slot.program = null;
                slot.program2 = null;
                slot.timeSlot = mTimeRow[iPos].timeSlot;
            }
        }
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
            mGridAdapter.add(mTimeRow[i]);
    }


    @Override
    public void onPostExecute(AsyncBackendCall taskRunner) {
        int [] tasks = taskRunner.getTasks();
        switch (tasks[0]) {
            case Video.ACTION_GUIDE:
                loadGuideData(taskRunner.getXmlResult());
                break;
            case Video.ACTION_PAUSE:
                loadGuideData(taskRunner.getXmlResults().get(1));
        }
    }

    void loadGuideData(XmlNode result) {
        mLoadInProgress = false;
        if (result == null)
            return;
        if (!isStarted)
            return;
        XmlNode programNode = null;
        for (; ; ) {
            if (programNode == null)
                programNode = result.getNode("Programs").getNode("Program");
            else
                programNode = programNode.getNextSibling();
            if (programNode == null)
                break;
            GuideSlot.Program program = new GuideSlot.Program(programNode);
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
                GuideSlot slot = (GuideSlot)mGridAdapter.get(ix);
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
        }
        mGridAdapter.notifyArrayItemRangeChanged(0, mGridAdapter.size()-1);
    }

}
