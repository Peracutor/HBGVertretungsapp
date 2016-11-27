package com.eissler.micha.hbgvertretungsapp.settings;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.eissler.micha.hbgvertretungsapp.AlarmReceiver;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.BootReceiver;
import com.eissler.micha.hbgvertretungsapp.HbgApplication;
import com.eissler.micha.hbgvertretungsapp.Notifications;
import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.UpdateCheck;
import com.eissler.micha.hbgvertretungsapp.fcm.PushNotifications;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

public class SettingsFragment extends PreferenceFragment {

    private final static String NOTIFICATION_SWITCH = "notification_switch";
    private final static String WHITELIST_SWITCH = "whitelist_switch";
    private final static String UPDATE = "update";
    private final static String ALARM_TIME_1 = "time_for_alarm_1";
    private final static String ALARM_TIME_2 = "time_for_alarm_2";
//    private static final String ABOUT = "about";
    private static final String PUSH_TEST = "push_test";
    private static final String AUTO_NAME = "auto-name";
    public static final String BATTERY_SAVER = "battery_saver";
    public static final String ALARM_WAKE_UP = "alarm_wake_up";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_screen);

        final TimePreference timePreference1 = (TimePreference) findPreference(ALARM_TIME_1);
        final TimePreference timePreference2 = (TimePreference) findPreference(ALARM_TIME_2);

        Preference.OnPreferenceChangeListener timePickerListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Notifications.newInstance(getActivity()).reset(preference == timePreference1 ? Notifications.ALARM_1 : Notifications.ALARM_2, (long) newValue);
                return true;
            }
        };

        timePreference1.setOnPreferenceChangeListener(timePickerListener);
        timePreference2.setOnPreferenceChangeListener(timePickerListener);


        findPreference(NOTIFICATION_SWITCH).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean switchedOn = (boolean) newValue;

                Log.d(HbgApplication.HBG_APP, "Notifications toggled, they are now switched: " + (switchedOn ? "On" : "Off"));

                ComponentName receiver = new ComponentName(getActivity(), BootReceiver.class);

                int state = switchedOn ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

                getActivity().getPackageManager().setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP);

                Notifications.newInstance(getActivity()).enable(switchedOn);
                return true;
            }
        });

        final CheckBoxPreference pushSwitch = (CheckBoxPreference) findPreference(Preferences.Key.PUSH_NOTIFICATION_SWITCH.getKey());
        final Preference pushTestPreference = findPreference(PUSH_TEST);

        final int playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity());
        if (playServicesAvailable != ConnectionResult.SUCCESS) {
            pushSwitch.setEnabled(false);
            pushSwitch.setSummary("Google Play Dienste nicht verfügbar");
            pushTestPreference.setTitle("Google Play Dienste installieren");
            pushTestPreference.setSummary("Google Play Dienste installieren/aktualisieren, um Push-Benachrichtigungen empfangen zu können.");
            pushTestPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), playServicesAvailable, 20).show();
                    return true;
                }
            });
        } else {
            pushSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, Object newValue) {
                    boolean switchedOn = (boolean) newValue;
                    if (switchedOn) {

                        AlertDialog.Builder dialog = App.dialog("Push-Benachrichtigungen", null, getActivity());

                        final boolean whitelistModeActive = Whitelist.isWhitelistModeActive(getActivity());
                        if (!whitelistModeActive) {
                            dialog.setMessage("Um Push-Benachrichtungen zu erhalten, musst du den Whitelist-Modus aktivieren.")
                                    .setPositiveButton("Verstanden", null)
                                    .show();
                            return false;
                        } else {
                            dialog.setMessage("Du bekommst nun immer schnellstmöglich die neuesten Vertretungsmeldungen der Woche vom App-Server gesendet. Immer Freitags um 19:00 Uhr wird auf die nächste Woche gewechselt.")
                                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialogInterface) {
                                            showTestPushDialog();
                                        }
                                    })
                                    .show();

                            PushNotifications.newInstance(getActivity()).activate();
                        }
                        return true;
                    } else {
                        PushNotifications.newInstance(getActivity()).deactivate();
                    }
                    return true;
                }
            });
            pushTestPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    return showTestPushDialog();
                }
            });
        }

        final CheckBoxPreference whitelistSwitch = (CheckBoxPreference) findPreference(WHITELIST_SWITCH);

        final PreferenceScreen hiddenSubjects = (PreferenceScreen) findPreference("hidden_subjects");
        final PreferenceScreen whitelistSubjects = (PreferenceScreen) findPreference("whitelist_subjects");

        final boolean whitelistModeActive = Whitelist.isWhitelistModeActive(getActivity());
//        whitelistSubjects.setEnabled(whitelistModeActive);
        hiddenSubjects.setEnabled(!whitelistModeActive);

        final Preferences preferences = Preferences.getDefaultPreferences(getActivity());
        whitelistSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // TODO: 18.09.2016 make sure MainActivity gets updated when changed
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean switchedOn = (boolean) newValue;



