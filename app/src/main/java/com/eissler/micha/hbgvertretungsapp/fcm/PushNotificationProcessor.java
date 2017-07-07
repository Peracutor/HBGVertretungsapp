package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.AlarmManager;
import android.content.Context;

import com.eissler.micha.cloudmessaginglibrary.InfoNotification;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.TestPushRequestProcessor;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Micha.
 * 25.08.2016
 */
public class PushNotificationProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    @Override
    public String getAction() {
        return InfoNotification.ACTION;
    }

    @Override
    public void process(RemoteMessage remoteMessage) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(TestPushRequestProcessor.getFailedPendingIntent(getContext()));

        InfoNotification notification = new InfoNotification(remoteMessage.getData());
        App.showNotification(notification, getContext());
    }


}
