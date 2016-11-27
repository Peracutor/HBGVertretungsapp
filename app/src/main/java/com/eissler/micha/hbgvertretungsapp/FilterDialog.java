package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;

import java.util.Map;

public class FilterDialog {

    private static final String DISPLAY_NAME = "Anzeigename für \"%s\":";

    private final AlertDialog mDialog;
    private final EditText kursName;
    private final InputMethodManager inputManager;
    private String errorText;

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

        if (Whitelist.isWhitelistModeActive(context)) {
            checkBox.setVisibility(View.GONE);
        }

        if (customNames == null) {
            customNames = CustomNames.get(context);
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
                boolean checked = checkBox.isChecked();
                if (!checked) {
                    inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    kursName.setError(errorText);
                } else {
                    kursName.setError(null);
                }
                kursName.setEnabled(!checked);

                if (mDialog != null) {
                    mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(checked || errorText == null);
                }
            }
        });

        final String originalSubject_final = originalSubject;
        final String oldName = customNamesFinal.get(originalSubject_final) == null ? originalSubject : customNamesFinal.get(originalSubject_final);

        builder.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String displayName = kursName.getText().toString();

                if (checkBox.isChecked()) {
                    displayName = "Nicht anzeigen";
                }

                if (displayName.equals(oldName)) {
                    return;
                }

                if (displayName.equals(originalSubject_final)) {
                    customNamesFinal.remove(originalSubject_final);
                } else {
                    customNamesFinal.put(originalSubject_final, displayName);
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

        mDialog = builder.show();


        kursName.addTextChangedListener(new InputValidator() {
            @Override
            protected boolean validate(String text) {
                if (text.equals(oldName)) {
                    return true;
                }

                if (text.trim().length() == 0) {
                    kursName.setError(errorText = "Der Anzeigename darf nicht leer sein");
                    return false;
                }

                if (!text.equals("Nicht anzeigen")) {
                    for (Map.Entry<String, String> entry : customNamesFinal.entrySet()) {
                        if (text.equals(entry.getValue())) {
                            kursName.setError(errorText = "\"" + text + "\" ist schon der Anzeigename für \"" + entry.getKey() + "\"");
                            return false;
                        }
                    }
                }

                errorText = null;
                return true;
            }

            @Override
            protected void onValidated(boolean valid) {
                mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(valid);
            }
        });
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
        show(true);
    }

    public void show(boolean toggleKeyboard) {
        mDialog.show();
        if (toggleKeyboard) inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public interface PostExecuteInterface {
        void onPostExecute();
    }
}

