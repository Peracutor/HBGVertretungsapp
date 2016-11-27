package com.eissler.micha.hbgvertretungsapp.fcm;

import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.eissler.micha.hbgvertretungsapp.ProcessorDistributor;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Micha.
 * 23.07.2016
 */
public class RegConfirmProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    @Override
    public String getAction() {
        return "RegistrationConfirmation";
    }

    @Override
    public void process(RemoteMessage remoteMessage) {
        System.out.println("Registration confirmed");
        Preferences sharedPreferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getContext());
        sharedPreferences.edit().putBoolean(Preferences.Key.TOKEN_SENT, true).apply();
        new InstanceIdListenerService.RegisterCheckBackoff(getContext()).reset();


    }
}
