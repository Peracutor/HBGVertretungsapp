package com.eissler.micha.cloudmessaginglibrary;

/**
 * Created by Micha.
 * 06.07.2017
 */
public class RegConfirmNotification extends AbstractPushNotification<RegConfirmNotification> {

    public static final String ACTION = "RegistrationConfirmation";

    @Override
    public String getAction() {
        return ACTION;
    }
}
