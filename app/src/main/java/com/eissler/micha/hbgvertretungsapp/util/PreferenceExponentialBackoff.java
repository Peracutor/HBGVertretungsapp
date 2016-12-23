package com.eissler.micha.hbgvertretungsapp.util;

import android.content.SharedPreferences;

/**
 * Created by Micha.
 * 15.09.2016
 */
public class PreferenceExponentialBackoff extends ExponentialBackoff {

    public PreferenceExponentialBackoff(long startValue, double multiplier, int maxRetries, final String preferenceName, final SharedPreferences sharedPreferences) {
        super(startValue, multiplier, maxRetries, new Saver() {
            @Override
            public void save(long value) {
                sharedPreferences.edit().putLong(preferenceName, value).apply();
            }

            @Override
            public Long getSavedValue() {
                return sharedPreferences.getLong(preferenceName, 0);
            }
        });
    }
}
