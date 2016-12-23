package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.settings.AutoName;
import com.eissler.micha.hbgvertretungsapp.settings.Blacklist;
import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;

import java.util.Map;

public class FilterDialog {

    private static final String DISPLAY_NAME = "Anzeigename für \"%s\":";

    private final AlertDialog mDialog;
    private final EditText kursName;
    private final InputMethodManager inputManager;
    private String errorText;

    public FilterDialog(String originalSubject, @Nullable String customName, Context context, Runnable postExecute) {
        this(originalSubject, customName, null, context, postExecute);
    }

    public FilterDialog(String originalSubject, @Nullable String customName, @Nullable CustomNames customNames, final Context context, final Runnable postExecute) {

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

        if (customName == null) {
            customName = customNames.get(originalSubject);
            if (customName == null) {
                customName = originalSubject;
            }
        }


        final Blacklist blacklist = Blacklist.get(context);
        if (blacklist.contains(originalSubject)) {
            checkBox.setChecked(true);
            kursName.setEnabled(false);

//            customName = originalSubject;
        }

        kursName.setText(customName);
        kursName.setSelection(customName.length());

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
        final String oldName;
        if (customNamesFinal.get(originalSubject_final) == null) {
            oldName = AutoName.isAutoNamingEnabled(context) ? new AutoName(context).getAutoName(originalSubject) : originalSubject;
        } else {
            oldName = customNamesFinal.get(originalSubject_final);
        }

        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String displayName = kursName.getText().toString();

            if (checkBox.isChecked()) {
                blacklist.add(originalSubject_final);
                blacklist.save();
            } else {
                if (displayName.equals(oldName)) {
                    return;
                }

                if (displayName.equals(originalSubject_final)) {
                    customNamesFinal.remove(originalSubject_final);
                } else {
                    customNamesFinal.put(originalSubject_final, displayName);
                }
                customNamesFinal.save();
            }

            postExecute.run();

            inputManager.hideSoftInputFromWindow(kursName.getWindowToken(), 0);
            System.out.println("Gespeichert: " + displayName);
        })
        .setNegativeButton("Abbrechen", (dialog, which) -> {
            inputManager.hideSoftInputFromWindow(kursName.getWindowToken(), 0);
            System.out.println("Nicht gespeichert");
        });

        mDialog = builder.show();


        kursName.addTextChangedListener(new InputValidator() {
            @Override
            protected boolean validate(String input) {
                if (input.equals(oldName)) {
                    return true;
                }

                if (input.trim().length() == 0) {
                    kursName.setError(errorText = "Der Anzeigename darf nicht leer sein");
                    return false;
                }

                for (Map.Entry<String, String> entry : customNamesFinal.entrySet()) {
                    if (input.equals(entry.getValue())) {
                        kursName.setError(errorText = "\"" + input + "\" ist schon der Anzeigename für \"" + entry.getKey() + "\"");
                        return false;
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

    public void show() {
        show(true);
    }

    public void show(boolean toggleKeyboard) {
        mDialog.show();
        if (toggleKeyboard) inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}

