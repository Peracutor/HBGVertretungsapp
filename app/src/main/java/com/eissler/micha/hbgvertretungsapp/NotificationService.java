package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.evaluation.DownloadHandler;
import com.eissler.micha.hbgvertretungsapp.util.PreferenceExponentialBackoff;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.ReplacedCoverMessage;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import static com.eissler.micha.hbgvertretungsapp.util.Preferences.Key.NOTIFICATION_SERVICE_BACKOFF;

public class NotificationService extends IntentService {

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.", Locale.GERMANY);

    private CountDownLatch termination;

    public NotificationService(){
        super("NotificationService");
    }

    public static Calendar getDayToNotify() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);

        if (new Date().after(calendar.getTime())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        System.out.println("dayToNotify= " + calendar.get(Calendar.DAY_OF_WEEK));
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
                execute(getDayToNotify().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
                break;
            case Calendar.SATURDAY:
                terminate();
                break;
            case Calendar.SUNDAY:
                execute(getDayToNotify().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY);
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
            sortedCoverMessages = new HbgDataDownload(App.getSelectedClass(this), getDayToNotify().get(Calendar.WEEK_OF_YEAR), new DownloadHandler(this)).executeSync();
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
                        .setContentText(e.getMessage()));
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
        if (builder.mContentTitle == null) {
            String dateToNotify = sdf.format(getDayToNotify().getTime());
            builder.setContentTitle("Meldungen für den " + dateToNotify);
        }

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

        String dateToNotify = sdf.format(getDayToNotify().getTime());
        ArrayList<ReplacedCoverMessage> dayMessages = sortedCoverMessages.getMessagesForDay(dateToNotify, App.getCoverMessageFilter(this), App.getReplacer(this));
        if (dayMessages.size() == 0) {
            return null;
        }

        int thisDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int notifyDay = getDayToNotify().get(Calendar.DAY_OF_WEEK);
        String todayOrTomorrow = thisDay == notifyDay ? "heute" :
                thisDay == notifyDay - 1 ? "morgen" : "";

        String summary = dayMessages.size() + " Vertretungsmeldung" + (dayMessages.size() > 1 ? "en" : "") + " für " + todayOrTomorrow;

        NotificationCompat.InboxStyle inboxStyle = formatToInbox(dayMessages, "", summary);

        return App.getIntentNotificationBuilder(this)
                .setContentTitle("Meldungen für den " + dateToNotify)
                .setContentText(dayMessages.size() == 0 ? "Keine Vertretungsmeldungen" : dayMessages.get(0) + (dayMessages.size() > 1 ? " (+" + (dayMessages.size() - 1) + " weitere)" : ""))
                .setStyle(inboxStyle);
    }

    public static NotificationCompat.InboxStyle formatToInbox(ArrayList<? extends HBGMessage> hbgMessages, String linePrefix, String summary) {
        int size = hbgMessages.size();
        System.out.println("size = " + size);
//        System.out.println("MESSAGE_LIMIT = " + MESSAGE_LIMIT);
        if (size == 0) return null;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

//        int exceededBy = 0;
//        for (int i = 0; i < size; i++) {
//            HBGMessage hbgMessage = hbgMessages.get(i);
//            if (i + 1 < MESSAGE_LIMIT || i + 1 == MESSAGE_LIMIT && hbgMessage instanceof CoverMessage) {
//                String message = hbgMessage instanceof CoverMessage ? String.format("%s%s", linePrefix, hbgMessage.toString()) : hbgMessage.toString();
//                inboxStyle.addLine(message);
//            } else if (i + 1 > MESSAGE_LIMIT) {
//                exceededBy++;
//            }
//        }
//
//        inboxStyle.setSummaryText(size > MESSAGE_LIMIT ? summaryInterface.getSummaryLimitExceeded(messageCount, exceededBy) : summaryInterface.getSummary(messageCount));

        for (HBGMessage hbgMessage :
                hbgMessages) {
            String message;
            if (hbgMessage instanceof CoverMessage) {
                message = String.format("%s%s", linePrefix, hbgMessage.toString());
            } else {
                message = hbgMessage.toString();
            }
            inboxStyle.addLine(message);
        }

        inboxStyle.setSummaryText(summary);
        return inboxStyle;
    }

//    public interface SummaryInterface {
//        String getSummary(int messageCount);
//
//        String getSummaryLimitExceeded(int messageCount, int exceededBy);
//    }

    @NonNull
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

