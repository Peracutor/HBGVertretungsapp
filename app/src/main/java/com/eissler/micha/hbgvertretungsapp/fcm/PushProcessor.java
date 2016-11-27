package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.NotificationService;
import com.eissler.micha.hbgvertretungsapp.ProcessorDistributor;
import com.eissler.micha.hbgvertretungsapp.evaluation.CustomNameReplacer;
import com.google.firebase.messaging.RemoteMessage;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by Micha.
 * 23.07.2016
 */
public class PushProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    private static final long QUEUE_TIMEOUT_SECONDS = 8;

    private static SortedCoverMessages messageQueue;
    private static boolean countDownRunning = false;
    private static int counter = 0;


    @Override
    public String getAction() {
        return "PushNotification";
    }

    @Override
    public void process(RemoteMessage remoteMessage) { // TODO: 22.11.2016 test PushNotifications again
        Map<String, String> data = remoteMessage.getData();

        if (messageQueue == null) {
            messageQueue = new SortedCoverMessages(4);
        }


        CoverMessage.Builder messageBuilder = new CoverMessage.Builder(Integer.parseInt(data.get("Jahr")));
        for (int i = 0; i < 8; i++) {
            messageBuilder.setField(i, data.get(String.valueOf(i)));
        }
        CoverMessage coverMessage = new CoverMessage(messageBuilder);
        System.out.println("coverMessage = " + coverMessage);

        synchronized (this) {
            messageQueue.insert(coverMessage);
        }
        System.out.println("Message " + messageQueue.size() +" received");

        System.out.println("countDownRunning = " + countDownRunning);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!countDownRunning) {
                    countDownRunning = true;

                    System.out.println("Start countdown");
                    System.out.print("Waiting ");
                    for (; counter < QUEUE_TIMEOUT_SECONDS; counter++) {
                        System.out.print(counter + ", ");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.print("\n");
                    fireNotifications();
                    countDownRunning = false;
                    counter = 0;

                }
            }
        }).start();
        if (countDownRunning) {
            System.out.println("Reset countdown");
            counter = 0;
        }
    }

    private synchronized void fireNotifications() {
        System.out.println("FIRE NOTIFICATIONS");
        CustomNameReplacer replacer = new CustomNameReplacer(getContext());
        ArrayList<HBGMessage> listItems = messageQueue.getListItems(replacer);

        NotificationCompat.InboxStyle inboxStyle = NotificationService.formatToInbox(listItems, "   ", messageQueue.size() + " neue Meldung" + (messageQueue.size() > 1 ? "en" : ""));

        NotificationCompat.Builder notificationBuilder = null;
        try {
            notificationBuilder = App.getIntentNotificationBuilder(getContext())
                    .setContentTitle("Push-Benachrichtigung")
                    .setContentText(App.DAY_NAME_SDF.format(App.NORMAL_SDF.parse(messageQueue.getDays().get(0) + Calendar.getInstance().get(Calendar.YEAR))) + ": " + replacer.replace(messageQueue.get(0)).toString() + (messageQueue.size() > 1 ? " (+" + (messageQueue.size() - 1) + " weitere)" : "") )
                    .setStyle(inboxStyle);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        assert notificationBuilder != null;
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

        messageQueue.clear();
    }

}
