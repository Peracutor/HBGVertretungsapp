package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.fcm.Subscriptions;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;

/**
 * Created by Micha.
 * 22.05.2016
 */
public class Whitelist extends SavedList {

    private static final String WHITELIST_ITEMS = "WhitelistItems";

    private boolean pushEnabled;

    private Subscriptions subscriptions;

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
        pushEnabled = Subscriptions.isEnabled(context);
        if (pushEnabled) subscriptions = Subscriptions.newInstance(context);
    }

    @Override
    public boolean save() {
        if (pushEnabled) {
            subscriptions.saveEdits();
        }
        return super.save();
    }

    @Override
    public boolean add(String subject) {
        if (pushEnabled) {
            subscriptions.add(subject);
        }
        return super.add(subject);
    }

    @Override
    public boolean remove(Object o) {
        if (pushEnabled) {

            subscriptions.remove((String) o);
        }
        return super.remove(o);
    }

    @Override
    public String remove(int index) {
        System.out.println("Whitelist.remove");
        if (pushEnabled) {
            String subject = get(index);
            System.out.println("subject = " + subject);
            subscriptions.remove(subject);
        }
        return super.remove(index);
    }

    public static boolean isWhitelistModeActive(Context context) {
        return Preferences.getDefaultPreferences(context).getString(Preferences.Key.WHITELIST_SWITCH, "blacklist").equals("whitelist");
    }

    public static void enableWhitelistMode(boolean enable, Context context) {
        Preferences defPrefs = Preferences.getDefaultPreferences(context);
        defPrefs.edit().putString(Preferences.Key.WHITELIST_SWITCH, enable ? "whitelist" : "blacklist").apply();
        if (Subscriptions.isEnabled(context)) Subscriptions.newInstance(context).resetSubscriptions();
    }

    public Subscriptions getSubscriptions() {
        return subscriptions;
    }
}
