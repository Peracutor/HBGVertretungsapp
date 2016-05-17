package com.eissler.micha.hbgvertretungsapp.settings;


import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

//import org.apache.commons.validator.routines.EmailValidator;

public class EmailEditTextPreference extends EditTextPreference {

    public EmailEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                if (preference == EmailEditTextPreference.this) {
//                    EmailValidator emailValidator = EmailValidator.getInstance();
//                    if (emailValidator.isValid((String) newValue)) {
//                        System.out.println("Email is valid");
//                        Toast.makeText(getContext(), "Danke, dass du deine E-Mail-Adresse bereitstellst!", Toast.LENGTH_SHORT).show();
//                        return true;
//                    } else {
//                        System.out.println("Email is invalid"); // TODO: 08.04.2016 remove or uncomment
//                        getEditText().setError("Ungültige E-Mail-Adresse");
//                        showDialog(null);
//                    }
//                }
//                return false;
                return true;
            }
        });
    }
}
