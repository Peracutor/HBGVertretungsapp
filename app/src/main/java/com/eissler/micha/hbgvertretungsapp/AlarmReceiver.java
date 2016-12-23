package com.eissler.micha.hbgvertretungsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eissler.micha.hbgvertretungsapp.fcm.InstanceIdListenerService;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;

public class AlarmReceiver extends BroadcastReceiver implements ProcessorDistributor.ProcessorRegisterer<Intent> {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Received the Intent!");
        new ProcessorDistributor<>(this).distribute(intent.getAction(), intent, context);
    }

    @Override
    public ProcessorDistributor.ProcessorRegister<Intent> register() {
        return new ProcessorDistributor.ProcessorRegister<>(
                new NotificationService.StartServiceProcessor(),
                new TestPushRequestProcessor(),
                new TestPushRequestProcessor.TestPushFailedProcessor(),
                new InstanceIdListenerService.RegisterCheckProcessor()
        );
    }
}