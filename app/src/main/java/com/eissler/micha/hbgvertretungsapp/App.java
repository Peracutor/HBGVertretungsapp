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

import com.eissler.micha.hbgvertretungsapp.settings.AutoName;
import com.eissler.micha.hbgvertretungsapp.settings.Blacklist;
import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.peracutor.hbgserverapi.BlacklistFilter;
import com.peracutor.hbgserverapi.CustomNameReplacer;
import com.peracutor.hbgserverapi.Filter;
import com.peracutor.hbgserverapi.Replacer;
import com.peracutor.hbgserverapi.WhitelistFilter;

import org.acra.ACRA;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

public class App {

    private static final String CODE_SECTION_REACHED = "CODE_SECTION_REACHED";
    private static final String ERROR = "ERROR";
    private static final String INFO = "INFO";
    private static final String TRACE = "Trace";
    private static final String INTERNET = "INTERNET";
    private static final String USER_INPUT = "USER_INPUT";

    private static final char[] UMLAUTS = "äöüÄÖÜß".toCharArray();


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

    public static void exitWithError(Throwable e) {
        e.printStackTrace();
        ACRA.getErrorReporter().handleException(e, true);
    }

    public static NotificationCompat.Builder getIntentNotificationBuilder(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, RequestCodes.ACTIVITY_OPEN_NOTIFICATION, intent, 0);

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

    public static int getSelectedClass(Context context) {
        return Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, context).getInt(Preferences.Key.SELECTED_CLASS, 0);
    }

    public static Filter getCoverMessageFilter(Context context) {
        if (Whitelist.isWhitelistModeActive(context)) {
            return new WhitelistFilter(Whitelist.get(context));
        } else {
            return new BlacklistFilter(Blacklist.get(context));
        }
    }

    public static Replacer getReplacer(Context context) {
        return AutoName.isAutoNamingEnabled(context) ?
                new CustomNameReplacer(CustomNames.get(context), new AutoName(context)) :
                new CustomNameReplacer(CustomNames.get(context));
    }

    public static String decodeUmlauts(String s) { //encoding/decoding needed because it was f***ing impossible to change default-encoding to UTF-8 on AppEngine: http://stackoverflow.com/questions/41204936/google-appengine-how-to-set-default-charset-file-encoding-to-utf-8-for-goo
        for (char umlaut : UMLAUTS) {
            s = s.replace("%" + (int) umlaut + "%", umlaut + "");
        }
        return s;
    }

    public static String encodeUmlauts(String s) {
        for (char umlaut : UMLAUTS) {
            s = s.replace(umlaut + "", "%" + (int) umlaut + "%");
        }
        return s;
    }

    public interface WaitFor<T> {
        void onResult(T result);
    }
}
