package com.eissler.micha.cloudmessaginglibrary;

import java.util.Map;

/**
 * Created by Micha.
 * 06.07.2017
 */
public class PushNotification extends AbstractPushNotification<PushNotification> {

    public static final String ACTION = "PushNotification";
    public static final String PARAM_MESSAGE_JSON = "message_json";

    public PushNotification(Map<String, String> data) {
        super(data);
    }

    public PushNotification(String messageJson) {
        super();
        data.put(PARAM_MESSAGE_JSON, messageJson);
    }

    public String getMessageJson() {
        return data.get(PARAM_MESSAGE_JSON);
    }

    @Override
    public String getAction() {
        return ACTION;
    }
}
