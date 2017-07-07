package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.evaluation.DownloadHandler;
import com.eissler.micha.hbgvertretungsapp.util.PreferenceExponentialBackoff;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import static com.eissler.micha.hbgvertretungsapp.util.Preferences.Key.NOTIFICATION_SERVICE_BACKOFF;

public class NotificationService extends IntentService {

    private CountDownLatch termination;

    public NotificationService(){
        super("NotificationService");
    }

    private static int getDayToNotify() {
        Calendar calendar = getDateToNotify();
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    private static Calendar getDateToNotify() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);

        if (new Date().after(calendar.getTime())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar;
    }

    @SuppressLint("SwitchIntDef")
    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("NotificationService running...");
        int dayInWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        System.out.println("dayInWeek = " + dayInWeek);

        termination = new CountDownLatch(1);

        switch (dayInWeek) {
            case Calendar.FRIDAY:
                execute(getDayToNotify() == Calendar.FRIDAY);
                break;
            case Calendar.SATURDAY:
                terminate();
                break;
            case Calendar.SUNDAY:
                execute(getDayToNotify() != Calendar.SUNDAY);
                break;
            default:
                execute(true);
                break;
        }

        try {
            System.out.println("NotificationService.onHandleIntent finished, waiting for termination");
            termination.await(); //so service keeps running while evaluating even if onHandleIntent is finished
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void execute(boolean execute) {
        if (!execute) {
            terminate();
            return;
        }
        SortedCoverMessages sortedCoverMessages;
        try {
            sortedCoverMessages = new HbgDataDownload(App.getSelectedClass(this), getDateToNotify().get(Calendar.WEEK_OF_YEAR), new DownloadHandler(this)).executeSync();
        } catch (Exception e) {
            e.printStackTrace();
            PreferenceExponentialBackoff backoff = getBackoff();
            if (backoff.retry()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + backoff.getValue(), Notifications.newInstance(NotificationService.this).getAlarmPendingIntent(RequestCodes.ALARM_NO_INTERNET_BACKOFF));
                terminate();
            } else {
                backoff.reset();
                showNotification(App.getIntentNotificationBuilder(this)
                        .setContentTitle("Verbindung fehlgeschlagen")
                        .setContentText(e.getMessage())
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(e.getMessage()))
                );
            }
            return;
        }

        PreferenceExponentialBackoff backoff = getBackoff();
        backoff.reset();
        if (sortedCoverMessages == null) {
//                    showNotification(getNoDataBuilder());
            return;
        }

        NotificationCompat.Builder builder = formatMessagesForNotification(sortedCoverMessages);
        if (builder == null) {
            System.out.println("No data");
//                    builder = getNoDataBuilder(); //no data for the day to notify about available
            return;
        }

        showNotification(builder);
    }



//    private NotificationCompat.Builder getNoDataBuilder() {
//        return App.getIntentNotificationBuilder(this)
//                  .setContentText("Keine Vertretungsdaten");
//    }

    private void showNotification(NotificationCompat.Builder builder) {
        System.out.println("showNotification");
        System.out.println("builder.mContentText = " + builder.mContentText);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(RequestCodes.NOTIFICATION_PULL, builder.build());
        System.out.println("SERVICE FINISHED");
        terminate();
    }

    private void terminate() {
        System.out.println("Terminate");
        termination.countDown();
        stopSelf();
    }

    public NotificationCompat.Builder formatMessagesForNotification(SortedCoverMessages sortedCoverMessages) {
        sortedCoverMessages.filter(App.getCoverMessageFilter(this));
        sortedCoverMessages.replace(App.getReplacer(this));

        int notifyDay = getDayToNotify();
        ArrayList<CoverMessage> dayMessages = sortedCoverMessages.getMessagesForWeekDay(notifyDay);
        if (dayMessages.size() == 0) {
            return null;
        }

        int thisDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        String todayOrTomorrow = thisDay == notifyDay ? "heute" :
                thisDay == notifyDay - 1 ? "morgen" : "";

        String summary = dayMessages.size() + " Vertretungsmeldung" + (dayMessages.size() > 1 ? "en" : "") + " für " + todayOrTomorrow;

        NotificationCompat.InboxStyle inboxStyle = formatToInbox(dayMessages, summary, false);

        return App.getIntentNotificationBuilder(this)
                .setContentTitle("Meldungen für den " + dayMessages.get(0).get(CoverMessage.DATE))
                .setContentText(dayMessages.get(0) + (dayMessages.size() > 1 ? " (+" + (dayMessages.size() - 1) + " weitere)" : ""))
                .setStyle(inboxStyle);
    }

    public static NotificationCompat.InboxStyle formatToInbox(ArrayList<? extends HBGMessage> hbgMessages, String summary, boolean withBullet) {
        int size = hbgMessages.size();
        if (size == 0) return null;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        final String bullet = " \u2022 ";
        for (HBGMessage hbgMessage :
                hbgMessages) {
            String message;
            if (withBullet && hbgMessage instanceof CoverMessage) {
                message =  String.format("%s%s", bullet, hbgMessage.toString());
            } else {
                message = hbgMessage.toString();
            }
            inboxStyle.addLine(message);
        }

        inboxStyle.setSummaryText(summary);
        return inboxStyle;
    }

    private PreferenceExponentialBackoff getBackoff() {
        return new PreferenceExponentialBackoff(15 * 1000, 2, 3, NOTIFICATION_SERVICE_BACKOFF.getKey(), Preferences.getDefaultPreferences(NotificationService.this));
    }


    public static class StartServiceProcessor extends ProcessorDistributor.Processor<Intent> {
        @Override
        public String getAction() {
            return "alarm.notification";
        }

        @Override
        public void process(Intent object) {
            Intent forwardIntent = new Intent(getContext(), NotificationService.class);
            getContext().startService(forwardIntent);
        }
    }
}

