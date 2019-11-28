package org.mythtv.leanfront.model;

import androidx.leanback.widget.HeaderItem;

// This class is used for row headers as well as for group cells

public class MyHeaderItem extends HeaderItem implements ListItem {
    // For type values see MainFragment
    private int mItemType;
    private String mBaseName;

    public MyHeaderItem(String name, int type, String baseName) {
        super(name);
        mItemType = type;
        mBaseName = baseName;
    }
    public int getItemType() { return mItemType; }
    public String getBaseName() { return mBaseName; }
}
