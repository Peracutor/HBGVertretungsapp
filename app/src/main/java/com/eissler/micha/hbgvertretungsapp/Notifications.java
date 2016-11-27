package com.eissler.micha.hbgvertretungsapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Created by Micha on 12.06.2016.
 */
public class Notifications {
    public static final int ALARM_1 = 1;
    public static final int ALARM_2 = 2;

//    private static Notifications sInstance;
    private final Preferences prefs;
    private final AlarmManager alarmManager;
    private Context context;

    public Notifications(Context context) {
        this.context = context;
        prefs = Preferences.getDefaultPreferences(context);
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static Notifications newInstance(Context context) {
        return new Notifications(context);
    }

    public void enable(boolean enable) {
        if (enable) {
            enable();
        } else {
            disable();
        }
    }

    public void enable() {
        setAlarm(ALARM_1);
        setAlarm(ALARM_2);
        System.out.println("Notifications enabled");
    }

    public void disable() {
        alarmManager.cancel(getAlarmPendingIntent(ALARM_1));
        alarmManager.cancel(getAlarmPendingIntent(ALARM_2));
        System.out.println("Notifications disabled");
    }

    public void reset() {
        enable();
    }

    public void reset(int requestCode, long millis) {
        setAlarm(requestCode, millis);
    }

    private void setAlarm(int requestCode) {
        alarmManager.setRepeating(getAlarmType(), getAlarmTime(requestCode).getTimeInMillis(), AlarmManager.INTERVAL_DAY, getAlarmPendingIntent(requestCode));
    }

    private void setAlarm(int requestCode, long millis) {
        alarmManager.setRepeating(getAlarmType(), getAlarmTime(requestCode, millis).getTimeInMillis(), AlarmManager.INTERVAL_DAY, getAlarmPendingIntent(requestCode));
    }

    private Calendar getAlarmTime(int requestCode) {
        return getAlarmTime(requestCode, -1);
    }

    private Calendar getAlarmTime(int requestCode, long millis) {
        if (millis == -1) millis = prefs.getLong(requestCode == ALARM_1 ? Preferences.Key.ALARM_TIME_1 : Preferences.Key.ALARM_TIME_2, -1);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);

        if (millis == -1) {
            calendar.set(Calendar.HOUR_OF_DAY, requestCode == ALARM_1 ? 7 : 19);
            calendar.set(Calendar.MINUTE, 0);
        } else {
            Calendar saved = Calendar.getInstance();
            saved.setTimeInMillis(millis);
            calendar.set(Calendar.HOUR_OF_DAY, saved.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, saved.get(Calendar.MINUTE));
        }

        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) > calendar.get(Calendar.HOUR_OF_DAY) || (now.get(Calendar.HOUR_OF_DAY) == calendar.get(Calendar.HOUR_OF_DAY) && now.get(Calendar.MINUTE) > calendar.get(Calendar.MINUTE))) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        System.out.printf("AlarmTime %s: %s\n", requestCode, App.PRECISE_SDF.format(calendar.getTime()));
        return calendar;
    }

    PendingIntent getAlarmPendingIntent(int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("alarm.notification");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int getAlarmType() {
        Preferences preferences = Preferences.getDefaultPreferences(context);
        return preferences.getBoolean(Preferences.Key.ALARM_WAKEUP, false) ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;
    }
}
