package com.eissler.micha.hbgvertretungsapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.eissler.micha.hbgvertretungsapp.fcm.InstanceIdListenerService;

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
            return;
        }

        System.out.println("ConnectivityChangeReceiver.onReceive");
        if (App.isConnected(context)) {
            System.out.println("Connected");
            enable(false, context);

            InstanceIdListenerService.sendRegistrationToServer(context);
//            Intent forwardIntent = new Intent(context, NotificationService.class);
//            context.startService(forwardIntent);
        }
    }

    public static void enable(boolean enable, Context context) {
        System.out.println("ConnectivityChangeReceiver.enable");
        ComponentName receiver = new ComponentName(context, ConnectivityChangeReceiver.class);
        context.getPackageManager().setComponentEnabledSetting(receiver, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
