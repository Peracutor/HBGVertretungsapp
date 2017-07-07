package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eissler.micha.hbgvertretungsapp.settings.AutoName;
import com.eissler.micha.hbgvertretungsapp.settings.Blacklist;
import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;

public class FilterDialog {

    private static final String HINT_PATTERN = "Anzeigename für \"%s\":";

    private MaterialDialog mDialog;
    private final InputMethodManager inputManager;
    private String errorText;

    public FilterDialog(String originalSubject, @Nullable String customName, Context context, Runnable postExecute) {
        this(originalSubject, customName, null, context, postExecute);
    }

    public FilterDialog(String originalSubject, @Nullable String customName, @Nullable CustomNames customNames, final Context context, final Runnable postExecute) {

        inputManager = ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE));

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title("Fach umbenennen")
                .customView(dialogView, false);

        EditText kursName = (EditText) dialogView.findViewById(R.id.kursName);
        final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.checkbox);

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
        }

        kursName.setText(customName);
        kursName.setSelection(customName.length());

        ((TextInputLayout) dialogView.findViewById(R.id.input_layout)).setHint(String.format(HINT_PATTERN, originalSubject));

        checkBox.setOnClickListener(v -> {
            boolean checked = checkBox.isChecked();
            if (!checked) {
                inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                kursName.setError(errorText);
            } else {
                kursName.setError(null);
            }
            kursName.setEnabled(!checked);

            if (mDialog != null) {
                mDialog.getActionButton(DialogAction.POSITIVE).setEnabled(checked || errorText == null);
            }
        });

        final String oldName;
        if (customNamesFinal.get(originalSubject) == null) {
            oldName = AutoName.isAutoNamingEnabled(context) ? new AutoName(context).getAutoName(originalSubject) : originalSubject;
        } else {
            oldName = customNamesFinal.get(originalSubject);
        }

        builder.positiveText("Speichern")
                .onPositive((dialog, which) -> {
                    String displayName = kursName.getText().toString();

                    System.out.println("originalSubject = " + originalSubject);
                    if (checkBox.isChecked()) {
                        blacklist.add(originalSubject);
                    } else {
                        blacklist.remove(originalSubject);
                        if (!displayName.equals(oldName)) {
                            if (displayName.equals(originalSubject)) {
                                customNamesFinal.remove(originalSubject);
                            } else {
                                customNamesFinal.put(originalSubject, displayName);
                            }
                            customNamesFinal.save();
                        }
                    }
                    blacklist.save();

                    postExecute.run();

                    inputManager.hideSoftInputFromWindow(kursName.getWindowToken(), 0);
                    System.out.println("Gespeichert: " + displayName);
                })
                .negativeText("Abbrechen")
                .onNegative((dialog, which) -> {
                    inputManager.hideSoftInputFromWindow(kursName.getWindowToken(), 0);
                    System.out.println("Nicht gespeichert");
                });

        mDialog = builder.show();


        kursName.addTextChangedListener(new InputValidator.DisablerEditTextValidator(kursName, mDialog.getActionButton(DialogAction.POSITIVE)) {
            @Override
            public CharSequence validate(CharSequence input) {
                if (input.equals(oldName)) {
                    return null;
                }

                if (input.toString().trim().length() == 0) {
                    return "Der Anzeigename darf nicht leer sein";
                }
//
//                for (Map.Entry<String, String> entry : customNamesFinal.entrySet()) {
//                    if (input.equals(entry.getValue())) {
//                        return "\"" + input + "\" ist schon der Anzeigename für \"" + entry.getKey() + "\""; //doesnt work
//                    }
//                }
                return null;
            }

            @Override
            protected void onError(CharSequence errorText) {
                super.onError(errorText);
                FilterDialog.this.errorText = errorText == null ? null : errorText.toString();
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

