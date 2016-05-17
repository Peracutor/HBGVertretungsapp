package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.Map;

public class FilterDialog {

    private static final String DISPLAY_NAME = "Anzeigename für \"%s\":";

    private final AlertDialog.Builder mBuilder;
    private final EditText kursName;
    private final InputMethodManager inputManager;

    public FilterDialog(String subject, PostExecuteInterface postExecuteInterface, Context context) {
        this(subject, null, postExecuteInterface, context);
    }

    public FilterDialog(String subject, CustomNames customNames, PostExecuteInterface postExecuteInterface, Context context) {
        this(subject, null, customNames, postExecuteInterface, context);
    }

    public FilterDialog(String subject, String originalSubject, CustomNames customNames, final PostExecuteInterface postExecuteInterface, final Context context) {

        inputManager = ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);

        builder.setTitle("Fach umbenennen")
                .setView(dialogView);


        kursName = (EditText) dialogView.findViewById(R.id.kursName);
        final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.checkbox);
        final TextView editTextLabel = (TextView) dialogView.findViewById(R.id.edit_text_label);

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("whitelist_switch", false)) {
            checkBox.setVisibility(View.GONE);
        }

        if (customNames == null) {
            try {
                customNames = new CustomNames(context);
            } catch (Exception e) {
                System.err.println("Unexpected Error");
                App.reportUnexpectedException(e);
                e.printStackTrace();
                mBuilder = App.dialog("Fehler", "Es ist ein unerwarteter Fehler aufgetreten", context);
                return;
            }
        }

        final CustomNames customNamesFinal = customNames;

        if (originalSubject == null) {
            originalSubject = getOriginalSubject(subject, customNamesFinal);
        }

        if (subject.equals("Nicht anzeigen")) {
            checkBox.setChecked(true);
            kursName.setEnabled(false);

            subject = originalSubject;
        }

        final String finalSubject = subject;
        kursName.setText(finalSubject);
        kursName.setSelection(finalSubject.length());

        editTextLabel.setText(String.format(DISPLAY_NAME, originalSubject));

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean wasEnabled = kursName.isEnabled();
                if (!wasEnabled) {
                    inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
                kursName.setEnabled(!wasEnabled);
            }
        });

        final String originalSubject_final = originalSubject;

        builder.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String displayName = kursName.getText().toString();

                if (displayName.trim().equals("")) {
                    displayName = originalSubject_final;
                }

                if (checkBox.isChecked()) {
                    displayName = "Nicht anzeigen";
                }

                String subjectKey = finalSubject;

                for (Map.Entry<String, String> entry : customNamesFinal.entrySet()) {
                    if (entry.getValue().equals(subjectKey)) {
                        subjectKey = entry.getKey();
                    }
                }

                if (customNamesFinal.get(subjectKey) != null && customNamesFinal.get(subjectKey).equals(displayName)) {
                    return;
                }

                for (Map.Entry<String, String> entry : customNamesFinal.entrySet()) {
                    if (!displayName.equals("Nicht anzeigen") && displayName.equals(entry.getValue())) {
                        App.dialog("Eingabefehler", "\"" + displayName + "\" ist schon der Anzeigename für \"" + entry.getKey() + "\"", context)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new FilterDialog(finalSubject, customNamesFinal, postExecuteInterface, context).show();

                                    }
                                })
                                .show();
                        return;
                    }
                }

                if (displayName.equals(subjectKey)) {
                    customNamesFinal.remove(subjectKey);
                } else {
                    customNamesFinal.put(subjectKey, displayName);
                }

                customNamesFinal.save();

                postExecuteInterface.onPostExecute();


                inputManager.hideSoftInputFromWindow(kursName.getWindowToken(), 0);
                System.out.println("Gespeichert: " + displayName);
            }
        })
            .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    inputManager.hideSoftInputFromWindow(kursName.getWindowToken(), 0);
                    System.out.println("Nicht gespeichert");
                }
            });

        mBuilder = builder;
    }

    public static String getOriginalSubject(String subject, CustomNames customNames) {
        if (customNames == null) {
            return subject;
        }

        String originalSubject = null;
        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            if (entry.getValue().equals(subject)) {
                originalSubject = entry.getKey();
                System.out.println("originalSubject = " + originalSubject);
            }
        }
        if (originalSubject == null) {
            originalSubject = subject;
        }
        return originalSubject;
    }

    public void show() {
        mBuilder.show();
        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public interface PostExecuteInterface {
        void onPostExecute();
    }
}

