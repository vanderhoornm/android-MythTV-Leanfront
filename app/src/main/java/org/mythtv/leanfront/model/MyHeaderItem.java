package org.mythtv.leanfront.model;

import androidx.leanback.widget.HeaderItem;

// This class is used for row headers as well as for group cells

public class MyHeaderItem extends HeaderItem implements ListItem {
    // For type values see MainFragment
    private int mItemType;

    public MyHeaderItem(String name, int type) {
        super(name);
        mItemType = type;
    }
    public int getItemType() { return mItemType; }
}
