package com.eissler.micha.hbgvertretungsapp;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    final static String CUSTOM_NAMES = "CustomNames";

    private static final String CODE_SECTION_REACHED = "CODE_SECTION_REACHED";
    private static final String ERROR = "ERROR";
    private static final String INFO = "INFO";
    private static final String TRACE = "Trace";
    private static final String INTERNET = "INTERNET";
    private static final String USER_INPUT = "USER_INPUT";


    public static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);
    public static final SimpleDateFormat TIME_SDF = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    public static final SimpleDateFormat NORMAL_SDF = new SimpleDateFormat("dd.MM.yy", Locale.GERMANY);
    public static final SimpleDateFormat PRECISE_SDF = new SimpleDateFormat("EE HH:mm:ss dd.MM.yy", Locale.GERMANY);
    public static final SimpleDateFormat DAY_NAME_SDF = new SimpleDateFormat("EE", Locale.GERMANY);





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
            throw new IOException("Retrieved Object was null."/* Expected: " + objectType.getClass().getName()*/);
        }

//        if (!(objectType.getClass().isInstance(object))) {
//            throw new ClassCastException("Object not of expected class.: '" + object.getClass() + "', expected '" + objectType.getClass() + "'");
//        }

        //noinspection unchecked
        return (T) object;
    }


    static public AlertDialog.Builder dialog(String title, String msg, Context activity){ return new AlertDialog.Builder(activity).setTitle(title).setMessage(msg).setPositiveButton("Ok", null); }

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

        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isMillisecondsLater(Date lastReload, long millisLater) {
        return new Date().getTime() - lastReload.getTime() > millisLater;
    }
}
