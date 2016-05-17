package com.eissler.micha.hbgvertretungsapp;


import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

public class LongDownload {

    private final long downloadReference;
    private DownloadManager downloadManager;
    //    private final BroadcastReceiver notificationClickedReceiver;

    public LongDownload(String urlString, String filename, final Activity activity, final LongDownloadInterface longDownloadInterface) {
        downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlString));

        request.setTitle("HBG-App-Update")
                .setDescription("Lade Update herunter...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        downloadReference = downloadManager.enqueue(request);

//        notificationClickedReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                long[] references = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
//
//                for (long reference : references) {
//                    if (reference == downloadReference) {
//                        Intent activityIntent = new Intent(context, activity.getClass());
//                        activityIntent.setAction(DOWNLOAD_NOTIFICATION);
//                        context.startActivity(activityIntent);
//                    }
//                }
//            }
//        };

        //activity.registerReceiver(notificationClickedReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("Broadcast received");

                activity.unregisterReceiver(this);
                //activity.unregisterReceiver(notificationClickedReceiver);

                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                if (reference == downloadReference) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);
                    if (cursor.getCount() < 1) {
                        longDownloadInterface.onDownloadCancelled();
                        return;
                    }

                    cursor.moveToFirst();

                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    String filepath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            System.out.println("HEY IT WORKS SO FAR");
                            break;
                        default:
                            System.out.println("status is: " + status);
                            System.out.println("reason = " + reason);
                    }

                    longDownloadInterface.onDownloadCompleted(filepath);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        activity.registerReceiver(downloadCompleteReceiver, intentFilter);

        new Thread(new Runnable() {

            @Override
            public void run() {

                boolean downloading = true;

                int latestPercentDone;
                int percentDone = 0;

                while (downloading) {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadReference);

                    Cursor cursor = downloadManager.query(q);
                    if (cursor.getCount() < 1) {
                        break;
                    }

                    cursor.moveToFirst();
                    int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }

                    latestPercentDone = (int) ((bytes_downloaded * 100l) / bytes_total);
                    if (percentDone != latestPercentDone) {
                        percentDone = latestPercentDone;
                        longDownloadInterface.showProgress(latestPercentDone);
                    }
                    cursor.close();
                }

            }
        }).start();
    }

    public void cancel() {
        downloadManager.remove(downloadReference);
    }

    public interface LongDownloadInterface {
        void showProgress(int progress);

        void onDownloadCompleted(String filepath);

        void onDownloadCancelled();
    }
}
