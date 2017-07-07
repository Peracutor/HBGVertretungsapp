package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.NotificationManager;
import android.content.Context;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.cloudmessaginglibrary.PushNotification;
import com.eissler.micha.cloudmessaginglibrary.WeekChangeNotification;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.NotificationService;
import com.eissler.micha.hbgvertretungsapp.RequestCodes;
import com.eissler.micha.hbgvertretungsapp.evaluation.DownloadHandler;
import com.eissler.micha.hbgvertretungsapp.util.DownloadException;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.firebase.messaging.RemoteMessage;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.eissler.micha.hbgvertretungsapp.util.DownloadException.ErrorType.ERROR;

/**
 * Created by Micha.
 * 23.07.2016
 */
public class PushProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    private static final long QUEUE_TIMEOUT = 8;

    private static PowerManager.WakeLock wakeLock;

    private static SortedCoverMessages messageQueue;
    private static boolean countDownRunning = false;
    private static int counter = 1;


    @Override

    public String getAction() {
        return PushNotification.ACTION;
    }

    @Override
    public void process(RemoteMessage remoteMessage) {
        wakeLock = ((PowerManager) getContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        wakeLock.acquire(60 * 1000);

        PushNotification pushNotification = new PushNotification(remoteMessage.getData());

        if (messageQueue == null) {
            messageQueue = new SortedCoverMessages();
        }
//        if (latch == null) {
//            latch = new CountDownLatch(1);
//        }

//        CoverMessage.Builder messageBuilder = new CoverMessage.Builder(Integer.parseInt(data.get("Jahr")));
//        for (int i = 0; i < 8; i++) {
//            messageBuilder.setField(i, data.get(String.valueOf(i)));
//        }
        CoverMessage coverMessage;
        try {
            coverMessage = CoverMessage.deserializer().fromJsonString(pushNotification.getMessageJson());
        } catch (IOException e) {
            e.printStackTrace();
            App.report(e);
            wakeLock.release();
            return;
        }
        System.out.println("Message " + messageQueue.size() + " received");
        System.out.println("coverMessage = " + coverMessage);

        synchronized (this) {
            messageQueue.insert(coverMessage);
            System.out.println("countDownRunning = " + countDownRunning);
            if (!countDownRunning) {
                countDownRunning = true;
                new Thread(() -> {
                    System.out.println("Start countdown");
                    for (; counter <= QUEUE_TIMEOUT; counter++) {
                        System.out.print(counter + ", ");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    fireNotification(messageQueue, getContext(), false);
                    countDownRunning = false;
                    counter = 1;

                }).start();
            } else {
                System.out.println("Reset countdown");
                counter = 1;
            }
        }
//        try {
//            latch.await(); TODO if process() returns much earlier than fireNotification() is called, context dies (mobile goes to sleep)
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.out.println("Processing finished");
    }

    private static synchronized void fireNotification(SortedCoverMessages sortedCoverMessages, Context context, boolean weekChangeTitle) {
        System.out.println("FIRE NOTIFICATIONS");

        sortedCoverMessages.filter(App.getCoverMessageFilter(context));
        sortedCoverMessages.replace(App.getReplacer(context));

        ArrayList<HBGMessage> listItems = sortedCoverMessages.getListItems();
        if (listItems.size() == 0) {
            System.out.println("no messages to be shown.");
            wakeLock.release();
            return;
        }

        NotificationCompat.InboxStyle inboxStyle = NotificationService.formatToInbox(listItems, sortedCoverMessages.size() + " neue Meldung" + (sortedCoverMessages.size() > 1 ? "en" : ""), true);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EE", Locale.GERMANY);
            SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yy", Locale.GERMANY);
            NotificationCompat.Builder notificationBuilder = App.getIntentNotificationBuilder(context)
                    .setContentTitle(weekChangeTitle ? "Meldungen für nächste Woche" : "Echtzeit-Benachrichtigung")
                    .setContentText(sdf.format(sdf2.parse(sortedCoverMessages.get(0).get(CoverMessage.DATE) + Calendar.getInstance().get(Calendar.YEAR))) + ": " +
                            sortedCoverMessages.get(0).toString() + (sortedCoverMessages.size() > 1 ? " (+" + (sortedCoverMessages.size() - 1) + " weitere)" : ""))
                    .setStyle(inboxStyle);


            int id = App.getNextPushId(RequestCodes.NOTIFICATION_PUSH, Preferences.Key.LAST_PUSH_ID, context);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, notificationBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
            App.report(e);
        }
        sortedCoverMessages.clear();
        wakeLock.release();
    }

    public static class WeekChangeProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

        private boolean isSecondTry = false;

        @Override
        public String getAction() {
            return WeekChangeNotification.ACTION;
        }

        @Override
        public void process(RemoteMessage remoteMessage) {
            System.out.println("WeekChangeProcessor.process");
            wakeLock = ((PowerManager) getContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
            wakeLock.acquire(60 * 1000);
            SortedCoverMessages sortedCoverMessages;
            try {
                Calendar calendar = Calendar.getInstance();
                final int dayInWeek = calendar.get(Calendar.DAY_OF_WEEK);
                boolean isWeekEnd = dayInWeek == Calendar.SATURDAY || dayInWeek == Calendar.SUNDAY;
                int weekNumber = calendar.get(Calendar.WEEK_OF_YEAR);
                if (isWeekEnd || dayInWeek == Calendar.FRIDAY && calendar.get(Calendar.HOUR_OF_DAY) >= 19) {
                    weekNumber++;
                }
                sortedCoverMessages = new HbgDataDownload(App.getSelectedClass(getContext()), weekNumber, new DownloadHandler(getContext())).executeSync();
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof ExecutionException && e.getCause() instanceof TimeoutException) {
                    wakeLock.release();
                    if (isSecondTry) {
                        int id = App.getNextPushId(RequestCodes.NOTIFICATION_PUSH, Preferences.Key.LAST_PUSH_ID, getContext());
                        String text = "Meldungen für die nächste Woche konnten nicht geladen werden";
                        NotificationCompat.Builder notificationBuilder = App.getIntentNotificationBuilder(getContext())
                                .setContentTitle("Meldungen für nächste Woche")
                                .setContentText(text)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
                        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(id, notificationBuilder.build());
                    } else {
                        isSecondTry = true;
                        process(null);
                    }
                    return;
                } else if (!(e instanceof DownloadException) || e.getMessage().equals(DownloadException.getMessageForErrorType(ERROR))) {
                    App.report(e);
                }
                wakeLock.release();
                return;
            }

            if (sortedCoverMessages == null) {
                wakeLock.release();
                return;
            }

            fireNotification(sortedCoverMessages, getContext(), true);
        }
    }
}
