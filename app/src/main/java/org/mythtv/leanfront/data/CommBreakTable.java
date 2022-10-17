package org.mythtv.leanfront.data;

import android.util.Log;

import androidx.annotation.Nullable;

public class CommBreakTable {
    public Entry[] entries = new Entry[0];

    private static final String TAG = "lfe";
    private static final String CLASS = "CommBreakTable";
    public static final int MARK_COMM_START = 4;
    public static final int MARK_COMM_END   = 5;
    public static final int MARK_CUT_START   = 1;
    public static final int MARK_CUT_END   = 0;

    public synchronized void clear(int count) {
        entries = new Entry[count];
    }

    public synchronized void load(XmlNode data) {
        XmlNode node = data;
        int firstmark = -1;
        int lastmark = -1;
        if (node != null)
            node = node.getNode("Cuttings");
        if (node != null)
            node = node.getNode("Cutting");
        if (node != null)
            firstmark = node.getInt("Mark", -99);
        int nodeCount = 0;
        while (node != null) {
            nodeCount++;
            XmlNode nextNode = node.getNextSibling();
            if (nextNode == null)
                lastmark = node.getInt("Mark", -99);
            node = nextNode;
        }
        // Cater for "Cut to start" where there is no start entry
        if (firstmark == MARK_COMM_END || firstmark == MARK_CUT_END)
            nodeCount++;
        // Cater for "Cut to end" where there is no end entry
        if (lastmark == MARK_COMM_START || lastmark == MARK_CUT_START)
            nodeCount++;
        Log.i(TAG, CLASS + " CommBreakTable size:" + nodeCount );
        clear(nodeCount);
        node = data;
        if (node != null)
            node = node.getNode("Cuttings");
        if (node != null)
            node = node.getNode("Cutting");
        int ix = 0;
        int prior = 0;
        // Cater for "Cut to start" where there is no start entry
        // by creating a first entry
        if (firstmark == MARK_COMM_END || firstmark == MARK_CUT_END)
            entries[ix++] = new Entry(0,MARK_CUT_START);
        while (node != null) {
            int mark = node.getInt("Mark", 0);
            int duration = node.getInt("Offset", 0);
            if (duration < prior) {
                Log.e(TAG, CLASS + " CommBreakTable out of sequence:" + prior + "," + duration);
                clear(0);
                return;
            }
            prior = duration;
            switch (mark) {
                case MARK_COMM_START:
                    mark = MARK_CUT_START;
                    break;
                case MARK_COMM_END:
                    mark = MARK_CUT_END;
                    break;
            }
            entries[ix++] = new Entry (duration, mark);
            node = node.getNextSibling();
        }
        if (ix == 0) {
            clear(0);
            return;
        }
        // Cater for "Cut to end" where there is no end entry
        // by creating a last entry
        if (lastmark == MARK_COMM_START || lastmark == MARK_CUT_START)
            entries[ix++] = new Entry(Integer.MAX_VALUE - 10000000, MARK_CUT_END);
        // Fill in any unused entries with the last entry
        while (ix < nodeCount)
            entries[ix++] = new Entry(Integer.MAX_VALUE - 10000000, MARK_CUT_START);;
    }

    public static class Entry implements Comparable<Entry> {

        public final int durationMs;
        public final int mark;

        public Entry(int durationMs, int mark) {
            this.durationMs = durationMs;
            this.mark = mark;
        }

        @Override
        public int compareTo(Entry o) {
            return durationMs - o.durationMs;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof Entry)
                return (durationMs == ((Entry) o).durationMs);
            return super.equals(o);
        }
    }
}