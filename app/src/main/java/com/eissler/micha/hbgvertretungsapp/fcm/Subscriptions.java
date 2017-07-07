package com.eissler.micha.hbgvertretungsapp.fcm;

import android.content.Context;

import com.eissler.micha.cloudmessaginglibrary.AsciiEncoder;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by Micha.
 * 23.05.2016
 */
public class Subscriptions {

    private final FirebaseMessaging firebase = FirebaseMessaging.getInstance();
    private final Preferences defaultPrefs;
    private final boolean whitelistModeActive;

    private SubscribedTopics subscribedTopics;
    private int classNum;
    private Context context;


    public static Subscriptions newInstance(Context context) {
        return new Subscriptions(context);
    }

    public static Subscriptions newInstance(Context context, boolean whitelistMode) {
        return new Subscriptions(context, whitelistMode);
    }

    private Subscriptions(Context context) {
        this(context, null);
    }

    private Subscriptions(Context context, Boolean whitelistMode) {
        this.context = context;
        defaultPrefs = Preferences.getDefaultPreferences(context);
        subscribedTopics = new SubscribedTopics();
        classNum = App.getSelectedClass(context);
        whitelistModeActive = whitelistMode != null ? whitelistMode : Whitelist.isWhitelistModeActive(context);
    }

    public void subscribe() {
        if (whitelistModeActive) {
            subscribedTopics.add(String.format("%s-whitelist", classNum));

            //subscribe to all whitelist-subjects
            Whitelist whitelist;
            whitelist = Whitelist.get(context);

            for (int i = 0; i < whitelist.size(); i++) {
                String subject = whitelist.get(i);
                subscribedTopics.add(classNum, subject);
            }

//            subscribedTopics.save();
        } else {
            subscribedTopics.add(String.format("%s-no_whitelist", classNum));
        }
        if (defaultPrefs.getBoolean(Preferences.Key.WEEK_CHANGE_NOTIFICATION, true)) {
            subscribedTopics.add(String.format("%s-week_change", classNum));
        }
        subscribeToClassNumber();
//        subscribedTopics.save();
    }

    public void unsubscribe() {
        subscribedTopics.clear();
        subscribeToClassNumber();
//        subscribedTopics.save();
    }


    private void subscribeToClassNumber() {
        if (!subscribedTopics.contains(String.valueOf(classNum))) {
            subscribedTopics.add(classNum);
        }
        subscribedTopics.save();
    }

    public void changeWeekChangeSubscription(boolean subscribe) {
        String weekChangeTopic = String.format("%s-week_change", classNum);
        if (subscribe) {
            subscribedTopics.add(weekChangeTopic);
        } else {
            subscribedTopics.remove(weekChangeTopic);
        }
    }

    public void printSubs() {
        System.out.println("Topics:");
        for (String topic : subscribedTopics) {
            System.out.println("    /topics/" + topic);
        }
    }

    public static boolean isEnabled(Context context) {
        return Preferences.getDefaultPreferences(context).getBoolean(Preferences.Key.PUSH_NOTIFICATION_SWITCH, true);
    }

    public boolean add(String subject) {
        return subscribedTopics.add(classNum, subject);
    }

    public boolean remove(String subject) {
        return subscribedTopics.remove(classNum, subject);
    }

    public boolean saveEdits() {
        return subscribedTopics.save();
    }

    public void resetSubscriptions() {
        unsubscribe();
        if (Subscriptions.isEnabled(context)) {
            subscribe();
        }
    }


    private class SubscribedTopics extends HashSet<String> {

        public SubscribedTopics() {
            super();
            loadSavedTopics();
        }

        private SubscribedTopics loadSavedTopics() {
            addAll(defaultPrefs.getStringSet(Preferences.Key.SUBSCRIBED_TOPICS, null), false);
            return this;
        }

        public boolean save() {
            printSubs();
            return defaultPrefs.edit().putStringSet(Preferences.Key.SUBSCRIBED_TOPICS, this).commit();
        }

        public boolean addAll(Collection<? extends String> collection, boolean subscribeToFirebase) {
            if (collection == null) {
                return false;
            }

            if (subscribeToFirebase) {
                for (String topic : this) {
                    firebase.subscribeToTopic(topic);
                }
            }
            return super.addAll(collection);
        }

        public boolean add(int classNumber, String subject) {
            String topic = AsciiEncoder.encode(subject).toLowerCase(Locale.GERMANY);
            return add(String.format("%s-%s", classNumber, topic));
        }

        public boolean add(int classNumber) {
            return add(String.valueOf(classNumber));
        }


        public boolean remove(int classNumber, String subject) {
            String topic = AsciiEncoder.encode(subject).toLowerCase(Locale.GERMANY);
            return remove(String.format("%s-%s", classNumber, topic));
        }

        @Override
        public boolean add(String topic) {
            boolean notAlreadyContained = super.add(topic);
            if (notAlreadyContained && topic.matches("[a-zA-Z0-9-_.~%]+")) firebase.subscribeToTopic(topic);
            return notAlreadyContained;
        }

        @Override
        public boolean remove(Object topic) {
            boolean wasContained = super.remove(topic);
            if (wasContained) firebase.unsubscribeFromTopic((String) topic);
            return wasContained;
        }

        @Override
        public void clear() {
            for (String topic : this) {
                firebase.unsubscribeFromTopic(topic);
            }
            super.clear();
        }
    }
}

