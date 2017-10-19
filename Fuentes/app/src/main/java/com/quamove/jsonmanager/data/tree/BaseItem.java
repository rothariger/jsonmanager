package com.quamove.jsonmanager.data.tree;

/**
 * Created by maximiliano.schmidt on 27/10/2015.
 */
public class BaseItem {
    public int icon;
    public String name;
    public Boolean isEdit;

    public BaseItem(int icon, String name) {
        this.icon = icon;
        this.name = name;
        this.isEdit = false;
    }

    public ArrayItem getAsArrayItem() {
        return (ArrayItem) this;
    }

    public ObjectItem getAsObjectItem() {
        return (ObjectItem) this;
    }

    public PropertyItem getAsPropertyItem() {
        return (PropertyItem) this;
    }

}
