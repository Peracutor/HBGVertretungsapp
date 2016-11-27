package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.MainActivity;
import com.eissler.micha.hbgvertretungsapp.ProcessorDistributor;
import com.eissler.micha.hbgvertretungsapp.TestPushRequestProcessor;
import com.google.firebase.messaging.RemoteMessage;
import com.koushikdutta.ion.Ion;

import java.util.Map;

/**
 * Created by Micha.
 * 25.08.2016
 */
public class NotificationProcessor extends ProcessorDistributor.Processor<RemoteMessage> {

    @Override
    public String getAction() {
        return "InfoNotification";
    }

    //dxO56uR8LnI:APA91bFoNTqszNmSuWe_DL62tcnEz-b4Z7bcVKx9MdK6BmlOnj5XKpGaR0P4S3HBjQonlnrKcRTD5rXXqQUav9kYLT6p3jpSSVcBmYuvxh0YxS5fZ9sARJY1_UWRjUnlJL5o7zZb0nI_
    @Override
    public void process(RemoteMessage remoteMessage) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(TestPushRequestProcessor.getFailedPendingIntent(getContext()));

        System.out.println("NotificationProcessor.process");
        Map<String, String> data = remoteMessage.getData();

        final String title = data.get("title");
        final String body = data.get("body");
        String imageUrl = data.get("imageUrl");

        System.out.println("title = " + title);
        System.out.println("body = " + body);


        Intent backIntent = new Intent(getContext(), MainActivity.class);
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = new Intent(getContext(), NotificationViewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("title", title)
                .putExtra("body", body)
                .putExtra("imageUrl", imageUrl);
        PendingIntent pendingIntent = PendingIntent.getActivities(getContext(), 0 /* Request code */, new Intent[]{backIntent, intent}, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder notificationBuilder = App.getNotificationBuilder(getContext())
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent);

        final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (imageUrl != null) try {
            Bitmap bitmap = Ion.with(getContext())
                    .load(imageUrl)
                    .asBitmap()
                    .get();

            System.out.println("Image downloaded");

            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(body));
            notificationManager.notify(5 /* ID of notification */, notificationBuilder.build());
            //                            termination.countDown();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        notificationManager.notify(5 /* ID of notification */, notificationBuilder.build());
    }


}
