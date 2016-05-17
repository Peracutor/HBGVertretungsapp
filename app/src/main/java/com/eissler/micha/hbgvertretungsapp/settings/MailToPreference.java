package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class MailToPreference extends Preference {

//    public MailToPreference(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }

    public MailToPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

//    public MailToPreference(Context context) {
//        super(context);
//    }

    @Override
    protected void onClick() {
        Intent i = new Intent(Intent.ACTION_SEND);
        //i.setType("text/plain"); //use this line for testing in the emulator
        i.setType("message/rfc822") ; // use from live device
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"m.eissler@hotmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT,"HBG-Vertretungsapp");
        //i.putExtra(Intent.EXTRA_TEXT,"(Falls du meinen Namen nicht kennst, ich bin Micha, 17 Jahre)");
        getContext().startActivity(Intent.createChooser(i, "Wähle eine Email-App aus"));
    }
}
