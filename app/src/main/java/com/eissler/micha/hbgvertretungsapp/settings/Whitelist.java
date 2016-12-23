package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.fcm.PushNotifications;

/**
 * Created by Micha.
 * 22.05.2016
 */
public class Whitelist extends SavedList {

    private static final String WHITELIST_ITEMS = "WhitelistItems";

    private boolean pushEnabled;

    private PushNotifications pushNotifications;

    public static Whitelist get(Context context) {
        return new Whitelist(context);
    }

    public static Whitelist get(Context context, int capacity) {
        return new Whitelist(context, capacity);
    }

    private Whitelist(Context context) {
        this(context, 10);
    }

    private Whitelist(Context context, int capacity) {
        super(WHITELIST_ITEMS, capacity, context);
        pushEnabled = PushNotifications.isEnabled(context);
    }

    private boolean pushEnabled() {
        if (pushNotifications == null) {
            pushNotifications = PushNotifications.newInstance(context);
        }
        return pushEnabled;
    }

    @Override
    public boolean save() {
        if (pushEnabled()) {
            pushNotifications.saveEdits();
        }
        return super.save();
    }

    @Override
    public boolean add(String subject) {
        if (pushEnabled()) {
            pushNotifications.add(subject);
        }
        return super.add(subject);
    }

    @Override
    public String set(int index, String subject) {
        String previousSubject = super.set(index, subject);
        if (pushEnabled()) {
            pushNotifications.remove(previousSubject);
            if (subject != null) {
                pushNotifications.add(subject);
            }
        }

        return previousSubject;
    }

    //    public boolean isWhitelistModeActive() {
//        return isWhitelistModeActive(context);
//    }

    public static boolean isWhitelistModeActive(Context context) {
        return Preferences.getDefaultPreferences(context).getBoolean(Preferences.Key.WHITELIST_SWITCH, false);
    }

    public static void enable(boolean enable, Context context) {
        Preferences.getDefaultPreferences(context).edit().putBoolean(Preferences.Key.WHITELIST_SWITCH, enable).apply();
        if (!enable) {
            PushNotifications.newInstance(context).deactivate();
        }

    }
}
