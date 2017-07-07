package com.eissler.micha.hbgvertretungsapp.settings;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eissler.micha.hbgvertretungsapp.AlarmReceiver;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.BootReceiver;
import com.eissler.micha.hbgvertretungsapp.HbgApplication;
import com.eissler.micha.hbgvertretungsapp.Notifications;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.RequestCodes;
import com.eissler.micha.hbgvertretungsapp.UpdateCheck;
import com.eissler.micha.hbgvertretungsapp.fcm.Subscriptions;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;
import com.peracutor.hbgserverapi.AutoName;

import java.util.Arrays;
import java.util.List;

import static com.eissler.micha.hbgvertretungsapp.util.Preferences.Key.AUTO_NAME;
import static com.eissler.micha.hbgvertretungsapp.util.Preferences.Key.AUTO_NAME_PATTERN;

public class SettingsFragment extends PreferenceFragment {

    private final static String NOTIFICATION_SWITCH = "notification_switch";
    private final static String WHITELIST_SWITCH = "whitelist_switch";
    private final static String UPDATE = "update";
    private final static String ALARM_TIME_1 = "time_for_alarm_1";
    private final static String ALARM_TIME_2 = "time_for_alarm_2";
//    private static final String ABOUT = "about";
    private static final String PUSH_TEST = "push_test";
    public static final String BATTERY_SAVER = "battery_saver";
    public static final String ALARM_WAKE_UP = "alarm_wake_up";
    public static final String DEFAULT_CUSTOM_PATTERN = "*f (*k*n)";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_screen);

        final TimePreference timePreference1 = (TimePreference) findPreference(ALARM_TIME_1);
        final TimePreference timePreference2 = (TimePreference) findPreference(ALARM_TIME_2);

        Preference.OnPreferenceChangeListener timePickerListener = (preference, newValue) -> {
            Notifications.newInstance(getActivity()).reset(preference == timePreference1 ? RequestCodes.ALARM_1 : RequestCodes.ALARM_2, (long) newValue);
            return true;
        };

        timePreference1.setOnPreferenceChangeListener(timePickerListener);
        timePreference2.setOnPreferenceChangeListener(timePickerListener);


        findPreference(NOTIFICATION_SWITCH).setOnPreferenceChangeListener((preference, newValue) -> {

            boolean switchedOn = (boolean) newValue;

            Log.d(HbgApplication.HBG_APP, "Notifications toggled, they are now switched: " + (switchedOn ? "On" : "Off"));

            ComponentName receiver = new ComponentName(getActivity(), BootReceiver.class);

            int state = switchedOn ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            getActivity().getPackageManager().setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP);

            Notifications.newInstance(getActivity()).enable(switchedOn);
            return true;
        });

        final CheckBoxPreference pushSwitch = (CheckBoxPreference) findPreference(Preferences.Key.PUSH_NOTIFICATION_SWITCH.getKey());
        final Preference pushTestPreference = findPreference(PUSH_TEST);

        final int playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity());
        if (playServicesAvailable != ConnectionResult.SUCCESS) {
            pushSwitch.setEnabled(false);
            pushSwitch.setSummary("Google Play Dienste nicht verfügbar (s. Fehlerbehebung unten)");
            pushTestPreference.setTitle("Google Play Dienste installieren");
            pushTestPreference.setSummary("Google Play Dienste installieren/aktualisieren, um Echtzeit-Benachrichtigungen empfangen zu können.");
            pushTestPreference.setOnPreferenceClickListener(preference -> {
                GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), playServicesAvailable, 20).show();
                return true;
            });
        } else {
            pushSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean switchedOn = (boolean) newValue;
                Subscriptions subscriptions = Subscriptions.newInstance(getActivity());
                if (!switchedOn) {
                    subscriptions.unsubscribe();
                    return true;
                } else {
                    final Preferences preferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getActivity());
                    if (!preferences.getBoolean(Preferences.Key.TEST_PUSH_PROMPTED, false)) {
                        preferences.edit().putBoolean(Preferences.Key.TEST_PUSH_PROMPTED, true).apply();
                        showTestPushDialog();
                    }
                    subscriptions.subscribe();
                }
                return true;
            });
            pushTestPreference.setOnPreferenceClickListener(preference -> showTestPushDialog());
        }

        findPreference(Preferences.Key.WEEK_CHANGE_NOTIFICATION.getKey()).setOnPreferenceChangeListener((preference, o) -> {
            boolean switchedOn = (boolean) o;
            Subscriptions subscriptions = Subscriptions.newInstance(getActivity());
            subscriptions.changeWeekChangeSubscription(switchedOn);
            subscriptions.printSubs();
            return true;
        });

        final ListPreference filterModePreference = (ListPreference) findPreference(WHITELIST_SWITCH);

        final Preference hiddenSubjects = findPreference("hidden_subjects");
        final Preference whitelistSubjects = findPreference("whitelist_subjects");

        final boolean whitelistModeActive = Whitelist.isWhitelistModeActive(getActivity());

        hiddenSubjects.setEnabled(!whitelistModeActive);
        whitelistSubjects.setEnabled(whitelistModeActive);
        final String[] filterEntries = getResources().getStringArray(R.array.filter_mode_entries);
        filterModePreference.setSummary(whitelistModeActive ? filterEntries[1] : filterEntries[0]);

        filterModePreference.setOnPreferenceClickListener(preference -> {
            Preferences preferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getActivity());
            if (!preferences.getBoolean(Preferences.Key.DONT_PROMPT_FILTER_MODE, false)) {
                new MaterialDialog.Builder(getActivity())
                        .title("Optionen zum Filtern")
                        .content("Damit dir nur für dich relevante Fächer angezeigt werden, gibt es zwei Möglichkeiten:\n\n" +
                                "1. Einzeln die Fächer, die du nicht hast, verstecken (wenn du sie siehst).\n\n" +
                                "2. Einmalig alle Fächer, die du hast, einspeichern.")
                        .positiveText("Ok")
                        .checkBoxPrompt("Nicht mehr anzeigen", true, null)
                        .onPositive((dialog, which) -> {
                            if (dialog.isPromptCheckBoxChecked()) {
                                preferences.edit().putBoolean(Preferences.Key.DONT_PROMPT_FILTER_MODE, true).apply();
                            }
                        })
                        .show();
            }
            return true;
        });

        filterModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean whitelistMode = newValue.equals("whitelist");
            Log.d(HbgApplication.HBG_APP, "Whitelist-mode is now switched: " + (whitelistMode ? "On" : "Off"));

            preference.setSummary(whitelistMode ? filterEntries[1] : filterEntries[0]);

            hiddenSubjects.setEnabled(!whitelistMode);
            whitelistSubjects.setEnabled(whitelistMode);

            if (Subscriptions.isEnabled(getActivity())) {
                Subscriptions.newInstance(getActivity(), whitelistMode).resetSubscriptions();
            }

            if (whitelistMode) {
                startActivity(new Intent(getActivity(), WhitelistSubjects.class));
            }
            return true;
        });

        ShowableListPreference patternPreference = (ShowableListPreference) findPreference(AUTO_NAME_PATTERN.getKey());

        int selectedClass = App.getSelectedClass(getActivity());
        if (selectedClass != 21 && selectedClass != 22) {
            patternPreference.setEnabled(false);
        } else {
            String autoNamePattern = Preferences.getDefaultPreferences(getActivity()).getString(AUTO_NAME_PATTERN, null);
            updatePatternPreference(autoNamePattern, patternPreference);

            patternPreference.setOnPreferenceChangeListener((preference, o) -> {
                String pattern = (String) o;

                ListPreference patternPref = (ListPreference) preference;

                CharSequence[] entryValues = patternPref.getEntryValues();
                if (pattern.equals(DEFAULT_CUSTOM_PATTERN) || pattern.equals(entryValues[entryValues.length - 1])) {
                    final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_custom_pattern, null);
                    final TextView label = (TextView) dialogView.findViewById(R.id.edit_text_label);
                    final EditText editText = (EditText) dialogView.findViewById(R.id.edit_text_custom_pattern);

                    editText.setText(pattern);
                    editText.setSelection(pattern.length());
                    String text = "Benutze \"*f\" als Platzhalter für den Namen des Fachs, \"*k\" für die Kursart (GK/LK)  und \"*n\" für die Kursnummer.\n\nAktuelles Muster:\nLEkN2 wird zu:";
                    label.setText(String.format("%s %s", text, new AutoName(pattern).getAutoName("LEkA2")));

                    final MaterialDialog dialog = new MaterialDialog.Builder(getActivity()).customView(dialogView, true)
                            .title("Eigenes Muster")
                            .positiveText("Speichern")
                            .onPositive((a, b) -> {
                                String pattern1 = editText.getText().toString();
                                updatePatternPreference(pattern1, patternPref);
                            })
                            .negativeText("Abbrechen")
                            .show();

                    if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);


                    editText.addTextChangedListener(new InputValidator.DisablerEditTextValidator(editText, dialog.getActionButton(DialogAction.POSITIVE)) {
                        @Override
                        public CharSequence validate(CharSequence pattern) {
                            if (pattern.toString().contains("*f")) {
                                label.setText(String.format("%s %s", text, new AutoName(pattern.toString()).getAutoName("LEkA2")));
                                return null;
                            } else {
                                label.setText(text);
                                return "Muss \"*f\" enthalten";
                            }
                        }
                    });
                    return false;
                }
                return true;
            });
        }

        findPreference(AUTO_NAME.getKey()).setOnPreferenceChangeListener((preference, o) -> {
            int classNumber = App.getSelectedClass(getActivity());
            if ((Boolean) o && (classNumber == 21 || classNumber == 22)) {
                patternPreference.show();
            }
            return true;
        });

        findPreference(UPDATE).setOnPreferenceClickListener(preference -> {
            Log.d(HbgApplication.HBG_APP, "Check for Updates - Preference clicked");
            new UpdateCheck(getActivity());
            return true;
        });

        findPreference(ALARM_WAKE_UP).setOnPreferenceChangeListener((preference, newValue) -> {
            Notifications.newInstance(getActivity()).enable(); // reset alarms to use the new alarm-type
            return true;
        });

        findPreference(BATTERY_SAVER).setOnPreferenceClickListener(preference -> {
            showBatterySaverInfo();
            return true;
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

    private void updatePatternPreference(String autoNamePattern, ListPreference patternPreference) {
        final List<CharSequence> entryValues = Arrays.asList(patternPreference.getEntryValues());
        final List<CharSequence> entries = Arrays.asList(patternPreference.getEntries());

        if (autoNamePattern != null && !entryValues.contains(autoNamePattern)) {
            entryValues.set(entryValues.size() - 1, autoNamePattern);
            patternPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

            entries.set(entries.size() - 1, new com.peracutor.hbgserverapi.AutoName(autoNamePattern).getAutoName("LEkA2") + " (eigenes)");
            patternPreference.setEntries(entries.toArray(new CharSequence[0]));

            patternPreference.setValue(autoNamePattern);
        } else if (autoNamePattern != null && autoNamePattern.equals(entryValues.get(entryValues.size() - 1))) {
            patternPreference.setValue(autoNamePattern);
        }
    }

    private boolean showTestPushDialog() {
        if (!App.isConnected(getActivity())) {
            App.dialog("Keine Internetverbindung", "Stelle eine Internetverbindung her.", getActivity()).show();
            return true;
        }
        App.dialog("Echtzeit-Benachrichtigung testen", "Zum Test kann der App-Server eine Nachricht an dich senden. Jetzt testen?\n\n(Schließe die App, um zu sehen, ob du Nachrichten auch im Hintergrund empfangen kannst)", getActivity())
                .setPositiveButton("Testen", (dialogInterface, i) -> {
                    Toast.makeText(getActivity(), "In 20 Sekunden wird die Anfrage gesendet", Toast.LENGTH_LONG).show();

                    AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                    intent.setAction("alarm.request_test_push");
                    intent.putExtra("token", FirebaseInstanceId.getInstance().getToken());
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), RequestCodes.ALARM_REQUEST_TEST_PUSH, intent, 0);
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 20 * 1000, pendingIntent);
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

    @Override
    public void onResume() {
        super.onResume();
        final String[] filterEntries = getResources().getStringArray(R.array.filter_mode_entries);
        ListPreference filterModePreference = (ListPreference) findPreference(WHITELIST_SWITCH);
        boolean whitelistModeActive = Whitelist.isWhitelistModeActive(getActivity());
        filterModePreference.setSummary(whitelistModeActive ? filterEntries[1] : filterEntries[0]);
        findPreference("hidden_subjects").setEnabled(!whitelistModeActive);
        findPreference("whitelist_subjects").setEnabled(whitelistModeActive);
    }
}
