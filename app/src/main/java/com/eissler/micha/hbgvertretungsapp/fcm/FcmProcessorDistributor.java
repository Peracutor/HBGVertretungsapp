package com.eissler.micha.hbgvertretungsapp.fcm;

import android.util.Log;

import com.eissler.micha.hbgvertretungsapp.ProcessorDistributor;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by Micha.
 * 08.04.2016
 */
public class FcmProcessorDistributor extends FirebaseMessagingService implements ProcessorDistributor.ProcessorRegisterer<RemoteMessage> {


    @Override
    public ProcessorDistributor.ProcessorRegister<RemoteMessage> register() {
        return new ProcessorDistributor.ProcessorRegister<>(
                new RegConfirmProcessor(),
                new NotificationProcessor(),
                new PushProcessor(),
                new UpdateAvailableProcessor()
        );
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        System.out.println("MESSAGE RECEIVED");
        Log.d("FcmListenerService", "From: " + remoteMessage.getFrom());

        replaceUmlaute(remoteMessage);
        String action = remoteMessage.getData().get("action");
        new ProcessorDistributor<>(this).distribute(action, remoteMessage, this);
    }

    private static void replaceUmlaute(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            data.put(entry.getKey(), replaceUmlaute(entry.getValue()));
        }
    }

    private static String replaceUmlaute(String s) {
        return s.replace("ae", "ä").replace("ue", "ü").replace("oe", "ö")
                .replace("Ae", "Ä").replace("Ue", "Ü").replace("Oe", "Ö")
                .replace("s_z", "ß");
    }
}