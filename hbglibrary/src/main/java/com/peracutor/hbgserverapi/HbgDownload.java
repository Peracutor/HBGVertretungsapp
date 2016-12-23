package com.peracutor.hbgserverapi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * Created by Micha.
 * 14.11.2016
 */
public abstract class HbgDownload<T> implements HtmlDownloadHandler {
    private String url;
    private HtmlDownloadHandler downloadHandler;

    public HbgDownload(String url) {
        setUrl(url);
        downloadHandler = this;
    }

    public HbgDownload(String url, HtmlDownloadHandler downloadHandler) {
        setUrl(url);
        this.downloadHandler = downloadHandler;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String URL) {
        this.url = URL;
    }

    public void executeAsync(final ResultCallback<T> callback) {
        downloadHandler.asyncDownload(getUrl(), Charset.forName("ISO-8859-1"), new ResultCallback<String>() {
            @Override
            public void onError(Throwable t) {
                callback.onError(onException((Exception) t));
            }

            @Override
            public void onResult(String htmlText) {
                callback.onResult(evaluate(htmlText));
            }
        });
    }


    public T executeSync() throws Exception {
        String htmlText;
        try {
            htmlText = downloadHandler.syncDownload(getUrl(), Charset.forName("ISO-8859-1"));
        } catch (IOException e) {
            throw onException(e);
        }

        return evaluate(htmlText);
    }

    @Override
    public void asyncDownload(String urlString, Charset charset, ResultCallback<String> callback) {
        throw new RuntimeException("implement your asynchronous download-logic by overriding asyncDownload() or passing a HtmlDownloadHandler");
    }

    @Override
    public String syncDownload(String urlString, Charset charset) throws Exception {
        java.net.URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        Reader r = new InputStreamReader(con.getInputStream(), charset);
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = r.read();
            if (ch < 0)
                break;
            buf.append((char) ch);
        }
        return buf.toString();
    }

    protected abstract T evaluate(String htmlText);

    protected Exception onException(Exception e){
        return DownloadException.getCorrespondingExceptionFor(e);
    }
}
