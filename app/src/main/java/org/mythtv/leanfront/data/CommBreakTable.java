package org.mythtv.leanfront.data;

import android.util.Log;

import androidx.annotation.Nullable;

public class CommBreakTable {
    public Entry[] entries = new Entry[0];

    private static final String TAG = "lfe";
    private static final String CLASS = "CommBreakTable";
    public static final int MARK_COMM_START = 4;
    public static final int MARK_COMM_END   = 5;

    public synchronized void clear(int count) {
        entries = new Entry[count];
    }

    public synchronized void load(XmlNode data) {
        XmlNode node = data;
        if (node != null)
            node = node.getNode("Cuttings");
        if (node != null)
            node = node.getNode("Cutting");
        int nodeCount = 0;
        while (node != null) {
            nodeCount++;
            node = node.getNextSibling();
        }
        Log.i(TAG, CLASS + " CommBreakTable size:" + nodeCount );
        clear(nodeCount);
        node = data;
        if (node != null)
            node = node.getNode("Cuttings");
        if (node != null)
            node = node.getNode("Cutting");
        int ix = 0;
        int prior = 0;
        while (node != null) {
            int mark = node.getInt("Mark", 0);
            int duration = node.getInt("Offset", 0);
            if (duration < prior) {
                Log.e(TAG, CLASS + " CommBreakTable out of sequence:" + prior + "," + duration);
                clear(0);
                return;
            }
            prior = duration;
            Entry entry = new Entry
                    (duration, mark);
            entries[ix++] = entry;
            node = node.getNextSibling();
        }
        if (ix == 0) {
            clear(0);
            return;
        }
        // Fill in any unused entries with the last entry
        while (ix < nodeCount)
            entries[ix++] = entries[ix-1];
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