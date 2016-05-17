package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomListView extends ListView {

    public CustomListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean canScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            return getChildCount() > 0
                    && (getFirstVisiblePosition() > 0 || getChildAt(0)
                    .getTop() < getPaddingTop());
        } else {
            return ViewCompat.canScrollVertically(this, -1);
        }
    }
}
