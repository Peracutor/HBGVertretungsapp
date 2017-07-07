package com.eissler.micha.hbgvertretungsapp.fcm;

import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
                new PushProcessor.WeekChangeProcessor(),
                new UpdateAvailableProcessor()
        );
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        System.out.println("MESSAGE RECEIVED");
        String action = remoteMessage.getData().get("action");
        new ProcessorDistributor<>(this).distribute(action, remoteMessage, this);
    }}