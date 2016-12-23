package com.peracutor.hbgserverapi;

import java.nio.charset.Charset;

/**
 * Created by Micha.
 * 04.06.2016
 */
public interface HtmlDownloadHandler {
    void asyncDownload(String urlString, Charset charset, ResultCallback<String> callback);

    String syncDownload(String urlString, Charset charset) throws Exception;
}
