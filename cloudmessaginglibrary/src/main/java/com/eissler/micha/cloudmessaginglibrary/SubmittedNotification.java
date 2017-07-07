package com.eissler.micha.cloudmessaginglibrary;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Micha.
 * 29.05.2017
 */


public class SubmittedNotification extends InfoNotification {

    private Recipients recipients;
    private String senderToken;
    private TimeToLive timeToLive;


    @SuppressWarnings("unused") //For Objectify
    private SubmittedNotification() {
        super(new HashMap<String, String>());
    }

    public SubmittedNotification(Map<String, String> data, Recipients recipients, String senderToken) {
        super(data);
        this.recipients = recipients;
        this.senderToken = senderToken;
    }

//    public Map<String, String> getData() {
//        return data;
//    }

    public Recipients getRecipients() {
        return recipients;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setRecipients(Recipients recipients) {
        this.recipients = recipients;
    }

    public String getSenderToken() {
        return senderToken;
    }

    public TimeToLive getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(TimeToLive timeToLive) {
        this.timeToLive = timeToLive;
    }

//    public static class ClassRecipients extends com.eissler.micha.backend_library.Recipients {
//
//        public ClassRecipients classNumbers(List<Integer> classNumbers) {
//            for (Integer classNumber : classNumbers) {
//                topic(String.valueOf(classNumber));
//            }
//            return this;
//        }
//    }


    public static class Builder extends InfoNotification.Builder {

        Recipients recipients;
        private String senderToken;

        public Builder setSenderToken(String senderToken) {
//            data.put("senderToken", senderToken);
            this.senderToken = senderToken;
            return this;
        }

        public void setRecipients(Recipients recipients) {
            this.recipients = recipients;
        }

        public SubmittedNotification build() {
            return new SubmittedNotification(data, recipients, senderToken);
        }
    }

}
