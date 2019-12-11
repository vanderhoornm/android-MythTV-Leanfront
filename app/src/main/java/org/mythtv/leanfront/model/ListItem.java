package org.mythtv.leanfront.model;

public interface ListItem {

    int getItemType();
    String getName();
    default String getBaseName() {return null;}
}
