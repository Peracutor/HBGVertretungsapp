package com.eissler.micha.hbgvertretungsapp.settings;


import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.BootReceiver;
import com.eissler.micha.hbgvertretungsapp.CheckForUpdate;
import com.eissler.micha.hbgvertretungsapp.HbgApplication;
import com.eissler.micha.hbgvertretungsapp.MainActivity;
import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.orhanobut.logger.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends PreferenceFragment {

    final static String NOTIFICATION_SWITCH = "notification_switch";
    final static String WHITELIST_SWITCH = "whitelist_switch";
    public final static String ALARM_TIME_1 = "time_for_alarm_1";
    public final static String ALARM_TIME_2 = "time_for_alarm_2";
    final static String UPDATE = "update";
    private static final String ABOUT = "about";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_screen);


        final Context context = getActivity();

        final SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);



        final TimePreference timePreference1 = (TimePreference) findPreference(ALARM_TIME_1);

        timePreference1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(HbgApplication.HBG_APP, "Set timePreference1");
                setAlarm(0, (long) newValue);
                return true;
            }


        });

        final TimePreference timePreference2 = (TimePreference) findPreference(ALARM_TIME_2);

        timePreference2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(HbgApplication.HBG_APP, "Set timePreference2");
                setAlarm(1, (long) newValue);
                return true;
            }


        });

        final CheckBoxPreference notificationSwitch = (CheckBoxPreference) findPreference(NOTIFICATION_SWITCH);
        boolean notifyBoolean = defaultPrefs.getBoolean(NOTIFICATION_SWITCH, true);
        notificationSwitch.setSummary(notifyBoolean ? "An" : "Aus");
        timePreference1.setEnabled(notifyBoolean);
        timePreference2.setEnabled(notifyBoolean);

        notificationSwitch.setChecked(notifyBoolean);

        notificationSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean switchedOn = (boolean) newValue;
                Log.d(HbgApplication.HBG_APP, "Notifications toggled, they are now switched: " + (switchedOn ? "On" : "Off"));
                //((TimePreference) newValue).getSummary();

                MainActivity.defaultPrefs.edit().putBoolean(NOTIFICATION_SWITCH, switchedOn).apply();

                timePreference1.setEnabled(switchedOn);
                timePreference2.setEnabled(switchedOn);
                preference.setSummary(switchedOn ? "An" : "Aus");

                ComponentName receiver = new ComponentName(context, BootReceiver.class);

                int state = switchedOn ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

                context.getPackageManager().setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP);

                if (!switchedOn) {
                    MainActivity.cancelAlarm(0, context);
                    MainActivity.cancelAlarm(1, context);
                } else {


                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMANY);

                    try {
                        calendar.setTime(new Date(defaultPrefs.getLong(ALARM_TIME_1, sdf.parse("07:00").getTime())));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }


                    MainActivity.setAlarm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0, context);

                    try {
                        calendar.setTime(new Date(defaultPrefs.getLong(ALARM_TIME_2, sdf.parse("19:00").getTime())));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    MainActivity.setAlarm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 1, context);
                }
                return true;
            }
        });




        final PreferenceScreen hiddenSubjects = (PreferenceScreen) findPreference("hidden_subjects");
        final PreferenceScreen whitelistSubjects = (PreferenceScreen) findPreference("whitelist_subjects");
        final CheckBoxPreference whitelistSwitch = (CheckBoxPreference) findPreference(WHITELIST_SWITCH);

        boolean whitelistBoolean = defaultPrefs.getBoolean(WHITELIST_SWITCH, true);
        whitelistSwitch.setSummary(whitelistBoolean ? "An" : "Aus");
        whitelistSwitch.setChecked(whitelistBoolean);

        whitelistSubjects.setEnabled(whitelistBoolean);
        hiddenSubjects.setEnabled(!whitelistBoolean);

        whitelistSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean switchedOn = (boolean) newValue;
                ArrayList<String> whiteListArray = WhitelistSubjects.getWhiteListArray(context);
                if (switchedOn && (whiteListArray == null || whiteListArray.size() < 8)) {
                    String warning;
                    if (whiteListArray == null || whiteListArray.size() == 0) {
                        warning = "Es sind keine Fächer gespeichert, die angezeigt werden sollen!";
                    } else if (whiteListArray.size() == 1) {
                        warning = "Es ist nur 1 Fach gespeichert, das angezeigt werden soll!";
                    } else {
                        warning = "Es sind nur " + whiteListArray.size() + " Fächer gespeichert, die angezeigt werden sollen!";
                    }

                    App.dialog("Achtung!", warning + "\n\nWillst du zur Whitelist-Einstellungsseite wechseln?", context)
                        .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(context, WhitelistSubjects.class));
                            }
                        })
                        .setNegativeButton("Nein", null)
                            .show();
                }
                Log.d(HbgApplication.HBG_APP, "Whitlist-mode toggled, is now switched: " + (switchedOn ? "On" : "Off"));

                MainActivity.defaultPrefs.edit().putBoolean(WHITELIST_SWITCH, switchedOn).apply();

                hiddenSubjects.setEnabled(!switchedOn);
                whitelistSubjects.setEnabled(switchedOn);
                preference.setSummary(switchedOn ? "An" : "Aus");

                return true;
            }
        });


        Preference updatePreference = findPreference(UPDATE);
        updatePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(HbgApplication.HBG_APP, "Check for Updates - Preference clicked");
                new CheckForUpdate(getActivity());
                return true;
            }
        });

        Preference aboutPref = findPreference(ABOUT);
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Logger.d("About Pref clicked");
                new LibsBuilder()
                        .withFields(R.string.class.getFields())
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
//                        .withActivityTheme(R.style.HbgTheme)
                        .start(context);

                return true;
            }
        });

    }

    private void setAlarm(int requestID, long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(millis));

        MainActivity.setAlarm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), requestID, getActivity().getApplicationContext());
    }

}
