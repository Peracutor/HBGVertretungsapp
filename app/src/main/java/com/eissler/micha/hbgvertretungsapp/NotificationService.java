package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.evaluation.CustomNameReplacer;
import com.eissler.micha.hbgvertretungsapp.evaluation.HbgDownload;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.DownloadException;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.ResultCallback;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

public class NotificationService extends IntentService {

    private CountDownLatch termination;

    public NotificationService(){
        super("NotificationService");
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
                execute(HbgDataDownload.getDayToNotify().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
                break;
            case Calendar.SATURDAY:
                terminate();
                break;
            case Calendar.SUNDAY:
                execute(HbgDataDownload.getDayToNotify().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY);
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

        if (!App.isConnected(this)) {
            showNotification(makeBuilder(new DownloadException(DownloadException.ErrorType.NO_CONNECTION).getMessage()));
            return;
        }

        new HbgDownload(HbgDownload.getDayToNotify().get(Calendar.WEEK_OF_YEAR), this).executeAsync(new ResultCallback<SortedCoverMessages>() {
            @Override
            public void onResult(SortedCoverMessages sortedCoverMessages) {
                System.out.println("onDownloadCompleted");
                System.out.println("sortedCoverMessages = " + sortedCoverMessages);

                ExponentialBackoff backoff = new NotificationServiceBackoff(NotificationService.this);

                backoff.reset();
                if (sortedCoverMessages == null) {
//                    showNotification(getNoDataBuilder());
                    return;
                }

                NotificationCompat.Builder builder = formatMessagesForNotification(sortedCoverMessages, NotificationService.this);
                if (builder == null) {
                    System.out.println("No data");
//                    builder = getNoDataBuilder(); //no data for the day to notify about available
                    return;
                }

                showNotification(builder);
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                ExponentialBackoff backoff = new NotificationServiceBackoff(NotificationService.this);
                if (backoff.retry()) {
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + backoff.getValue(), Notifications.newInstance(NotificationService.this).getAlarmPendingIntent(47)); // TODO: 15.09.2016 check all request codes
                    terminate();
                } else {
                    backoff.reset();
                    showNotification(makeBuilder(e.getMessage()));
                }
            }
        });
    }

//    private NotificationCompat.Builder getNoDataBuilder() {
//        return makeBuilder("Keine Vertretungsdaten");
//    }

    private NotificationCompat.Builder makeBuilder(String contentText) {
        return App.getIntentNotificationBuilder(this)
                .setContentText(contentText);
    }

    private void showNotification(NotificationCompat.Builder builder) {
        System.out.println("showNotification");
        System.out.println("builder.mContentText = " + builder.mContentText);
        if (builder.mContentTitle == null) {
            String dateToNotify = App.SHORT_SDF.format(HbgDataDownload.getDayToNotify().getTime());
            builder.setContentTitle("Meldungen für den " + dateToNotify);
        }

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

    public NotificationCompat.Builder formatMessagesForNotification(SortedCoverMessages sortedCoverMessages, Context context) {

        String dateToNotify = App.SHORT_SDF.format(HbgDataDownload.getDayToNotify().getTime());
        ArrayList<CoverMessage> dayMessages = sortedCoverMessages.getMessagesForDay(dateToNotify, new CustomNameReplacer(context));
        if (dayMessages.size() == 0) {
            return null;
        }

        int thisDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int notifyDay = HbgDataDownload.getDayToNotify().get(Calendar.DAY_OF_WEEK);
        String todayOrTomorrow = thisDay == notifyDay ? "heute" :
                thisDay == notifyDay - 1 ? "morgen" : "";

        String summary = dayMessages.size() + " Vertretungsmeldung" + (dayMessages.size() > 1 ? "en" : "") + " für " + todayOrTomorrow;

        NotificationCompat.InboxStyle inboxStyle = formatToInbox(dayMessages, "", summary);

        return App.getIntentNotificationBuilder(context)
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

    private static class NotificationServiceBackoff extends ExponentialBackoff {

        public static final int START_VALUE = 15 * 1000;
        public static final int MAX_RETRIES = 3;

        public NotificationServiceBackoff(Context context) {
            super(context);
        }

        @Override
        protected Preferences.Key getPreferenceName() {
            return Preferences.Key.NOTIFICATION_SERVICE_BACKOFF;
        }

        @Override
        protected long getStartValue() {
            return START_VALUE;
        }

        @Override
        protected int getMaxRetries() {
            return MAX_RETRIES;
        }
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

