package com.eissler.micha.hbgvertretungsapp.fcm;

import android.util.Log;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
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
                new PushNotificationProcessor(),
                new PushProcessor(),
                new UpdateAvailableProcessor()
        );
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        System.out.println("MESSAGE RECEIVED");
        Log.d("FcmListenerService", "From: " + remoteMessage.getFrom());

        replaceUmlauts(remoteMessage);
        String action = remoteMessage.getData().get("action");
        new ProcessorDistributor<>(this).distribute(action, remoteMessage, this);
    }

    private static void replaceUmlauts(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            data.put(entry.getKey(), App.decodeUmlauts(entry.getValue()));
        }
    }
}