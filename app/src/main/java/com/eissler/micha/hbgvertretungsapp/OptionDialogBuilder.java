package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;


public class OptionDialogBuilder extends AlertDialog.Builder {

    final AlertDialog optionDialog;

    protected OptionDialogBuilder(final Activity activity, final CharSequence[] subjects) {
        super(activity);

        setTitle("Welches Fach umbenennen?");
        setSingleChoiceItems(subjects, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new FilterDialog(subjects[which].toString(),
                                new FilterDialog.PostExecuteInterface() {
                                    @Override
                                    public void onPostExecute() {
                                        EventBus.getDefault().post(new Event.ResetRequest());
                                    }
                                }
                                , activity).show();
                        optionDialog.dismiss();
                    }
                });
        optionDialog = this.create();

    }

    public AlertDialog getOptionDialog() {
        return optionDialog;
    }
}
