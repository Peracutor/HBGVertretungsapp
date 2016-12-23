package com.eissler.micha.hbgvertretungsapp.fcm;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Micha.
 * 23.05.2016
 */
public class PushNotifications {

    private final FirebaseMessaging firebase = FirebaseMessaging.getInstance();
    private final Preferences defaultPrefs;

    private SubscribedTopics subscribedTopics;
    private int classNum;
    private Context context;


    public static PushNotifications newInstance(Context context) {
        return new PushNotifications(context);
    }

    private PushNotifications(Context context) {
        this.context = context;
        defaultPrefs = Preferences.getDefaultPreferences(context);
        subscribedTopics = new SubscribedTopics();
        classNum = App.getSelectedClass(context);
    }

    public void activate() {
        activate(Whitelist.isWhitelistModeActive(context));
    }

    public void activate(boolean whitelistModeActive) {
        if (whitelistModeActive) {
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
        subscribeToClassNumber();
//        subscribedTopics.save();
    }

    public void deactivate() {
        unsubscribeAll();
    }

    private void unsubscribeAll() {
        subscribedTopics.clear();
        subscribeToClassNumber();
//        subscribedTopics.save();
    }


    public void subscribeToClassNumber() {
        if (!subscribedTopics.contains(String.valueOf(classNum))) {
            subscribedTopics.add(classNum);
        }
        subscribedTopics.save();
    }

    public void printSubs() {
        System.out.println("Topics:");
        for (String topic : subscribedTopics) {
            System.out.println("    /topics/" + topic);
        }
    }

    public static boolean isEnabled(Context context) {
        return Preferences.getDefaultPreferences(context).getBoolean(Preferences.Key.PUSH_NOTIFICATION_SWITCH, false);
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
            return add(String.format("%s-%s", classNumber, App.encodeUmlauts(subject)));
        }

        public boolean add(int classNumber) {
            return add(String.valueOf(classNumber));
        }


        public boolean remove(int classNumber, String subject) {
            return remove(String.format("%s-%s", classNumber, App.encodeUmlauts(subject)));
        }

        @Override
        public boolean add(String topic) {
            firebase.subscribeToTopic(topic);
            return super.add(topic);
        }

        @Override
        public boolean remove(Object topic) {
            firebase.unsubscribeFromTopic((String) topic);
            return super.remove(topic);
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

