package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 14.11.2016
 */
public interface ResultCallback<T> {
    void onError(Throwable t);

    void onResult(T result);
}
