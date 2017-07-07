package com.eissler.micha.cloudmessaginglibrary;

import com.google.android.gcm.server.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Micha.
 * 04.07.2017
 */

public abstract class AbstractPushNotification<T extends AbstractPushNotification> {

    protected Map<String, String> data;

    protected AbstractPushNotification() {
        this.data = new HashMap<>();
    }

    protected AbstractPushNotification(Map<String, String> data) {
        this.data = data;
    }

    public abstract String getAction();

    @SuppressWarnings("unchecked")
    protected List<Result> send(T notfication, Recipients recipients, TimeToLive timeToLive, String apiKey) throws IOException {
        Message.Builder builder = new Message.Builder();
        notfication.data.put("action", getAction());
        builder.setData(notfication.data);
        if (timeToLive != null && timeToLive.getTimeToLive() != null) {
            int ttl = timeToLive.getTimeToLive();
            if (ttl < 0) {
                throw new IllegalArgumentException("timeToLive is in the past, not sending message");
            }
            builder.timeToLive(ttl);
        }

        Sender sender = new Sender(apiKey);
        Message message = builder.build();

        List<Result> results = new ArrayList<>(recipients.getRecipients().size());

        for (String recipient : recipients.getRecipients()) {
            Result result = sender.sendMessage(message, recipient, 3);
            results.add(result);
        }
        return results;
    }

    public List<Result> send(Recipients recipients, String apiKey) throws IOException {
        //noinspection unchecked
        return send(recipients, (TimeToLive) null, apiKey);
    }

    public List<Result> send(Recipients recipients, Integer timeToLive, String apiKey) throws IOException {
        //noinspection unchecked
        return send(recipients, new TimeToLive(timeToLive), apiKey);
    }

    public List<Result> send(Recipients recipients, TimeToLive timeToLive, String apiKey) throws IOException {
        //noinspection unchecked
        return send((T) this, recipients, timeToLive, apiKey);
    }

    public static abstract class Builder<T extends AbstractPushNotification> {
        protected Map<String, String> data = new HashMap<>();

        public abstract T build();
    }


//    public static void main(String[] args) {
//        Recipients recipients = new Recipients().condition(Recipients.Condition
//                .topicOne("TopicA")
//                .and()
//                .condition(Recipients.Condition
//                        .topicOne("TopicB")
//                        .or()
//                        .topic("TopicC")));
//
//        System.out.println("condition = " + recipients.recipients.get(0));
//    }

}
