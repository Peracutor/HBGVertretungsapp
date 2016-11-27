package com.eissler.micha.hbgvertretungsapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.fcm.AppEngine;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.peracutor.hbgbackend.messaging.Messaging;
import com.peracutor.hbgserverapi.DownloadException;

import org.acra.util.Installation;

/**
 * Created by Micha.
 * 22.11.2016
 */
public class TestPushRequestProcessor extends ProcessorDistributor.Processor<Intent> {

        //    private static CountDownLatch termination;
        private static Messaging sMessaging;

        @Override
        public String getAction() {
            return "alarm.request_test_push";
        }

        @Override
        public void process(Intent intent) {
            String token = intent.getStringExtra("token");
            if (sMessaging == null) {
                sMessaging = AppEngine.getApiInstance(new Messaging.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null));
            }


            try {
                System.out.println("token = " + token);
                sMessaging.messagingEndpoint().sendTestPush(token, Installation.id(getContext())).execute();

                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 30 * 1000, getFailedPendingIntent(getContext()));
            } catch (Exception e) {
                e.printStackTrace();
                e = DownloadException.getCorrespondingExceptionFor(e);
                NotificationCompat.Builder builder = App.getIntentNotificationBuilder(getContext())
                        .setContentTitle("Fehler")
                        .setContentText("Testanfrage konnte nicht gesendet werden")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("Testanfrage konnte nicht an den Server gesendet werden:\n" + e.getMessage() + ",\n" + e.getCause().getMessage()));

                NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.notify(7, builder.build());
            }
        }

    public static PendingIntent getFailedPendingIntent(Context context) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("alarm.test_push_failed");
            return PendingIntent.getBroadcast(context, 3, intent, 0); // TODO: 20.10.2016  pendingIntent requestCodes

        }

    public static class TestPushFailedProcessor extends ProcessorDistributor.Processor<Intent> {
        @Override
        public String getAction() {
            return "alarm.test_push_failed";
        }

        @Override
        public void process(Intent object) {
            String text = "Es wurde keine Push-Benachrichtigung empfangen"; // TODO: 22.11.2016 maybe don't show at all
            NotificationCompat.Builder builder = App.getIntentNotificationBuilder(getContext())
                    .setContentTitle("Test fehlgeschlagen")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text + "\n\n(Timeout: 30s)"));

            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(7, builder.build());
        }
    }
}
