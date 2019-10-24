package org.mythtv.leanfront.ui;

import androidx.leanback.widget.HeaderItem;

public class MyHeaderItem extends HeaderItem {
    private int mItemType;
    static public final int ITEMTYPE_GROUP = 1;
    static public final int ITEMTYPE_SETTINGS = 2;
    static public final int ITEMTYPE_DIRECTORY = 3;

    public MyHeaderItem(String name, int type) {
        super(name);
        mItemType = type;
    }

    public int getItemType() { return mItemType; }
}
