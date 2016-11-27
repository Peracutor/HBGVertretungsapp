package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.peracutor.hbgserverapi.DownloadException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class UpdateTask {

    private final ProgressDialog downloadProgress;
//    private Future<String> apkUrlDownload;
    private Future<FileOutputStream> apkDownload;


    private boolean interrupted;
    private Activity activity;
    private InstallApk installApk;



    @SuppressLint("WorldReadableFiles")
    public UpdateTask(final int newVersion, String apkUrl, final Activity activity) {
        this.activity = activity;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        final ProgressDialog downloadProgress = new ProgressDialog(activity);
        downloadProgress.setTitle("HBG-App Update");
        downloadProgress.setMessage("Bereite Download vor...");
        downloadProgress.setCancelable(false);
        downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        downloadProgress.setMax(100);
        downloadProgress.setProgress(0);

        downloadProgress.setButton(DialogInterface.BUTTON_NEGATIVE, "Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interrupted = true;

//                if (apkUrlDownload != null) {
//                    apkUrlDownload.cancel();
//                }

                if (apkDownload != null) {
                    apkDownload.cancel();
                }

                if (installApk != null) {
                    //noinspection ResultOfMethodCallIgnored
                    installApk.delete();
                }

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                }

                if ((activity instanceof MainActivity && !MainActivity.mainActivityPaused) || (activity instanceof SettingsActivity && !SettingsActivity.settingsActivityPaused)) {
                    App.dialog("Abbruch", "Der Download wurde abgebrochen.", activity).show();
                }
            }
        });
        System.out.println("show");
        downloadProgress.show();
        this.downloadProgress = downloadProgress;

        downloadProgress.setMessage("Lade Update herunter...");
        downloadProgress.setProgress(0);

        if (!interrupted) {
            installApk = new InstallApk(newVersion, activity);
            FileOutputStream outputStream = null;
            try {
                //noinspection deprecation
                outputStream = activity.openFileOutput(installApk.getName(), Context.MODE_WORLD_READABLE);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            apkDownload = Ion.with(activity)
                    .load(apkUrl)
                    .progressDialog(downloadProgress)
                    .noCache()
                    .write(outputStream)
                    .setCallback(new FutureCallback<FileOutputStream>() {
                        @Override
                        public void onCompleted(Exception e, final FileOutputStream outStreamUnused) {
                            if (e != null) {
                                showError(e);
                                return;
                            }

                            if (Build.VERSION.SDK_INT >= 18) {
                                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                            }

                            installApk.save();

                            if ((activity instanceof MainActivity && !MainActivity.mainActivityPaused) || (activity instanceof SettingsActivity && !SettingsActivity.settingsActivityPaused)) {
                                AlertDialog.Builder dialog = getReadyForInstallationBuilder(activity, installApk);

                                showDialog(dialog);
                            }
                        }
                    });
        }

    }

    static AlertDialog.Builder getReadyForInstallationBuilder(final Activity activity, final InstallApk installApk) {
        return App.dialog("Bereit zur Installation", "Das Update ist bereit zur Installation.\nJetzt installieren?", activity)
                                                            .setPositiveButton("Jetzt", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog1, int which) {
                                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                                    intent.setDataAndType(Uri.fromFile(installApk), "application/vnd.android.package-archive");
                                                                    activity.startActivity(intent);
                                                                }
                                                            })
                                                            .setNegativeButton("Später", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog1, int which) {
                                                                    if ((!(activity instanceof MainActivity) || MainActivity.mainActivityPaused) && (!(activity instanceof SettingsActivity) || SettingsActivity.settingsActivityPaused)) {
                                                                        App.dialog("Update später installieren", "Um das Update später zu installieren, wähle Einstellungen --> Nach Updates suchen", activity)
                                                                                .show();
                                                                    }
                                                                }
                                                            })
                                                            .setNeutralButton("APK löschen", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    App.dialog("Löschen bestätigen", "Bist du dir sicher, dass du die heruntergeladene Update-Datei löschen möchtest?", activity)
                                                                            .setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
                                                                                @Override
                                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                                    installApk.deleteWithToast(activity);
                                                                                }
                                                                            })
                                                                            .setNegativeButton("Abbrechen", null)
                                                                            .show();
                                                                }
                                                            });
    }

    private void showError(Exception e) {
        if (interrupted) {
            return;
        }

        e = DownloadException.getCorrespondingExceptionFor(e);
        e.printStackTrace();
        showDialog(App.dialog("Fehler", e.getMessage(), activity));
    }

    private void showDialog(final AlertDialog.Builder builder) {
        if (interrupted) {
            return;
        }

        if (downloadProgress != null) {
            downloadProgress.dismiss();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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
