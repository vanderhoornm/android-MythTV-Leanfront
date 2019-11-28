package org.mythtv.leanfront.model;

public interface ListItem {

    public int getItemType();
    public String getName();
    public default String getBaseName() {return null;}
}
