package com.eissler.micha.hbgvertretungsapp.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

/**
 * Created by Micha.
 * 13.12.2016
 */

public class CustomListPreference extends ListPreference {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomListPreference(Context context) {
        super(context);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {

        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
    }
}
