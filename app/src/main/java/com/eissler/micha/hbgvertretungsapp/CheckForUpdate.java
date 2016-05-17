package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckForUpdate {

    private Future<String> versionDownload;
    private Future<String> versionNameDownload;
    private Future<String> infoTextDownload;

    Activity activity;
    ProgressDialog progressDialog;
    boolean isSettingsActivity;

    private boolean interrupted;

    public CheckForUpdate(final Activity activity) {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED); // TODO: 04.05.2016 change orientation back
        }
        this.activity = activity;

        isSettingsActivity = activity instanceof SettingsActivity;
        System.out.println("isSettingsActivity = " + isSettingsActivity);

//            int orientation = activity.getResources().getConfiguration().orientation;
//            System.out.println("orientation == Configuration.ORIENTATION_LANDSCAPE = " + (orientation == Configuration.ORIENTATION_LANDSCAPE));
//            System.out.println("orientation == Configuration.ORIENTATION_PORTRAIT = " + (orientation == Configuration.ORIENTATION_PORTRAIT));

        if (isSettingsActivity) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setTitle("Updatevorgang");
            progressDialog.setMessage("Suche nach Updates...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    interrupted = true;
                    if (versionDownload != null) {
                        versionDownload.cancel();
                    }
                    if (versionNameDownload != null) {
                        versionNameDownload.cancel();
                    }
                    if (infoTextDownload != null) {
                        infoTextDownload.cancel();
                    }
                }
            });
            progressDialog.show();
        }

        final int thisVersion;
        try {
            thisVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
        System.out.println("thisVersion = " + thisVersion);

        if (!App.isConnected(activity)) {
            showError(new Download.NoInternetConnectionException("Es besteht keine Internetverbindung")); // TODO: 05.05.2016 choose best way to show error
            return;
        }

        if (!interrupted) versionDownload = Ion.with(activity)
                .load("https://www.dropbox.com/s/o3bborqjm8i6xmw/neueste_version.txt?dl=1")
                .asString(Charset.forName("UTF-8"))
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String newVersionString) {
                        if (e != null) {
                            showError(e);
                            return;
                        }

                        final int newVersion = Integer.parseInt(newVersionString);
                        System.out.println("newVersion = " + newVersion);
                        if (thisVersion == newVersion) { // TODO: 05.05.2016 remove
                            System.out.println("App is the newest version.");
                            showDialog(App.dialog("Kein Update verfügbar", "Es ist die neueste Version der App installiert", activity));
                            return;
                        }


                        if (!interrupted) versionNameDownload = Ion.with(activity)
                                .load("https://www.dropbox.com/s/3ysw5dxq3ssyc5x/neueste_version_name.txt?dl=1")
                                .asString(Charset.forName("UTF-8"))
                                .setCallback(new FutureCallback<String>() {
                                    @Override
                                    public void onCompleted(Exception e, final String newVersionName) {
                                        if (e != null) {
                                            showError(e);
                                            return;
                                        }

                                        System.out.println("thisVersion = " + thisVersion);

                                        String thisVersionName;
                                        try {
                                            thisVersionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                                        } catch (PackageManager.NameNotFoundException e2) {
                                            System.exit(1);
                                            return;
                                        }

                                        String jsonApk = MainActivity.prefs.getString(UpdateTask.APK, thisVersionName);

                                        InstallApk apk = null;
                                        File apkFile = null;
                                        try {
                                            apk = InstallApk.fromJsonString(jsonApk);
                                            apkFile = apk.getApkFile(activity);

                                        } catch (JSONException e1) {
                                            e1.printStackTrace();
//                                            showError(e1);
//                                            return;
                                        }

//                                        String apkVersion = MainActivity.prefs.getString(UpdateTask.APK_VERSION, thisVersionName);
//                                        String apkName = "hbg-vertretungsapp-" + apkVersion.replace(".", "_") + ".apk";
//                                        System.out.println("apkVersion = " + apkVersion);
//

                                        if (apk != null && apk.getVersionName().equals(newVersionName) && apkFile.exists()) {

                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            activity.setResult(Activity.RESULT_OK, intent);
                                            if (!interrupted) activity.startActivity(intent);

                                            if (Build.VERSION.SDK_INT >= 18) {
                                                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                                            }

                                            if (progressDialog != null) {
                                                progressDialog.dismiss();
                                            }

                                            final File finalApkFile = apkFile;
                                            App.dialog(null, "Löschen?", activity).setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    finalApkFile.delete(); // TODO: 07.05.2016 remove
                                                }
                                            }).show();
                                        }

                                        {
                                            if (!interrupted) infoTextDownload = Ion.with(activity)
                                                    .load("https://www.dropbox.com/s/m39p4sc0gt5m4hj/version_infotext.txt?dl=1")
                                                    .asString(Charset.forName("UTF-8"))
                                                    .setCallback(new FutureCallback<String>() {
                                                        @Override
                                                        public void onCompleted(Exception e, final String infoText) {
                                                            System.out.println("UTF");
                                                            if (e != null) {
                                                                showError(e);
                                                                return;
                                                            }

                                                            AlertDialog.Builder dialog = App.dialog("Update verfügbar", "Es steht ein Update zu Version " + newVersionName + " zum Download bereit." +
                                                                    "\n\n" + infoText, activity)
                                                                    .setPositiveButton("Herunterladen", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            new UpdateTask(newVersionName, activity);
                                                                        }
                                                                    }).setNegativeButton("Später", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            if (!isSettingsActivity) {
                                                                                App.dialog("Update später installieren", "Um das Update später zu installieren, wähle Einstellungen --> Update", activity)
                                                                                        .show();
                                                                            }
                                                                            if (Build.VERSION.SDK_INT >= 18) {
                                                                                System.out.println("ORIENTATION SENSOR");
                                                                                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                                                                            }
                                                                        }
                                                                    });


                                                            showDialog(dialog);
                                                        }
                                                    });
                                        }
                                    }
                                });
                    }
                });
    }

    private void showDialog(final AlertDialog.Builder builder) {
        if (interrupted) {
            return;
        }

        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        if ((!(activity instanceof MainActivity) || MainActivity.mainActivityPaused) && (!(activity instanceof SettingsActivity) || SettingsActivity.settingsActivityPaused)) {
            return;
        }
//            if (android.os.Build.VERSION.SDK_INT < 18) {
//                dialog("Achtung", "Deine Android Version ist schon etwas älter, um Fehler zu vermeiden, drehe während des Update-Prozesses bitte nicht dein Handy.", activity)
//                        .show();
//            }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (builder != null) {
                    builder.show();
                }
            }
        });

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
        MainActivity.prefs.edit().putString(MainActivity.LAST_UPDATE_CHECK, dateFormat.format(new Date())).apply();
    }

    private void showError(Exception e) {
        if (interrupted) {
            return;
        }
        // TODO: 04.05.2016 handle exceptions
        e.printStackTrace();
        showDialog(App.dialog("Fehler", e.getMessage(), activity)); // TODO: 04.05.2016 do not use showDialog, because of preference
    }
}

