package com.peracutor.hbgserverapi;

import java.nio.charset.Charset;

/**
 * Created by Micha.
 * 14.11.2016
 */
public abstract class HbgDownload<T> {
    private String url;
    private HtmlDownloadHandler downloadHandler;

    public HbgDownload(String url) {
        this.url = url;
        downloadHandler = new BasicDownloadHandler();
    }

    public HbgDownload(String url, HtmlDownloadHandler downloadHandler) {
        this.url = url;
        this.downloadHandler = downloadHandler != null ? downloadHandler : new BasicDownloadHandler();
    }

    public void executeAsync(final ResultCallback<T> callback) {
        downloadHandler.asyncDownload(url, Charset.forName("ISO-8859-1"), new ResultCallback<String>() {
            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }

            @Override
            public void onResult(String htmlText) {
                try {
                    callback.onResult(evaluate(htmlText));
                } catch (Exception e) {
                    callback.onError(new Exception("Fehler beim Parsen", e));
                }
            }
        });
    }


    public T executeSync() throws Exception {
        String htmlText = downloadHandler.syncDownload(url, Charset.forName("ISO-8859-1"));
        try {
            return evaluate(htmlText);
        } catch (Exception e) {
            throw new Exception("Fehler beim Parsen", e);
        }
    }

    protected abstract T evaluate(String htmlText) throws Exception;

}
