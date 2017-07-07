package com.eissler.micha.hbgvertretungsapp;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.eissler.micha.cloudmessaginglibrary.InfoNotification;
import com.eissler.micha.hbgvertretungsapp.fcm.NotificationViewActivity;
import com.eissler.micha.hbgvertretungsapp.settings.AutoName;
import com.eissler.micha.hbgvertretungsapp.settings.Blacklist;
import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.koushikdutta.ion.Ion;
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
//    private static final String INTERNET = "INTERNET";
//    private static final String USER_INPUT = "USER_INPUT";

//    private static final char[] UMLAUTS = "äöüÄÖÜß".toCharArray();


    public static final String ACTION_UPDATE = "ACTION_UPDATE";

    public static String installationId;


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

    public static void logTrace(String s) {
        Log.d(TRACE, s);
    }

    public static void report(Throwable e) {
        firebaseReport(e);

        acraLogFcmToken();
        ACRA.getErrorReporter().putCustomData(INFO, "Non-fatal error");
        ACRA.getErrorReporter().handleSilentException(e);
        ACRA.getErrorReporter().removeCustomData(INFO);
    }

    public static void exitWithError(Throwable e) {
        e.printStackTrace();
        FirebaseCrash.log("FATAL ERROR");
        firebaseReport(e);
        acraLogFcmToken();
        ACRA.getErrorReporter().handleException(e, true);
    }

    static void acraLogFcmToken() {
        ACRA.getErrorReporter().putCustomData("FCM-token", FirebaseInstanceId.getInstance().getToken());
    }

    static void firebaseReport(Throwable e) {
        FirebaseCrash.log("FCM-token: " + FirebaseInstanceId.getInstance().getToken());
        FirebaseCrash.log("ACRA-id: " + installationId);
//        FirebaseCrash.report(e); // TODO: 23.03.2017 UNCOMMENT
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isMillisecondsLater(Date pastDate, long millisLater) {
        return new Date().getTime() - pastDate.getTime() > millisLater;
    }

    public static NotificationCompat.Builder getIntentNotificationBuilder(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // TODO: 25.06.2017 check flags
        PendingIntent pendingIntent = PendingIntent.getActivity(context, RequestCodes.ACTION_OPEN_NOTIFICATION, intent, 0);

        return getNotificationBuilder(context)
                .setContentIntent(pendingIntent);
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.notification_icon)
                .setAutoCancel(true);

        Preferences defPrefs = Preferences.getDefaultPreferences(context);
        if (defPrefs.getBoolean(Preferences.Key.NOTIFICATION_VIBRATION, true)) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
//                .setVibrate(new long[]{0,200,200,200})
        }
        if (defPrefs.getBoolean(Preferences.Key.NOTIFICATION_LIGHT, true)) {
            int colorARGB = Integer.parseInt(defPrefs.getString(Preferences.Key.NOTIFICATION_LIGHT_COLOR, String.valueOf(Color.RED)));
            builder.setLights(colorARGB, 2000, 1000);
        }
        if (defPrefs.getBoolean(Preferences.Key.NOTIFICATION_SOUND, false)) {
            String soundUriString = defPrefs.getString(Preferences.Key.NOTIFICATION_SOUND_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            System.out.println("soundUriString = " + soundUriString);
            if (soundUriString.equals("")) {
                return builder;
            }
            builder.setSound(Uri.parse(soundUriString));
        }

        return builder;
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

//    public static String decodeUmlauts(String s) { //encoding/decoding needed because it was f***ing impossible to change default-encoding to UTF-8 on AppEngine: http://stackoverflow.com/questions/41204936/google-appengine-how-to-set-default-charset-file-encoding-to-utf-8-for-goo
//        for (char umlaut : UMLAUTS) {
//            s = s.replace("%" + (int) umlaut + "%", umlaut + "");
//        }
//        return s;
//    }
//
//    public static String encodeUmlauts(String s) {
//        for (char umlaut : UMLAUTS) {
//            s = s.replace(umlaut + "", "%" + (int) umlaut + "%");
//        }
//        return s;
//    }

    public static void lockOrientation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    public static int getNextPushId(int startId, Preferences.Key preferenceKey, Context context) {
        Preferences preferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, context);
        int id = preferences.getInt(preferenceKey, startId - 1) + 1;
        if (id == startId + 100) {
            id = startId;
        }
        System.out.println("push_notification_id = " + id);
        preferences.edit().putInt(preferenceKey, id).apply();
        return id;
    }

    public static void showErrorDialog(String errorText, Activity activity) {
        new MaterialDialog.Builder(activity)
            .title("Fehler")
            .content("Ein unerwarteter Fehler ist aufgetreten:\n" + errorText + " \nStarte die App neu und versuche es erneut.\n\nBitte melde mir schwerwiegende Fehler.")
            .positiveText("Ok")
            .neutralText("Melden")
            .onNeutral((dialog, which) -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                //i.setType("text/plain"); //use this line for testing in the emulator
                i.setType("message/rfc822") ; // use for live device
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"m.eissler@hotmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT,"HBG-Vertretungsapp Fehlermeldung");
                i.putExtra(Intent.EXTRA_TEXT, "Ein Fehler ist bei mir aufgetreten: \"" + errorText + "\"\n\n" +
                        "Folge: (z.B. \"Ein Fach konnte nicht umbenannt werden\")\n\n" +
                        "In welchem Kontext ist er aufgetreten: (z.B.\"In den Einstellungen unter \"Anzeigenamen\"\")\n\n\n\n\n" +
                        "Fehlerbehebungs-ID: " + FirebaseInstanceId.getInstance().getToken());
                activity.startActivity(Intent.createChooser(i, "Email-App auswählen:"));
            })
            .show();
    }

    public static void showNotification(InfoNotification notification, Context context) {
        String title = notification.getTitle();
        String body = notification.getContent();
        String imageUrl = notification.getImageUrl();

        Intent backIntent = new Intent(context, MainActivity.class);

        Intent intent = new Intent(context, NotificationViewActivity.class);

        intent.putExtra("title", title)
                .putExtra("body", body)
                .putExtra("imageUrl", imageUrl);
        int id = getNextPushId(RequestCodes.NOTIFICATION_PUSH, Preferences.Key.LAST_NOTIFICATION_ID, context);

        PendingIntent pendingIntent = PendingIntent.getActivities(context, id, new Intent[]{backIntent, intent}, 0); // TODO: 07.07.2017 different behaviours when multiple notifications received...

        final NotificationCompat.Builder notificationBuilder = getNotificationBuilder(context)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent);

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (imageUrl != null && !imageUrl.equals("")) try {
            Bitmap bitmap = Ion.with(context)
                    .load(imageUrl)
                    .asBitmap()
                    .get();

            System.out.println("Image downloaded");

            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(body));
            notificationManager.notify(id, notificationBuilder.build());
            return;
        } catch (Exception e) {
            if (!e.getMessage().equals("Invalid URI")) report(e);
            e.printStackTrace();
        }

        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        notificationManager.notify(id, notificationBuilder.build());
    }


    public interface WaitFor<T> {
        void onResult(T result);
    }
}
