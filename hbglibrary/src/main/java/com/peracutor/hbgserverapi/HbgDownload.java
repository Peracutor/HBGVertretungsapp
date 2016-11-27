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
    private String URL;

    public HbgDownload(String URL) {
        setUrl(URL);
    }

    public String getUrl() {
        return URL;
    }

    public void setUrl(String URL) {
        this.URL = URL;
    }

    public void executeAsync(final ResultCallback<T> callback) {
        System.out.println("starting evaluation");
        asyncDownload(getUrl(), Charset.forName("ISO-8859-15"), new ResultCallback<String>() {
            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR");
//                if (t instanceof FileNotFoundException) {
//                    callback.onResult(null); // no data for this week
//                } else
                callback.onError(onException((Exception) t));
            }

            @Override
            public void onResult(String htmlText) {
                System.out.println("SUCCESS");
                callback.onResult(evaluate(htmlText));
            }
        });
    }


    public T executeSync() throws Exception {
        String htmlText;
        try {
            htmlText = syncDownload(getUrl(), Charset.forName("ISO-8859-15"));
        } catch (IOException e) {
            throw onException(e);
        }

        return evaluate(htmlText);
    }

    @Override
    public void asyncDownload(String urlString, Charset charset, ResultCallback<String> callback) {
        throw new RuntimeException("asyncDownload() must be overridden and implement asynchronous download-logic with ResultCallback");
    }

    @Override
    public String syncDownload(String urlString, Charset charset) throws IOException {
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
