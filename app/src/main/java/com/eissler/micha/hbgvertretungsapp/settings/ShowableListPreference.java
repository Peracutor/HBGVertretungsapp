package com.eissler.micha.hbgvertretungsapp.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by Micha.
 * 15.12.2016
 */
@SuppressWarnings("unused")
public class ShowableListPreference extends ListPreference {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ShowableListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ShowableListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ShowableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShowableListPreference(Context context) {
        super(context);
    }

    public void show() {
        showDialog(null);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle("Wie sollen Kürzel ausgeschrieben werden? (Bsp.: LEkN2)");
    }
}
