package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class MailToPreference extends Preference {

    public MailToPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        Intent i = new Intent(Intent.ACTION_SEND);
        //i.setType("text/plain"); //use this line for testing in the emulator
        i.setType("message/rfc822") ; // use for live device
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"m.eissler@hotmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT,"HBG-Vertretungsapp");
        getContext().startActivity(Intent.createChooser(i, "Email-App auswählen:"));
    }
}
