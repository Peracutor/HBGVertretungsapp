package com.eissler.micha.hbgvertretungsapp.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;

/**
 * Created by Micha.
 * 20.06.2017
 */

public class EditTextBetterError extends TextInputEditText {
    public EditTextBetterError(Context context) {
        super(context);
    }

    public EditTextBetterError(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextBetterError(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setError(CharSequence error, Drawable icon) {
        if (error == null) {
            super.setError(null, icon);
            setCompoundDrawables(null, null, null, null);
        }
        else if (error.toString().equals("")) setCompoundDrawables(null, null, icon, null);
        else super.setError(error, icon);
    }
}
