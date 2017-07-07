package com.eissler.micha.hbgvertretungsapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.fcm.AppEngine;
import com.eissler.micha.hbgvertretungsapp.util.DownloadException;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.peracutor.hbgbackend.messaging.Messaging;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Micha.
 * 22.11.2016
 */
public class TestPushRequestProcessor extends ProcessorDistributor.Processor<Intent> {

        private CountDownLatch termination = new CountDownLatch(1);

        @Override
        public String getAction() {
            return "alarm.request_test_push";
        }

        @Override
        public void process(Intent intent) {
            String token = intent.getStringExtra("token");

            new AppEngine.Task<Messaging>(new Messaging.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null), messaging -> {
                try {
                    messaging.messagingEndpoint().sendTestPush(token).execute();

                    AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 30 * 1000, getFailedPendingIntent(getContext()));
                } catch (Exception e) {
                    e.printStackTrace();
                    e = DownloadException.getCorrespondingExceptionFor(e);
                    NotificationCompat.Builder builder = App.getIntentNotificationBuilder(getContext())
                            .setContentTitle("Fehler")
                            .setContentText("Testanfrage konnte nicht gesendet werden")
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("Testanfrage konnte nicht an den Server gesendet werden:\n" + e.getMessage() + (e.getCause() != null ? "\n(" + e.getCause().getMessage() + ")" : "")));

                    NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

                    notificationManager.notify(RequestCodes.NOTIFICATION_TEST_PUSH_FAIL, builder.build());
                }
                termination.countDown();
            }).execute();

            try {
                termination.await();// may seem stupid to use AsyncTask with CountDownLatch, but otherwise causes NetworkOnMainThreadException
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    public static PendingIntent getFailedPendingIntent(Context context) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("alarm.test_push_failed");
            return PendingIntent.getBroadcast(context, RequestCodes.ALARM_TEST_PUSH_FAILED, intent, 0);

        }

    public static class TestPushFailedProcessor extends ProcessorDistributor.Processor<Intent> {
        @Override
        public String getAction() {
            return "alarm.test_push_failed";
        }

        @Override
        public void process(Intent object) {
            String text = "Es wurde keine Testbenachrichtigung empfangen";
            NotificationCompat.Builder builder = App.getIntentNotificationBuilder(getContext())
                    .setContentTitle("Test fehlgeschlagen")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text + "\n\n(Timeout: 30s)"));

            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(RequestCodes.NOTIFICATION_TEST_PUSH_FAIL, builder.build());
        }
    }
}
