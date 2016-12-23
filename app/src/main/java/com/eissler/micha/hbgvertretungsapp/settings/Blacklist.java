package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

/**
 * Created by Micha.
 * 21.12.2016
 */

public class Blacklist extends SavedList {
    private static final String BLACKLIST_ITEMS = "BlacklistItems";

    private Blacklist(Context context) {
        super(BLACKLIST_ITEMS, context);
    }

    private Blacklist(int initialCapacity, Context context) {
        super(BLACKLIST_ITEMS, initialCapacity, context);
    }

    public static Blacklist get(Context context) {
        return new Blacklist(context);
    }

    public static Blacklist get(Context context, int capacity) {
        return new Blacklist(capacity, context);
    }
}
