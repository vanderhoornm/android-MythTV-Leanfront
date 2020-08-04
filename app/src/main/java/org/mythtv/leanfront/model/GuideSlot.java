package org.mythtv.leanfront.model;

import android.content.Context;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.ui.GuideFragment;

import java.text.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GuideSlot {
    public int chanId = -1;
    public String chanDetails;
    public Date timeSlot;       // Time of this grid position
    public Program program;
    public Program program2;    // In case of 15 minute programs
    // position in grid.
    public int position = 0;
    public static final int POS_LEFT = 1;
    public static final int POS_MIDDLE = 2;
    public static final int POS_RIGHT = 3;
    public int cellType = 0;
    public static final int CELL_TIMESLOT = 1;
    public static final int CELL_CHANNEL = 2;
    public static final int CELL_PROGRAM = 3;
    public static final int CELL_TIMESELECTOR = 4;
    public static final int CELL_LEFTARROW = 5;
    public static final int CELL_RIGHTARROW = 6;
    private static DateFormat timeFormatter;
    private static DateFormat dateFormatter;
    private static DateFormat dayFormatter;

    public GuideSlot(int cellType) {
        this.cellType = cellType;
    }

    public GuideSlot(int cellType, int position, Date timeSlot)
    {
        this.position = position;
        this.timeSlot = timeSlot;
        this.cellType = cellType;
    }

    public GuideSlot(int chanId, String chanDetails) {
        this.chanId = chanId;
        this.chanDetails = chanDetails;
        cellType = CELL_CHANNEL;
    }

    public String getGuideText(Context context) {
        if (timeFormatter == null) {
            timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
            dateFormatter = android.text.format.DateFormat.getLongDateFormat(context);
            dayFormatter = new SimpleDateFormat("EEE ");
        }
        StringBuilder build = new StringBuilder();
        try {
            boolean titleDone = false;
            if (cellType == CELL_TIMESLOT && timeSlot != null) {
                Date endTime = new Date(timeSlot.getTime() + GuideFragment.TIMESLOT_SIZE *60000);
                build.append(timeFormatter.format(timeSlot)).append(" - ").append(timeFormatter.format(endTime));
            }
            else if (cellType == CELL_TIMESELECTOR && timeSlot != null) {
                build.append(context.getString(R.string.title_grid_time)).append('\n')
                  .append(dayFormatter.format(timeSlot))
                  .append(dateFormatter.format(timeSlot)).append(' ')
                  .append(timeFormatter.format(timeSlot)).append('\n');
            }
            else if (cellType == CELL_CHANNEL && chanDetails != null)
                build.append(chanDetails);
            else if (program != null){
                if (program2 != null)
                    build.append("1. ");
                titleDone = getTitle(build, program);
                if (titleDone && program2 == null) {
                    build.append('\n');
                    if (program.season > 0 && program.episode > 0)
                        build.append('S').append(program.season).append('E').append(program.episode).append(' ');
                    if (program.subTitle != null)
                        build.append(program.subTitle);
                }
                if (!titleDone) {
                    build.append(program.title).append(' ').append(context.getString(R.string.note_program_continuation));
                }
                if (program2 != null) {
                    build.append('\n').append("2. ");
                    getTitle(build,program2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return build.toString();
    }

    private boolean getTitle(StringBuilder build, Program program) {
        boolean titleDone = false;
        if (program != null) {
            long timeSetStart = (program.startTime.getTime() - timeSlot.getTime());
            if (timeSetStart < 0 && (position == POS_LEFT)
                    || timeSetStart > 0) {
                build.append("(").append(timeFormatter.format(program.startTime)).append(") ");
                build.append(program.title);
                titleDone = true;
            }
            if (!titleDone && timeSetStart == 0) {
                build.append(program.title);
                titleDone = true;
            }
        }
        return titleDone;
    }
}
