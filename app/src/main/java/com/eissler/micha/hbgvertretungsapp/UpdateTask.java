package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class UpdateTask {

    final static String APK_VERSION = "ApkVersion";
    final static String PREFERENCE = "MainPreference";
    public static final String APK = "APK";

    private final ProgressDialog downloadProgress;
    private Future<String> apkUrlDownload;
    private Future<File> apkDownload;


    private boolean interrupted;
    private SharedPreferences prefs;
    private Activity activity;


    public UpdateTask (final String newVersion, final Activity activity) {
        this.activity = activity;

        prefs = activity.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);

        downloadProgress = new ProgressDialog(activity);
        downloadProgress.setTitle("HBG-App Update");
        downloadProgress.setMessage("Bereite Download vor...");
        downloadProgress.setCancelable(false);
        downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        downloadProgress.setMax(100);
        downloadProgress.setProgress(0);

        DialogInterface.OnClickListener defaultListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interrupted = true;

                if (apkUrlDownload != null) {
                    apkUrlDownload.cancel();
                }

                if (apkDownload != null) {
                    apkDownload.cancel();
                }

                if (Build.VERSION.SDK_INT >= 18) {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                }

                if ((activity instanceof MainActivity && !MainActivity.mainActivityPaused) || (activity instanceof SettingsActivity && !SettingsActivity.settingsActivityPaused)) {
                    App.dialog("Abbruch", "Der Download wurde abgebrochen.", activity).show();
                }
            }
        };
        downloadProgress.setButton(DialogInterface.BUTTON_NEGATIVE, "Abbrechen", defaultListener);
        downloadProgress.show();


        if (!interrupted) apkUrlDownload = Ion.with(activity)
                .load("https://www.dropbox.com/s/2eb4xe192i1icv5/apk_download_url.txt?dl=1")
                .asString(Charset.forName("UTF-8"))
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String apkUrl) {
                        if (e != null) {
                            showError(e);
                            return;
                        }


                        String version = newVersion.replace(".", "_");

                        final String filename = "hbg-vertretungsapp-" + version + ".apk";
                        System.out.println("filename = " + filename);

                        downloadProgress.setMessage("Lade Update herunter...");
                        downloadProgress.setProgress(0);

                        if (!interrupted) {
                            final InstallApk installApk = new InstallApk(filename, newVersion);
                            apkDownload = Ion.with(activity)
                                    .load(apkUrl)
                                    .setLogging("Ion", Log.VERBOSE)
                                    .progressDialog(downloadProgress)
                                    .noCache()
                                    .write(installApk.getApkFile(activity))
                                    .setCallback(new FutureCallback<File>() {
                                        @Override
                                        public void onCompleted(Exception e, final File apkFile) {
                                            if (e != null) {
                                                showError(e);
                                                return;
                                            }

                                            if (Build.VERSION.SDK_INT >= 18) {
                                                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                                            }


//                                            System.out.println("Worldreadable");
//                                            boolean success = apkFile.setReadable(true, false);
//                                            boolean b = apkFile.setExecutable(true, false);
//                                            boolean b1 = apkFile.setWritable(true, false);
//
//                                            System.out.println("b1 = " + b1);
//                                            System.out.println("b = " + b);
//                                            System.out.println("success = " + success);


                                            //                                        prefs.edit().putString(APK_VERSION, newVersion).apply();
                                            try {
                                                prefs.edit().putString(APK, installApk.toJsonString()).apply();
                                            } catch (JSONException e1) {
                                                e1.printStackTrace();
                                            }

                                            if ((activity instanceof MainActivity && !MainActivity.mainActivityPaused) || (activity instanceof SettingsActivity && !SettingsActivity.settingsActivityPaused)) {
                                                AlertDialog.Builder dialog = App.dialog("Bereit zur Installation", "Das Update ist jetzt bereit zur Installation.\nJetzt installieren?", activity)
                                                        .setPositiveButton("Jetzt", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                                                                activity.startActivity(intent);
                                                            }
                                                        })
                                                        .setNegativeButton("Später", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                App.dialog("Update später installieren", "Um das Update später zu installieren, wähle Einstellungen --> Nach Updates suchen", activity)
                                                                        .show();
                                                            }
                                                        });

                                                showDialog(dialog);
                                            }
                                        }
                                    });
                        }

                    }
                });

    }

    private void showError(Exception e) {
        if (interrupted) {
            return;
        }

        if (downloadProgress != null) {
            downloadProgress.dismiss();
        }
        // TODO: 04.05.2016 handle exceptions
        e.printStackTrace();
        showDialog(App.dialog("Fehler", e.getMessage(), activity)); // TODO: 04.05.2016 do not use showDialog, because of preference
    }

    private void showDialog(final AlertDialog.Builder builder) {
        if (interrupted) {
            return;
        }

        if (downloadProgress != null) {
            downloadProgress.dismiss();
        }

        if (Build.VERSION.SDK_INT >= 18) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        if ((!(activity instanceof MainActivity) || MainActivity.mainActivityPaused) && (!(activity instanceof SettingsActivity) || SettingsActivity.settingsActivityPaused)) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (builder != null) {
                    builder.show();
                }
            }
        });
    }
}
