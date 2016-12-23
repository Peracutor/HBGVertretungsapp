package com.eissler.micha.hbgvertretungsapp.util;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by Micha.
 * 28.05.2016
 */
public abstract class InputValidator implements TextWatcher {

    protected abstract boolean validate(String text);

    protected abstract void onValidated(boolean valid);

    @Override
    final public void afterTextChanged(Editable s) {
        boolean valid = validate(s.toString());
        onValidated(valid);
    }

    @Override
    final public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    final public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
