package com.eissler.micha.hbgvertretungsapp;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Ion;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class Download {

    private int fileLength;
    private InputStream inputStream;
    private final Context context;
    private StringBuilder stringBuilder;
    private final DownloadProgressInterface downloadProgress;
    private final int bufferSize;
    private boolean interrupted = false;
    private boolean connectionEstablished = false;


    public Download(int bufferSize, Context context) throws IOException, NoInternetConnectionException, InterruptedException {
        this(bufferSize, null, context);
    }

    public Download(int bufferSize, DownloadProgressInterface downloadProgress, Context context) {
        App.logCodeSection("Download");

        this.context = context;

        if (downloadProgress == null && context instanceof DownloadProgressInterface) {
            downloadProgress = (DownloadProgressInterface) context;
        }
        this.downloadProgress = downloadProgress;

        this.bufferSize = bufferSize;

    }

    public Download connect(String urlStr, String internetAction, int timeoutInMs) throws IOException, NoInternetConnectionException, InterruptedException {
        App.logCodeSection("Connect");

        if (!connected()) {
            App.logError("Not connected");
            throw new NoInternetConnectionException("Not connected to the internet.");
        }

        App.logInternet(internetAction);

        if (interrupted) {
            throw new InterruptedException();
        }

        java.net.URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(timeoutInMs /* milliseconds */);
        connection.setConnectTimeout(timeoutInMs /* milliseconds */);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        if (interrupted) {
            throw new InterruptedException();
        }


        // Starts the query
        connection.connect();

        fileLength = connection.getContentLength();

        inputStream = url.openStream();

        if (interrupted) {
            throw new InterruptedException();
        }

        connectionEstablished = true;

        App.logInfo("Connection established");
        return this;
    }

    public String dataToString() throws IOException, InterruptedException {

        if (!connectionEstablished) {
            throw new InterruptedException("No connection established!");
        }

        if (downloadProgress != null && fileLength != -1) {
            return downloadShowingProgress();
        } else {
            return downloadWithoutProgress();
        }

    }

    private String downloadWithoutProgress() throws IOException, InterruptedException {
        InputStreamReader inputStreamReader= new InputStreamReader(inputStream, "windows-1252");
        char[] chars = new char[bufferSize];

        System.out.println("downloading without progress");

        if (interrupted) {
            throw new InterruptedException();
        }

        try {
            stringBuilder = new StringBuilder(fileLength == -1 ? 3000 : fileLength);
            while (inputStreamReader.read(chars) != -1 && !interrupted) {
                stringBuilder.append(chars);
            }
        } finally {
            inputStreamReader.close();
        }

        if (interrupted) {
            throw new InterruptedException();
        }

        return stringBuilder.toString();
    }

    private String downloadShowingProgress() throws IOException, InterruptedException {
        System.out.println("downloading with progress");

        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "windows-1252");
        char[] chars = new char[bufferSize];

        try {
            long total = 0;
            int count, latestPercentDone;
            int percentDone = -1;

            stringBuilder = new StringBuilder(fileLength);

            if (interrupted) {
                throw new InterruptedException();
            }

            while ((count = inputStreamReader.read(chars)) != -1 && !interrupted) {
                total += count;
                // Publish the progress
                latestPercentDone = (int) total * 100 / fileLength;
                if (percentDone != latestPercentDone) {
                    percentDone = latestPercentDone;
                    downloadProgress.showProgress(percentDone);
                }
                stringBuilder.append(chars);
            }

            if (interrupted) {
                throw new InterruptedException();
            }

            return stringBuilder.toString();
        } finally {
            inputStreamReader.close();
        }
    }

//    public void downloadAndWriteToStorage(String filepath) throws IOException, InterruptedException {
//
//        InputStream input = new BufferedInputStream(inputStream);
//        OutputStream output = new FileOutputStream(filepath);
//
//        try {
//            byte[] byteBuffer = new byte[bufferSize];
//            long total = 0;
//            int count, latestPercentDone;
//            int percentDone = -1;
//
//            while ((count = input.read(byteBuffer)) != -1 && !interrupted) {
//                total += count;
//                // Publish the progress
//                latestPercentDone = (int) total * 100 / fileLength;
//                if (percentDone != latestPercentDone) {
//                    percentDone = latestPercentDone;
//                    downloadProgress.showProgress(percentDone);
//                }
//                output.write(byteBuffer, 0, count);
//            }
//
//            if (interrupted) {
//                throw new InterruptedException();
//            }
//        } finally {
//            output.flush();
//            output.close();
//            input.close();
//        }
//    }

    public void interrupt() {
        interrupted = true;
    }

    private boolean connected() {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }

    public int getFileLength() {
        return fileLength;
    }

    public interface DownloadProgressInterface {
        void showProgress(int progress);
    }

    public static class NoInternetConnectionException extends Exception {
        NoInternetConnectionException(String s) {
            super(s);
        }
    }
}
