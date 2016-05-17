package com.eissler.micha.hbgvertretungsapp;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;


public class OptionDialogBuilder extends AlertDialog.Builder {

    final AlertDialog optionDialog;

    protected OptionDialogBuilder(final MainActivity mainActivity, final CharSequence[] subjects) {
        super(mainActivity);

        setTitle("Welches Fach umbenennen?");
        setSingleChoiceItems(subjects, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new FilterDialog(subjects[which].toString(),
                                new FilterDialog.PostExecuteInterface() {
                                    @Override
                                    public void onPostExecute() {
                                        mainActivity.resetPager();
                                    }
                                }
                                , mainActivity).show();
                        optionDialog.dismiss();
                    }
                });
        optionDialog = this.create();

    }

    public AlertDialog getOptionDialog() {
        return optionDialog;
    }
}
