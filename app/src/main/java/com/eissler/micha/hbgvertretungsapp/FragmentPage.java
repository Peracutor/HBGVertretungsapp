package com.eissler.micha.hbgvertretungsapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.eissler.micha.hbgvertretungsapp.evaluation.DataEvaluation;
import com.eissler.micha.hbgvertretungsapp.evaluation.SortedCoverMessages;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class FragmentPage extends Fragment {

    static final String THIS_WEEK = "Diese Woche";

    private ListView listView;
    private ViewGroup view;
    private SortedCoverMessages mSortedCoverMessages;
    private boolean noData = false;
    private Integer position;
    private boolean alreadyPermitted = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Register fragment");
        EventBus.getDefault().register(FragmentPage.this);

        position = getArguments().getInt("position");
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        System.out.println("FragmentPage.onCreateView, position " + position);

        if (view == null) {
            view = (ViewGroup) inflater.inflate(
                    R.layout.fragment_page_layout, container, false);

            listView = (ListView) view.findViewById(R.id.list);
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == SCROLL_STATE_IDLE) {
                        EventBus.getDefault().post(new Event.CanScrollUp(((CustomListView) listView).canScrollUp()));
                    }
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }
            });

            listView.setAdapter(HbgListAdapter.getLoadingAdapter(getContext())); // TODO: 08.05.2016 loading adapter ?
        }

        if (noData) {
            listView.setAdapter(HbgListAdapter.getNoDataAdapter(getContext()));
            EventBus.getDefault().post(new Event.ListViewChange(listView)); // TODO: 03.05.2016 are that many ListViewChanges needed?
        } else if (mSortedCoverMessages != null) {
            resetPage();
        } else if (!alreadyPermitted) {
            System.out.println("Sending request");
            EventBus.getDefault().post(new Event.LoadPermissionRequest());
        }

        return view;
    }

    @Subscribe
    public void onLoadPermitted(Event.LoadPermission loadPermission) {
        System.out.println("alreadyPermitted for position " + position+ ": " +  alreadyPermitted);
        if (!alreadyPermitted && loadPermission.isPermitted(position) && mSortedCoverMessages == null && !noData) {
            alreadyPermitted = true;
            System.out.println("onLoadPermitted for position " + position);
            noData = false;
            new DataEvaluation(getContext(), position, new DataEvaluation.DataEvaluationInterface() {
                @Override
                public void onDataEvaluationComplete(DataEvaluation.DataEvaluationException e, SortedCoverMessages sortedCoverMessages) {
                    if (e != null) {
                        e.printStackTrace();
                        if (e.getErrorCode() == DataEvaluation.DataEvaluationException.NO_DATA) {
                            listView.setAdapter(HbgListAdapter.getNoDataAdapter(getContext()));
                            noData = true;
                            EventBus.getDefault().post(new Event.ListViewChange(listView));
                        } else {
                            EventBus.getDefault().post(e);
                        }
                        return;
                    }

                    listView.setAdapter(sortedCoverMessages.getFormattedListAdapter());
                    mSortedCoverMessages = sortedCoverMessages;
                    EventBus.getDefault().post(new Event.ListViewChange(listView));
                }
            });
        }

    }


    @Subscribe
    public void onResetRequested(Event.ResetRequest resetRequest) {
        if (mSortedCoverMessages != null) {
            resetPage();
        }
    }

    public void resetPage() {
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

        System.out.println("resetting listadapter");
        listView.setAdapter(mSortedCoverMessages.getFormattedListAdapter());

        listView.setSelectionFromTop(index, top);

        EventBus.getDefault().post(new Event.ListViewChange(listView));
    }

    @Subscribe
    public void onCanScrollUpRequested(Event.CanScrollUpRequest request) {
        boolean canScrollUp = false;
        if (listView != null) {
            canScrollUp = ((CustomListView) listView).canScrollUp();
        }
        EventBus.getDefault().post(new Event.CanScrollUp(canScrollUp));
    }

    public ListView getListView() {     // TODO: 03.05.2016 find a better way if possible
        return listView;
    }
}
