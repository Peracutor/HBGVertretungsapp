package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.eissler.micha.hbgvertretungsapp.util.Preferences;

import java.io.File;
import java.util.Locale;

/**
 * Created by Micha.
 * 10.05.2016
 */
public class InstallApk extends File {

    public static final String APK_NAME_FORMAT = "hbg-vertretungsapp-%d.apk";
    private final Preferences preference;

    private int version;

    public InstallApk(int newVersion, Context context) {
        super(getInstallApkDirectory(context), String.format(Locale.GERMANY, APK_NAME_FORMAT, newVersion));
        version = newVersion;
        preference = getPref(context);
    }


    private static Preferences getPref(Context context) {
        return Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, context);
    }

    public static InstallApk getLastSavedApk(Context context) {
        int version = -1;
        try {
            version = getPref(context).getInt(Preferences.Key.APK, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new InstallApk(version, context);
    }

    public void save() {
        preference.edit().putInt(Preferences.Key.APK, version).apply();
    }

    public int getVersion() {
        return version;
    }

    private static File getInstallApkDirectory(Context context) {
        return context.getFilesDir();
    }

    public void deleteWithToast(Activity activity) {
        boolean deleted = super.delete();

        if (deleted) {
            Toast.makeText(activity, R.string.act_ma_del_install_apk_success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity, R.string.act_ma_del_install_apk_failure, Toast.LENGTH_LONG).show();
        }
    }

    //    private InstallApk(String jsonString, Context context) throws JSONException {
//        super(getInstallApkDirectory(context), new JSONObject(jsonString).getString("name"));
//        JSONObject json = new JSONObject(jsonString);
//        version = json.getInt("version");
//        preference = getPref(context);
//    }

//    private static InstallApk fromJsonString(String jsonString, Context context) throws JSONException {
//        return new InstallApk(jsonString, context);
//    }

//    private String toJsonString() throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("name", name);
//        json.put("version", version);
//        return json.toString();
//    }


}
