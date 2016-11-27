package com.eissler.micha.hbgvertretungsapp.evaluation;

import android.content.Context;
import android.widget.ProgressBar;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.peracutor.hbgserverapi.DownloadException;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.ResultCallback;

import java.nio.charset.Charset;

/**
 * Created by Micha.
 * 04.06.2016
 */
public class HbgDownload extends HbgDataDownload {

    private final Context context;
    private ProgressBar progressBar;

    public HbgDownload(int weekNumber, Context context) {
        this(weekNumber, null, context);
    }


    public HbgDownload(int weekNumber, ProgressBar progressBar, Context context) {
        this(-1, weekNumber, progressBar, context);
    }

    public HbgDownload(int classNum, int weekNumber, ProgressBar progressBar, final Context context) {
        super(classNum == -1 ? Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, context).getInt(Preferences.Key.SELECTED_CLASS, 0) : classNum, weekNumber);
        this.context = context;
        this.progressBar = progressBar;
    }

    @Override
    public void asyncDownload(String urlString, Charset charset, final ResultCallback<String> callback) {
        if (!App.isConnected(context)) {
            callback.onError(new DownloadException(DownloadException.ErrorType.NO_CONNECTION));
            return;
        }

        Ion.with(context)
                .load(urlString)
//                .setLogging("DOWNLOAD", Log.VERBOSE)
                .noCache()
                .progress(progressBar != null ? new ProgressCallback() {
                    @Override
                    public void onProgress(long downloaded, long total) {
                        int progress = (int) (downloaded * 100 / total);
                        progressBar.setProgress(progress);
                    }
                } : null)
                .asString(charset)
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (progressBar != null) progressBar.setProgress(100);
                        if (e != null) {
                            callback.onError(e);
                        } else {
                            callback.onResult(result);
                        }
                    }
                });
    }

//    @Override
//    public void cancel() {
//        super.cancel();
//        if (download != null) download.cancel();
//    }
}
