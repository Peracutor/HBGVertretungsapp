package com.eissler.micha.hbgvertretungsapp;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.evaluation.DataEvaluation;
import com.eissler.micha.hbgvertretungsapp.evaluation.SortedCoverMessages;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class CheckForNewDataService extends IntentService {
    private CountDownLatch termination;

    public CheckForNewDataService(){
        super("CheckForNewDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("CheckForNewDataService running...");

//        if (!connected()) {
//            ComponentName receiver = new ComponentName(this, ConnectivityChangeReceiver.class);
//            getPackageManager().setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
//            return;
//        }

        String dayInWeek = new SimpleDateFormat("EE", Locale.GERMANY).format(new Date());
        System.out.println("dayInWeeko = " + dayInWeek);

        DataEvaluation.DataEvaluationInterface dataEvaluationInterface = new DataEvaluation.DataEvaluationInterface() {
            @Override
            public void onDataEvaluationComplete(DataEvaluation.DataEvaluationException e, SortedCoverMessages sortedCoverMessages) {
                System.out.println("onDataEvaluationComplete");
                if (e != null) {
                    e.printStackTrace();
                    if (e.getErrorCode() == DataEvaluation.DataEvaluationException.NO_DATA) {
                        System.out.println("e.getMessage() = " + e.getMessage());
                        showNotification(getNoDataBuilder());
                    }
                    else if (e.getErrorCode() == DataEvaluation.DataEvaluationException.NO_CONNECTION) {
                        showNotification(makeBuilder("Es besteht keine Internetverbindung"));
                    }
                    else if (e.getErrorCode() == DataEvaluation.DataEvaluationException.BAD_CONNECTION) {
                        showNotification(makeBuilder("Internetverbindung konnte nicht hergestellt werden"));
                    }
                    else if (e.getErrorCode() == DataEvaluation.DataEvaluationException.ERROR) {
                        showNotification(makeBuilder("Beim Herunterladen ist ein Fehler aufgetreten"));
                    }
                    return;
                }

                NotificationCompat.Builder builder = sortedCoverMessages.formatMessagesForNotification();
                if (builder == null) {
                    System.out.println("No data");
                    builder = getNoDataBuilder(); //no data for the day to notify about available
                }

                showNotification(builder);
            }
        };

        termination = new CountDownLatch(1);

        switch (dayInWeek) {
            case "Fr.":
                if (DataEvaluation.getDayToNotify().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                    new DataEvaluation(this, dataEvaluationInterface);
                } else {
                    terminate();
                }
                break;
            case "Sa.":
                terminate();
                break;
            case "So.":
                if (DataEvaluation.getDayToNotify().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    terminate();
                } else {
                    new DataEvaluation(this, dataEvaluationInterface);
                }
                break;
            default:
                new DataEvaluation(this, dataEvaluationInterface);
                break;
        }

        try {
            termination.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private NotificationCompat.Builder getNoDataBuilder() {
        return makeBuilder("Keine Vertretungsdaten");
    }

    private NotificationCompat.Builder makeBuilder(String contentText) {
        return new NotificationCompat.Builder(this)
                .setContentText(contentText);
    }

    private void showNotification(@NonNull NotificationCompat.Builder builder) {
        System.out.println("showNotification");
        System.out.println("builder.mContentText = " + builder.mContentText);
        String dateToNotify = App.SHORT_SDF.format(DataEvaluation.getDayToNotify().getTime());

        builder.setContentTitle("Meldungen für den " + dateToNotify)
            .setSmallIcon(R.mipmap.notification_icon)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setLights(Color.RED, 3000, 3000);

        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(1, builder.build());
        System.out.println("SERVICE FINISHED");
        terminate();
    }

    private void terminate() {
        System.out.println("Terminate");
        termination.countDown();
        stopSelf();
    }
}
