package org.mythtv.leanfront.data;

import android.util.Log;

import androidx.annotation.Nullable;

public class SeekTable {
    public SeekEntry[] entries = new SeekEntry[0];

    private static final String TAG = "lfe";
    private static final String CLASS = "SeekTable";

    public synchronized void clear(int count) {
        entries = new SeekEntry[count];
    }

    public synchronized void load(AsyncBackendCall taskRunner, int index) {
        XmlNode node = taskRunner.getXmlResults().get(index);
        if (node != null)
            node = node.getNode("Cuttings");
        if (node != null)
            node = node.getNode("Cutting");
        int durCount = 0;
        while (node != null) {
            durCount++;
            node = node.getNextSibling();
        }
        node = taskRunner.getXmlResults().get(index+1);
        if (node != null)
            node = node.getNode("Cuttings");
        if (node != null)
            node = node.getNode("Cutting");
        int byteCount = 0;
        while (node != null) {
            byteCount++;
            node = node.getNextSibling();
        }
        Log.i(TAG, CLASS + " Seektable size dur:" + durCount + " byte:" + byteCount);
        int tabSize = Math.max(durCount, byteCount);
        clear(tabSize);
        XmlNode dNode = taskRunner.getXmlResults().get(index);
        if (dNode != null)
            dNode = dNode.getNode("Cuttings");
        if (dNode != null)
            dNode = dNode.getNode("Cutting");
        XmlNode bNode = taskRunner.getXmlResults().get(index+1);
        if (bNode != null)
            bNode = bNode.getNode("Cuttings");
        if (bNode != null)
            bNode = bNode.getNode("Cutting");
        int ix = 0;
        int prior = 0;
        while (dNode != null && bNode != null) {
            int dMark = dNode.getInt("Mark", 0);
            int bMark = bNode.getInt("Mark", 0);
            if (dMark < bMark) {
                dNode = dNode.getNextSibling();
                continue;
            }
            if (bMark < dMark) {
                bNode = bNode.getNextSibling();
                continue;
            }
            // if you get here dMark == bMark
            int duration = dNode.getInt("Offset", 0);
            if (duration < prior) {
                Log.e(TAG, CLASS + " Seektable out of sequence:" + prior + "," + duration);
                clear(0);
                return;
            }
            prior = duration;
            SeekTable.SeekEntry entry = new SeekTable.SeekEntry
                    (duration, bNode.getInt("Offset", 0));
            entries[ix++] = entry;
            dNode = dNode.getNextSibling();
            bNode = bNode.getNextSibling();
        }
        if (ix == 0) {
            clear(0);
            return;
        }
        // Fill in any unused entries with the last entry
        while (ix < tabSize)
            entries[ix++] = entries[ix-1];
    }

    public static class SeekEntry implements Comparable<SeekEntry> {

        public final int durationMs;
        public final long byteOffset;

        public SeekEntry(int durationMs, long byteOffset) {
            this.durationMs = durationMs;
            this.byteOffset = byteOffset;
        }

        @Override
        public int compareTo(SeekEntry o) {
            return durationMs - o.durationMs;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof SeekEntry)
                return (durationMs == ((SeekEntry) o).durationMs);
            return super.equals(o);
        }
    }
}