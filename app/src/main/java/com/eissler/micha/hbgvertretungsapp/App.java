package com.eissler.micha.hbgvertretungsapp;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.acra.ACRA;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class App {

    public final static String CUSTOM_NAMES = "CustomNames";

    private static final String CODE_SECTION_REACHED = "CODE_SECTION_REACHED";
    private static final String ERROR = "ERROR";
    private static final String INFO = "INFO";
    private static final String TRACE = "Trace";
    private static final String INTERNET = "INTERNET";
    private static final String USER_INPUT = "USER_INPUT";


    public static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);
//    public static final SimpleDateFormat TIME_SDF = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    public static final SimpleDateFormat NORMAL_SDF = new SimpleDateFormat("dd.MM.yy", Locale.GERMANY);
    public static final SimpleDateFormat PRECISE_SDF = new SimpleDateFormat("EE HH:mm:ss dd.MM.yy", Locale.GERMANY);
    public static final SimpleDateFormat DAY_NAME_SDF = new SimpleDateFormat("EE", Locale.GERMANY);

    public static final String ACTION_UPDATE = "ACTION_UPDATE";


    //----------read/write object to internal storage---------//
    public static void writeObject(Object object, String filename, Context c) throws IOException {
        App.logCodeSection("WriteObject");

        FileOutputStream fos = c.openFileOutput(filename, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(object);
        oos.close();
    }

    public static <T> T retrieveObject(String filename, Context c) throws ClassCastException, IOException, ClassNotFoundException {

        FileInputStream fin = c.openFileInput(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);
        Object object = ois.readObject();
        ois.close();

        if (object == null) {
            System.err.print("Object retrieved for following name was null: " + filename);
            return null;
//            throw new IOException("Retrieved Object was null.");
        }

        //noinspection unchecked
        return (T) object;
    }


    static public AlertDialog.Builder dialog(String title, CharSequence msg, Context activity){ return new AlertDialog.Builder(activity).setTitle(title).setMessage(msg).setPositiveButton("Ok", null); }

    public static void logCodeSection(String s) {
        Log.d(CODE_SECTION_REACHED, s);
    }


    public static void logError(String s) {
        Log.e(ERROR, s);
    }

    public static void logInfo(String s) {
        Log.i(INFO, s);
    }

    public static void logTrace(String s) {
        Log.d(TRACE, s);
    }

    public static void logInternet(String s) {
        Log.i(INTERNET, s);
    }

    public static void reportUnexpectedException(Exception e) {
        App.logError("EXCEPTION!");
        if (true) {  // TODO: 20.03.2016 remove!!!
            return;
        }

        ACRA.getErrorReporter().putCustomData(INFO, "APP DID NOT CRASH");
        ACRA.getErrorReporter().handleSilentException(e);
        ACRA.getErrorReporter().removeCustomData(INFO);
    }

    public static void logUserInput(String s) {
        Log.i(USER_INPUT, s);
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isMillisecondsLater(Date lastReload, long millisLater) {
        return new Date().getTime() - lastReload.getTime() > millisLater;
    }

    public static void exitWithError(Exception e) {
        e.printStackTrace();
        ACRA.getErrorReporter().handleException(e, true);
    }

    public static NotificationCompat.Builder getIntentNotificationBuilder(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return getNotificationBuilder(context)
                .setContentIntent(pendingIntent);
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.notification_icon)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setLights(Color.RED, 2000, 2000)
//                .setVibrate(new long[]{0,200,200,200})
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                ;
    }

    public interface WaitFor<T> {
        void onResult(T result);
    }
}
