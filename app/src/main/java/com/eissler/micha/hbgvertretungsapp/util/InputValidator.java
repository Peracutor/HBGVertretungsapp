package com.eissler.micha.hbgvertretungsapp.util;

import android.support.annotation.CallSuper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Micha.
 * 28.05.2016
 */
public abstract class InputValidator implements TextWatcher, ValidatableChild {

    private ValidatorGroup parentGroup;
    private CharSequence errorMessage;

    protected abstract CharSequence validate(CharSequence text);

    protected abstract void onError(CharSequence errorText);

    @Override
    final public void afterTextChanged(Editable s) {
        errorMessage = validate(s);
        onError(errorMessage);
        if (parentGroup != null) {
            parentGroup.update();
        }
    }

    @Override
    public void setParent(ValidatorGroup validatorGroup) {
        parentGroup = validatorGroup;
    }

    public boolean isValid() {
        return errorMessage == null;
    }

    @Override
    final public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    final public void onTextChanged(CharSequence s, int start, int before, int count) {}

    public static abstract class EditTextValidator extends InputValidator {

        private TextView editText;

        public EditTextValidator(TextView editText) {
            this(editText, "");
        }

        public EditTextValidator(TextView editText, CharSequence initialText) {
            super.errorMessage = validate(initialText);
            this.editText = editText;
        }


//        public TextView getEditText() {
//            return editText;
//        }

        @Override
        @CallSuper
        protected void onError(CharSequence errorText) {
            editText.setError(errorText);
        }

    }

    public static abstract class DisablerEditTextValidator extends EditTextValidator {

        private final View disablerView;

        public DisablerEditTextValidator(TextView editText, View disablerView) {
            super(editText);
            this.disablerView = disablerView;
        }

        @Override
        @CallSuper
        protected void onError(CharSequence errorText) {
            super.onError(errorText);
            disablerView.setEnabled(isValid());
        }
    }


    public static abstract class ValidatorGroup implements ValidatableChild {

        private ArrayList<ValidatableChild> validators = new ArrayList<>();
        private ValidatorGroup parent;
        private boolean groupValid;

        @Override
        public void setParent(ValidatorGroup validatorGroup) {
            parent = validatorGroup;
        }

        @Override
        public boolean isValid() {
            return groupValid;
        }

        public void add(ValidatableChild validator) {
            validator.setParent(this);
            validators.add(validator);
        }

        public void remove(ValidatableChild validator) {
            validator.setParent(null);
            validators.remove(validator);
        }

        protected abstract void onGroupValidated(boolean groupValid);

        private void update() {
            groupValid = isGroupValid();
            onGroupValidated(groupValid);
            if (parent != null) parent.update();
        }

        private boolean isGroupValid() {
            for (ValidatableChild validator : validators) {
                if (!validator.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class DisablerValidatorGroup extends ValidatorGroup {
        private final View disableView;

        public DisablerValidatorGroup(View disableView) {
            this.disableView = disableView;
        }

        @Override
        @CallSuper
        protected void onGroupValidated(boolean groupValid) {
            disableView.setEnabled(groupValid);
        }
    }


    public static class NotEmptyValidator extends EditTextValidator {

        public NotEmptyValidator(TextView editText) {
            super(editText);
        }

        @Override
        public CharSequence validate(CharSequence text) {
            return text.toString().trim().equals("") ? getErrorText() : null;
        }

        protected CharSequence getErrorText() {
            return  "Darf nicht leer sein";
        }
    }


}
