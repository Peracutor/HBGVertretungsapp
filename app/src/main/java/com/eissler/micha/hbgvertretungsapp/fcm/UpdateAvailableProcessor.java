package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.cloudmessaginglibrary.UpdateAvailableNotification;
import com.eissler.micha.cloudmessaginglibrary.VersionInfo;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.MainActivity;
import com.eissler.micha.hbgvertretungsapp.RequestCodes;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Micha.
 * 08.10.2016
 */
public class UpdateAvailableProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    @Override
    public String getAction() {
        return UpdateAvailableNotification.ACTION;
    }

    @Override
    public void process(RemoteMessage remoteMessage) {
        VersionInfo versionInfo = new UpdateAvailableNotification(remoteMessage.getData()).getVersionInfo();

        final int thisVersion;
        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            thisVersion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            App.report(e);
            return;
        }
        if (versionInfo.getVersionNumber() > thisVersion) {
            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            String text = String.format("Es ist ein Update zu Version %s verfügbar!", versionInfo.getVersionName());

            int id = RequestCodes.NOTIFICATION_UPDATE_AVAILABLE;
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(App.ACTION_UPDATE);
            intent.putExtra("type", "notificationClick");
            PendingIntent pendingContentIntent = PendingIntent.getActivity(getContext(), RequestCodes.ACTION_OPEN_NOTIFICATION, intent, 0);

            intent.putExtra("type", "actionButton");
            intent.putExtra("id", id);

            PendingIntent pendingActionIntent = PendingIntent.getActivity(getContext(), RequestCodes.ACTION_UPDATE_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            notificationManager.notify(id, App.getNotificationBuilder(getContext())
                    .setContentTitle("Update verfügbar!")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pendingContentIntent)
                    .addAction(android.R.drawable.stat_sys_download, "Herunterladen", pendingActionIntent)
                    .build());
        }
    }
}