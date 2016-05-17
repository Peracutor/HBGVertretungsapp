package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Micha.
 * 18.04.2016
 */
public class SwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {

    private boolean canScrollUp = false;

    public SwipeRefreshLayout(Context context) {
        super(context);
        init();
    }

    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean canChildScrollUp() {
//        return !canRefresh || viewPager != null && ((CustomListView) ((HbgPagerAdapter) viewPager.getAdapter()).getListView(viewPager.getCurrentItem())).canScrollUp();
        return canScrollUp;
    }

    @Subscribe
    public void setCanScrollUp(Event.CanScrollUp canScrollUpEvent) {
        System.out.println("CanScrollUp received");
        this.canScrollUp = canScrollUpEvent.canScrollUp();
    }

}