//                Whitelist whitelist = Whitelist.get(getActivity());
//                if (switchedOn && whitelist.size() < 8) {
//                    String warning;
//                    String format = "Es sind %s Fächer gespeichert, die angezeigt werden sollen!";
//                    if (whitelist.size() == 0) {
//                        warning = String.format(format, "keine");
//                    } else if (whitelist.size() == 1) {
//                        warning = "Es ist nur 1 Fach gespeichert, das angezeigt werden soll!";
//                    } else {
//                        warning = String.format(format, "nur " + whitelist.size());
//                    }
//
//                    App.dialog("Achtung!", warning + "\n\nWillst du zur Whitelist-Einstellungsseite wechseln?", getActivity())
//                        .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                startActivity(new Intent(getActivity(), WhitelistSubjects.class));
//                            }
//                        })
//                        .setNegativeButton("Nein", null)
//                            .show();
//                } else
                if (switchedOn) {
//                    Toast.makeText(getActivity(), "", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getActivity(), WhitelistSubjects.class));
                } else if (preferences.getBoolean(Preferences.Key.PUSH_NOTIFICATION_SWITCH, false)) {
                    App.dialog("Achtung!", "Wenn du den Whitelist-Modus deaktivierst, wirst du keine Push-Benachrichtigungen mehr erhalten.\n\nTrotzdem deaktivieren?", getActivity())
                        .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pushSwitch.setChecked(false);
                                whitelistSwitch.setChecked(false);
                                hiddenSubjects.setEnabled(true);
                            }
                        })
                        .setNegativeButton("Nein", null)
                        .show();
                    return false;
                }
                Log.d(HbgApplication.HBG_APP, "Whitelist-mode toggled, is now switched: " + (switchedOn ? "On" : "Off"));

                hiddenSubjects.setEnabled(!switchedOn);
                whitelistSubjects.setEnabled(switchedOn);

                return true;
            }
        });

        findPreference(AUTO_NAME).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                App.dialog("Automatische Fachbenennung",
                        "Sollen alle Fächer deiner Whitelist, die du noch nicht selbst umbenannt hast, automatisch umbenannt werden? Alle Abkürzungen werden dabei ausgeschrieben (z.B. \"D\" zu \"Deutsch\", \"GGeN2\" zu \"Geschichte\").",
                        getActivity())
                        .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int renamings = AutoName.autoNameWhitelist(getActivity());
                                Toast.makeText(getActivity(), "Es wurden " + renamings + " Fächer umbenannt", Toast.LENGTH_LONG).show();
                                getActivity().startActivity(new Intent(getActivity(), CustomNamesList.class));
                            }
                        })
                        .setNegativeButton("Abbrechen", null)
                        .show();
                return true;
            }
        });

        findPreference(UPDATE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(HbgApplication.HBG_APP, "Check for Updates - Preference clicked");
                new UpdateCheck(getActivity());
                return true;
            }
        });

        findPreference(ALARM_WAKE_UP).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Notifications.newInstance(getActivity()).enable(); // reset alarms to use the new alarm-type
                return true;
            }
        });

        findPreference(BATTERY_SAVER).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showBatterySaverInfo();
                return true;
            }
        });

//        Preference aboutPref = findPreference(ABOUT);
//        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                new LibsBuilder() // TODO: 24.10.2016 about section
//                        .withFields(R.string.class.getFields())
//                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
////                        .withActivityTheme(R.style.HbgTheme)
//                        .start(getActivity());
//
//                return true;
//            }
//        });

    }

    private boolean showTestPushDialog() {
        if (!App.isConnected(getActivity())) {
            App.dialog("Keine Internetverbindung", "Stelle eine Internetverbindung her.", getActivity()).show();
            return true;
        }
        App.dialog("Push-Benachrichtigung testen", "Soll eine Testbenachrichtigung an dich gesendet werden?\n\nSchließe die App, um zu sehen, ob du Nachrichten im Hintergrund empfangen kannst.", getActivity()) // TODO: 20.10.2016 Wortwahl
                .setPositiveButton("Ja, testen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//                                new TestPushRequestProcessor(getActivity()).executeAsync(FirebaseInstanceId.getInstance().getToken());
                        Toast.makeText(getActivity(), "In 30 Sekunden wird die Anfrage gesendet", Toast.LENGTH_LONG).show();

                        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                        intent.setAction("alarm.request_test_push");
                        intent.putExtra("token", FirebaseInstanceId.getInstance().getToken());
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 3, intent, 0);
                        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 30 * 1000, pendingIntent);
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
        return true;
    }

    private void showBatterySaverInfo() {

//        final Preferences preferences = Preferences.getDefaultPreferences(getActivity());
//        if (!preferences.getBoolean(Preferences.Key.SHOW_INFO, true)) {
//            return;
//        }
        App.dialog("Energiespar-Apps",
                "Falls du keine Benachrichtigungen von dieser App erhältst, kann das an einer Energiespar-App liegen, die Hintegrundprozesse automatisch beendet, um deinen Akku zu schonen. " +
                        "Setze in diesem Fall die HBG-App auf die Liste der Apps, die nicht beendet werden sollen, um Benachrichtigungen zu erhalten.",
                getActivity())
                .setCancelable(false)
                .setPositiveButton("Verstanden", null /*new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        preferences.edit().putBoolean(Preferences.Key.SHOW_INFO, false).apply();
                    }
                }*/)
                .show();
    }

}
