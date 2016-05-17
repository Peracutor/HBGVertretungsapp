package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by Micha.
 * 10.05.2016
 */
public class InstallApk {

    private String name;
    private String versionName;

    public InstallApk(File apkFile, String versionName) {
        this.versionName = versionName;
        name = apkFile.getName();
    }

    public InstallApk(String name, String newVersionName) {
        this.name = name;
        versionName = newVersionName;
    }

    public String getName() {
        return name;
    }

    public String getVersionName() {
        return versionName;
    }

    public File getApkFile(Context context) {
        return new File(getInstallApkDirectory(context), name);
    }

    private File getInstallApkDirectory(Context context) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); //context.getFilesDir(); //
    }

    public static InstallApk fromJsonString(String jsonString) throws JSONException {
        return new InstallApk(jsonString);
    }

    public String toJsonString() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("versionName", versionName);
        return json.toString();
    }

    private InstallApk(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        name = json.getString("name");
        versionName = json.getString("versionName");
    }


}
