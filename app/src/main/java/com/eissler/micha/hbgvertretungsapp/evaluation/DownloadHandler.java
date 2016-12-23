package com.eissler.micha.hbgvertretungsapp.evaluation;

import android.content.Context;
import android.widget.ProgressBar;

import com.eissler.micha.hbgvertretungsapp.App;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.future.ResponseFuture;
import com.peracutor.hbgserverapi.DownloadException;
import com.peracutor.hbgserverapi.HtmlDownloadHandler;
import com.peracutor.hbgserverapi.ResultCallback;

import java.nio.charset.Charset;

/**
 * Created by Micha.
 * 10.12.2016
 */
public class DownloadHandler implements HtmlDownloadHandler {
    private final Context context;
    private final ProgressBar progressBar;

    public DownloadHandler(Context context) {
        this.context = context;
        progressBar = null;
    }

    public DownloadHandler(Context context, ProgressBar progressBar) {
        this.context = context;
        this.progressBar = progressBar;
    }

    @Override
    public void asyncDownload(String urlString, Charset charset, final ResultCallback<String> callback) {
        if (!App.isConnected(context)) {
            callback.onError(new DownloadException(DownloadException.ErrorType.NO_CONNECTION));
            return;
        }

        getIonBuilder(urlString, charset)
                .setCallback((e, result) -> {
                    if (progressBar != null) progressBar.setProgress(100);
                    if (e != null) {
                        callback.onError(e);
                    } else {
                        callback.onResult(result);
                    }
                });
    }

    @Override
    public String syncDownload(String urlString, Charset charset) throws Exception {
        if (!App.isConnected(context)) {
            throw new DownloadException(DownloadException.ErrorType.NO_CONNECTION);
        }
        return getIonBuilder(urlString, charset).get();
    }

    private ResponseFuture<String> getIonBuilder(String urlString, Charset charset) {
        return Ion.with(context)
                .load(urlString)
//                .setLogging("DOWNLOAD", Log.VERBOSE)
                .noCache()
                .progress(progressBar == null ? null : (downloaded, total) -> {
                    int progress = (int) (downloaded * 100 / total);
                    progressBar.setProgress(progress);
                })
                .asString(charset);
    }
}
