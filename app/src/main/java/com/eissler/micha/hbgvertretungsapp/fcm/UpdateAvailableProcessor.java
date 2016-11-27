package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.MainActivity;
import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.eissler.micha.hbgvertretungsapp.ProcessorDistributor;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by Micha.
 * 08.10.2016
 */
public class UpdateAvailableProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    @Override
    public String getAction() {
        return "UpdateAvailableNotification";
    }

    @Override
    public void process(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        final int versionNumber = Integer.parseInt(data.get("versionNumber"));
        final String versionName = data.get("versionName");
        final String apkUrl = data.get("apkUrl");
        final int thisVersion;
//        final String thisVersionName;
        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            thisVersion = packageInfo.versionCode;
//            thisVersionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            App.exitWithError(e);
            return;
        }
        if (versionNumber > thisVersion) {
            Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getContext()).edit().putLong(Preferences.Key.LAST_UPDATE_CHECK, 0).apply();
            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            String text = String.format("Es ist ein Update zu Version %s verfügbar!", versionName);
            int id = 29;

            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(App.ACTION_UPDATE);
            intent.putExtra("versionNumber", versionNumber);
            intent.putExtra("id", id);
            intent.putExtra("apkUrl", apkUrl);
            PendingIntent actionIntent = PendingIntent.getActivity(getContext(), 0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            notificationManager.notify(id, App.getIntentNotificationBuilder(getContext())
                    .setContentTitle("Update verfügbar!")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .addAction(android.R.drawable.stat_sys_download, "Herunterladen", actionIntent)
                    .build());
        }
    }
}
