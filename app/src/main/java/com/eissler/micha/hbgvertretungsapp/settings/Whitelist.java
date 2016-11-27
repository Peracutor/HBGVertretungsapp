package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.eissler.micha.hbgvertretungsapp.fcm.PushNotifications;

import org.acra.ACRA;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Micha.
 * 22.05.2016
 */
public class Whitelist extends ArrayList<String> {

    private static final String WHITELIST_ITEMS = "WhitelistItems";


    private final Context context;
    private boolean pushEnabled;

    private PushNotifications pushNotifications;

    public static Whitelist get(Context context) {
        return new Whitelist(context);
    }

    public static Whitelist get(Context context, int capacity) {
        return new Whitelist(context, capacity);
    }

    private Whitelist(Context context) {
        this(context, 0);
    }

    private Whitelist(Context context, int capacity) {
        super(capacity);
        this.context = context;

        ArrayList<String> whitelist = null;
        try {
            whitelist = App.retrieveObject(WHITELIST_ITEMS, context);
        } catch (FileNotFoundException e) {
            System.out.println("No Whitelist saved: " + e.getMessage());
        } catch (ClassNotFoundException | IOException e) {
            App.exitWithError(e);
            return;
        }

        if (whitelist != null) {
            this.addAll(whitelist);
        }

        pushEnabled = PushNotifications.isEnabled(context);
    }

    public boolean save() {
        if (contains(null)) removeAll(Collections.singleton(null));

        if (pushEnabled()) {
            pushNotifications.saveEdits();
        }

        try {
            App.writeObject(new ArrayList<>(this), WHITELIST_ITEMS, context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
        return false;
    }

    private boolean pushEnabled() {
        if (pushNotifications == null) {
            pushNotifications = PushNotifications.newInstance(context);
        }
        return pushEnabled;
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
