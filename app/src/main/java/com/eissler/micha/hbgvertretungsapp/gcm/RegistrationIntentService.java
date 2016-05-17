package com.eissler.micha.hbgvertretungsapp.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.acra.util.Installation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Micha.
 * 07.04.2016
 */

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};
    private static final Random RANDOM = new Random();

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken("49393142546",
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            boolean registered = sharedPreferences.getBoolean(Preferences.SENT_TOKEN_TO_SERVER, false);
            if (!registered) {
                sendRegistrationToServer();
                subscribeTopics(token, TOPICS);
            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                ArrayList<String> topics = extras.getStringArrayList("topics");
                if (topics != null && topics.size() != 0) {
                    subscribeTopics(token, topics.toArray(new String[0]));
                }
            }

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(Preferences.SENT_TOKEN_TO_SERVER, true).apply(); // TODO: 29.04.2016 set to true after getting response from server
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(Preferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Preferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to backend-server.
     *
     */
    private void sendRegistrationToServer() throws IOException {

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        Bundle data = new Bundle();
        // the account is used for keeping
        // track of user notifications
//        data.putString("account", account);

        // the action is used to distinguish
        // different message types on the server
        data.putString("action", getPackageName() + ".REGISTER");
        data.putString("acraID", Installation.id(getApplicationContext()));
        String msgId = getRandomMessageId();
        System.out.println("msgId = " + msgId);

        gcm.send("49393142546@gcm.googleapis.com", msgId, data);
        System.out.println("REGISTRATION REQUEST SENT to senderid");

    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @param topics the topics to subscribe to
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token, String[] topics) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : topics) {
            System.out.println("topic = " + "/topics/" + topic);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]


    public String getRandomMessageId() {
        return "m-" + UUID.randomUUID();
    }

}