package com.eissler.micha.hbgvertretungsapp;

/**
 * Created by Micha.
 * 08.05.2016
 */
public class Event {

    public static class LoadPermission {
        private int position;

        public LoadPermission(int position) {
            this.position = position;
        }

        public boolean isPermitted(int position) {
            return position == this.position;
        }
    }

    public static class LoadPermissionRequest {
        private final int sender;

        public LoadPermissionRequest(int sender) {
            this.sender = sender;
        }

        public int getSender() {
            return sender;
        }
    }



    public static class CanScrollUp {

        private boolean canScrollUp;

        public CanScrollUp(boolean canScrollUp) {
            this.canScrollUp = canScrollUp;
        }
        public boolean canScrollUp() {
            return canScrollUp;
        }
    }

    public static class CanScrollUpRequest {
        private int position;

        public CanScrollUpRequest(int position) {
            this.position = position;
        }

        public int forPosition() {
            return position;
        }
    }

    public static class ResetRequest {}

    public static class FinishActionMode {}

    public static class RefreshRequest {}

    public static class Exception {

        private java.lang.Exception e;

        public Exception(java.lang.Exception e) {
            this.e = e;
        }

        public java.lang.Exception getException() {
            return e;
        }
    }
}
