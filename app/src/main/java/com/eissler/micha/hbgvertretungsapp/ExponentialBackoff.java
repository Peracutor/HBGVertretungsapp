package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;

/**
 * Created by Micha.
 * 15.09.2016
 */
public abstract class ExponentialBackoff {

    private final Preferences sharedPreferences;
    private final Preferences.Key preferenceName;
    private int retries;
    private long value;

    public ExponentialBackoff(Context context) {
        sharedPreferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, context);
        this.preferenceName = getPreferenceName();
        value = sharedPreferences.getLong(preferenceName, getStartValue() / 2);
        retries = (int) (Math.log(2 * value / getStartValue()) / Math.log(2));
    }

    protected abstract Preferences.Key getPreferenceName();

    protected abstract long getStartValue();

    protected int getMultiplier() {
        return 2;
    }

    protected abstract int getMaxRetries();


    public boolean retry() {
        System.out.println("ExponentialBackoff.retry");
        value *= getMultiplier();
        sharedPreferences.edit().putLong(preferenceName, value).apply();
        retries++;
        System.out.println("retries = " + retries);
        System.out.println("value = " + value);
        return retries <= getMaxRetries();
    }

    public long getValue() {
        return value;
    }


    public void reset() {
        value = getStartValue() / 2;
        retries = 0;
        sharedPreferences.edit().putLong(preferenceName, value).apply();
    }
}
