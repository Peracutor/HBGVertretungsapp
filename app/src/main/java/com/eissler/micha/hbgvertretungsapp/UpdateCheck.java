package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import com.eissler.micha.hbgvertretungsapp.fcm.AppEngine;
import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.peracutor.hbgbackend.registration.Registration;
import com.peracutor.hbgbackend.registration.model.VersionInfo;
import com.peracutor.hbgserverapi.DownloadException;

import java.io.IOException;
import java.util.Date;

public class UpdateCheck {

    private Activity activity;
    private ProgressDialog progressDialog;
    private boolean isSettingsActivity;

    private boolean interrupted;
    private static Registration sRegistration;

    public UpdateCheck(final Activity activity) {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        this.activity = activity;

        isSettingsActivity = activity instanceof SettingsActivity;
        System.out.println("isSettingsActivity = " + isSettingsActivity);

        final UpdatePoll updatePoll = new UpdatePoll();

        if (isSettingsActivity) {
            ProgressDialog progressDialog = new ProgressDialog(activity);
            progressDialog.setTitle("Updatevorgang");
            progressDialog.setMessage("Suche nach Updates...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    interrupted = true;
                    updatePoll.cancel(true);
                }
            });
            progressDialog.show();
            this.progressDialog = progressDialog;

        }

        if (!App.isConnected(activity)) {
            showError(new DownloadException(DownloadException.ErrorType.NO_CONNECTION));
            return;
        }

        updatePoll.execute();
    }

    private class UpdatePoll extends AsyncTask<Void, Void, VersionInfo> {

        private IOException e;

        @Override
        protected VersionInfo doInBackground(Void... voids) {

            if (sRegistration == null) {
                sRegistration = AppEngine.getApiInstance(new Registration.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null));
            }

            try {
                return !interrupted ? sRegistration.getVersionInfo().execute() : null;
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(final VersionInfo result) {
            if (e != null) {
                showError(e);
                return;
            } else if (result == null) {
                return;
            }

            final int thisVersion;
            try {
                thisVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                App.exitWithError(e);
                return;
            }
            System.out.println("thisVersion = " + thisVersion);

            final int newVersion = result.getVersionNumber();
            System.out.println("newVersion = " + newVersion);
            if (thisVersion >= newVersion) {
                System.out.println("App is the newest version.");
                if (isSettingsActivity) showDialog(App.dialog("Kein Update verfügbar", "Es ist die neueste Version der App installiert", activity));
                return;
            }


            InstallApk installApk = InstallApk.getLastSavedApk(activity);

            if (installApk.getVersion() == result.getVersionNumber() && installApk.exists()) {
                showDialog(UpdateTask.getReadyForInstallationBuilder(activity, installApk));
                return;
            }
            //else
            if (!interrupted) {
                AlertDialog.Builder dialog = App.dialog("Update verfügbar", new StringBuilder("Es steht ein Update zu Version ").append(result.getVersionName()).append(" zum Download bereit.\n\n")
                        .append(result.getVersionDescription()), activity)
                        .setPositiveButton("Herunterladen", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new UpdateTask(result.getVersionNumber(), result.getApkUrl(), activity);
                            }
                        }).setNegativeButton("Später", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!isSettingsActivity) {
                                    App.dialog("Update später installieren", "Um das Update später zu installieren, wähle Einstellungen --> Nach Updates suchen", activity)
                                            .show();
                                }
                            }
                        });
                showDialog(dialog);
            }
        }
    }

    private void showDialog(final AlertDialog.Builder builder) {
        showDialog(builder, true);
    }

    private void showDialog(final AlertDialog.Builder builder, boolean successful) {
        if (interrupted || builder == null) {
            return;
        }

        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);


        if (((activity instanceof MainActivity) && !MainActivity.mainActivityPaused) || (isSettingsActivity && !SettingsActivity.settingsActivityPaused)) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.show();
                }
            });

            if (successful) {
                Preferences sharedPreferences = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, activity);
                sharedPreferences.edit().putLong(Preferences.Key.LAST_UPDATE_CHECK, new Date().getTime()).apply();
            }
        }
    }

    private void showError(Exception e) {
        if (interrupted) {
            return;
        }
        e = DownloadException.getCorrespondingExceptionFor(e);
        e.printStackTrace();
        if (isSettingsActivity) showDialog(App.dialog("Fehler", e.getMessage(), activity), false);
    }
}

