package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.support.v7.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;


public class OptionDialogBuilder extends AlertDialog.Builder {

    private AlertDialog optionDialog;

    protected OptionDialogBuilder(final Activity activity, final CharSequence[] subjects, final CharSequence[] originalSubjects) {
        super(activity);

        setTitle("Welches Fach umbenennen?");
        setSingleChoiceItems(subjects, -1, (dialog, which) -> {
                    new FilterDialog(originalSubjects[which].toString(), subjects[which].toString(),
                            activity, () -> EventBus.getDefault().post(new Event.ResetRequest())
                    ).show();
                    optionDialog.dismiss();
                });
        optionDialog = create();

    }

    public AlertDialog getOptionDialog() {
        return optionDialog;
    }
}
