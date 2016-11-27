package com.peracutor.hbgserverapi;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Created by Micha.
 * 23.08.2016
 */
public class DownloadException extends Exception {

    public enum ErrorType {
        BAD_CONNECTION,
        NO_CONNECTION,
        ERROR
    }

    public DownloadException(ErrorType errorType) {
        super(getMessageForErrorType(errorType));
    }

    public DownloadException(ErrorType errorType, Throwable cause) {
        super(getMessageForErrorType(errorType), cause);
    }

    public DownloadException(Throwable cause) {
        super(getMessageForErrorType(getErrorTypeFor(cause)), cause);
    }

    private static String getMessageForErrorType(ErrorType type) {
        switch (type) {
            case BAD_CONNECTION:
                return "Verbindung fehlgeschlagen";
            case NO_CONNECTION:
                return "Keine Internetverbindung";
            case ERROR:
                return "Fehler beim Laden"; // TODO: 19.10.2016 Wortwahl
        }
        return null;
    }

    public static DownloadException getCorrespondingExceptionFor(Throwable e) {
        System.out.println("Error/Cause: " + e.getMessage());
        e.printStackTrace();
        if (e instanceof DownloadException) {
            return (DownloadException) e;
        } else {
            return new DownloadException(e);
        }
    }

    public static ErrorType getErrorTypeFor(Throwable t) {
        if (t instanceof SocketException || t instanceof UnknownHostException || t instanceof SocketTimeoutException || t instanceof TimeoutException) {
            return ErrorType.BAD_CONNECTION;
        } else {
            return ErrorType.ERROR; // TODO: 02.05.2016 EOF Exception
        }
    }
}
