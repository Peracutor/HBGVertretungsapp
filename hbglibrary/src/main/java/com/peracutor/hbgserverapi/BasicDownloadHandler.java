package com.peracutor.hbgserverapi;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;

/**
 * Created by Micha.
 * 26.06.2017
 */
public class BasicDownloadHandler implements HtmlDownloadHandler {

    @Override
    public void asyncDownload(String urlString, Charset charset, ResultCallback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                callback.onResult(syncDownload(urlString, charset));
            } catch (Exception e) {
                callback.onError(e);
            }
        });
        Executors.newSingleThreadExecutor().shutdown();
    }

    @Override
    public String syncDownload(String urlString, Charset charset) throws Exception {
        java.net.URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        Reader r = new InputStreamReader(con.getInputStream(), charset);
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = r.read();
            if (ch < 0) break;
            else buf.append((char) ch);
        }
        return buf.toString();
    }
}
