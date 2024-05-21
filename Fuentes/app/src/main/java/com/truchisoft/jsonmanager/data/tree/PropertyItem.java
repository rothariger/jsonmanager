package com.truchisoft.jsonmanager.data.tree;

import com.truchisoft.jsonmanager.R;

/**
 * Created by maximiliano.schmidt on 27/10/2015.
 */
public class PropertyItem extends BaseItem {
    public String value;

    public PropertyItem(String text) {
        super(R.string.ic_property, text);
    }

    public PropertyItem(String text, String value) {
        this(text);
        this.value = value;
    }
}
