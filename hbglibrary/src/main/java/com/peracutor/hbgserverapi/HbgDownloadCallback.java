package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 14.11.2016
 */
public interface HbgDownloadCallback {
    void onDownloadCompleted(SortedCoverMessages sortedCoverMessages, DownloadException e);
}
