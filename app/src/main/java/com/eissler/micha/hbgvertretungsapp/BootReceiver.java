package com.eissler.micha.hbgvertretungsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eissler.micha.hbgvertretungsapp.settings.SettingsFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMANY);

            try {
                calendar.setTime(new Date(defaultPrefs.getLong(SettingsFragment.ALARM_TIME_1, sdf.parse("07:00").getTime())));
            } catch (ParseException e) {
                e.printStackTrace();
            }


            MainActivity.setAlarm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0, context);

            try {
                calendar.setTime(new Date(defaultPrefs.getLong(SettingsFragment.ALARM_TIME_2, sdf.parse("19:00").getTime())));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            MainActivity.setAlarm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 1, context);
        }
    }
}
