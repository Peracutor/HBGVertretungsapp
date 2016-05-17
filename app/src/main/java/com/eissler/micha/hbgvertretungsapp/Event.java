package com.eissler.micha.hbgvertretungsapp;

import android.widget.ListView;

/**
 * Created by Micha.
 * 08.05.2016
 */
public class Event {

    public static class ListViewChange {
        private final ListView listView;

        public ListViewChange(ListView listView) {
            this.listView = listView;
        }

        public ListView getListView() {
            return listView;
        }
    }

    public static class LoadPermission {
        private int position;

        public LoadPermission(int position) {
            this.position = position;
        }

        public boolean isPermitted(int position) {
            return position == this.position;
        }
    }

    public static class LoadPermissionRequest {}



    public static class CanScrollUp {
        private boolean canScrollUp;

        public CanScrollUp(boolean canScrollUp) {
            this.canScrollUp = canScrollUp;
        }

        public boolean canScrollUp() {
            return canScrollUp;
        }
    }

    public static class CanScrollUpRequest {}

    public static class ResetRequest {}
}
