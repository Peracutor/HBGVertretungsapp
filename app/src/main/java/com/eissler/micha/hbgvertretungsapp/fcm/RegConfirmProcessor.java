package com.eissler.micha.hbgvertretungsapp.fcm;

import com.eissler.micha.cloudmessaginglibrary.RegConfirmNotification;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Micha.
 * 23.07.2016
 */
public class RegConfirmProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    @Override
    public String getAction() {
        return RegConfirmNotification.ACTION;
    }

    @Override
    public void process(RemoteMessage remoteMessage) {
        System.out.println("Registration confirmed");
        Preferences sharedPreferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getContext());
        sharedPreferences.edit().putBoolean(Preferences.Key.TOKEN_SENT, true).apply();
        InstanceIdListenerService.getRegisterCheckBackoff(getContext()).reset();
    }
}
