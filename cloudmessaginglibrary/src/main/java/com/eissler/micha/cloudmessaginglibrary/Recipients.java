package com.eissler.micha.cloudmessaginglibrary;

import java.util.ArrayList;

/**
 * Created by Micha.
 * 04.07.2017
 */
public class Recipients {

    private ArrayList<String> recipients = new ArrayList<>();

    public Recipients to(String to) {
        recipients.add(to);
        return this;
    }

    public Recipients id(String registrationId) {
        return to(registrationId);
    }


//    public Recipients ids(ArrayList<String> registrationIds) {
//        recipients.addAll(registrationIds);
//        return this;
//    }

    public Recipients topic(String topic) {
        recipients.add("/topics/" + topic);
        return this;
    }

//    public Recipients topics(ArrayList<String> topics) {
//        for (String topic : topics) {
//            topic(topic);
//        }
//        return this;
//    }

    public Recipients condition(Condition condition) {
        recipients.add(condition.getCondition());
        return this;
    }

    public Recipients id(ArrayList<Condition> conditions) {
        for (Condition condition : conditions) {
            condition(condition);
        }
        return this;
    }

    public ArrayList<String> getRecipients() {
        return recipients;
    }

    public static class Condition {

        StringBuilder to;

        public Condition() {
            to = new StringBuilder();
        }

        public Condition topic(String topic) {
            to.append('\'').append(topic).append("' in topics");
            return this;
        }

        public Condition or() {
            to.append(" || ");
            return this;
        }

        public Condition and() {
            to.append(" && ");
            return this;
        }

        public Condition condition(Condition condition) {
            to.append('(').append(condition.getCondition()).append(')');
            return this;
        }

        public String getCondition() {
            return to.toString();
        }

    }
}
