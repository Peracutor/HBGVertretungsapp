package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.eissler.micha.hbgvertretungsapp.util.DownloadException;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Ion;

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

        App.lockOrientation(activity);

        final ProgressDialog downloadProgress = new ProgressDialog(activity);
        downloadProgress.setTitle("HBG-App Update");
        downloadProgress.setMessage("Bereite Download vor...");
        downloadProgress.setCancelable(false);
        downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        downloadProgress.setMax(100);
        downloadProgress.setProgress(0);

        downloadProgress.setButton(DialogInterface.BUTTON_NEGATIVE, "Abbrechen", (dialog, which) -> {
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

            App.unlockOrientation(activity);

            if ((activity instanceof MainActivity && !MainActivity.mainActivityPaused) || (activity instanceof SettingsActivity && !SettingsActivity.settingsActivityPaused)) {
                App.dialog("Abbruch", "Der Download wurde abgebrochen.", activity).show();
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
                    .setCallback((e, outStreamUnused) -> {
                        if (e != null) {
                            showError(e);
                            return;
                        }

                        App.unlockOrientation(activity);

                        installApk.save();

                        if ((activity instanceof MainActivity && !MainActivity.mainActivityPaused) || (activity instanceof SettingsActivity && !SettingsActivity.settingsActivityPaused)) {
                            AlertDialog.Builder dialog = getReadyForInstallationBuilder(activity, installApk);

                            showDialog(dialog);
                        }
                    });
        }

    }

    static AlertDialog.Builder getReadyForInstallationBuilder(final Activity activity, final InstallApk installApk) {
        return App.dialog("Bereit zur Installation", "Das Update ist bereit zur Installation.\nJetzt installieren?", activity)
                                                            .setPositiveButton("Jetzt", (dialog1, which) -> {
                                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                                intent.setDataAndType(Uri.fromFile(installApk), "application/vnd.android.package-archive");
                                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                activity.startActivity(intent);
                                                            })
                                                            .setNegativeButton("Später", (dialog1, which) -> {
                                                                if ((!(activity instanceof MainActivity) || MainActivity.mainActivityPaused) && (!(activity instanceof SettingsActivity) || SettingsActivity.settingsActivityPaused)) {
                                                                    App.dialog("Update später installieren", "Um das Update später zu installieren, wähle Einstellungen --> Nach Updates suchen", activity)
                                                                            .show();
                                                                }
                                                            })
                                                            .setNeutralButton("APK löschen", (dialogInterface, i) -> App.dialog("Löschen bestätigen", "Bist du dir sicher, dass du die heruntergeladene Update-Datei löschen möchtest?", activity)
                                                                    .setPositiveButton("Löschen", (dialogInterface1, i1) -> installApk.deleteWithToast(activity))
                                                                    .setNegativeButton("Abbrechen", null)
                                                                    .show());
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

        App.unlockOrientation(activity);

        if ((!(activity instanceof MainActivity) || MainActivity.mainActivityPaused) && (!(activity instanceof SettingsActivity) || SettingsActivity.settingsActivityPaused)) {
            return;
        }

        activity.runOnUiThread(() -> {
            if (builder != null) {
                builder.show();
            }
        });
    }
}
