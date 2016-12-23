package com.eissler.micha.hbgvertretungsapp.util;

/**
 * Created by Micha.
 * 12.12.2016
 */

public abstract class ExponentialBackoff {

    private int retries;
    private long value;
    private double exponent;
    private int maxRetries;
    private long startValue;
    private Saver saver;

    protected ExponentialBackoff(long startValue, double exponent, int maxRetries, Saver saver) { // TODO: 16.12.2016 maybe add constructor without saver
        this.exponent = exponent;
        this.maxRetries = maxRetries;
        this.startValue = startValue;
        this.saver = saver;
        Long savedValue = saver.getSavedValue();
        value = savedValue != null && savedValue != 0 ? savedValue : startValue / 2;

        retries = (int) (Math.log(2 * value / startValue) / Math.log(2));
    }

    public boolean retry() {
        value *= exponent;
        saver.save(value);
        retries++;
        return retries <= maxRetries;
    }

    public long getValue() {
        return value;
    }

//    public int getRetries() {
//        return retries;
//    }

    public void reset() {
        retries = 0;
        value = startValue / 2;
        saver.save(value);
    }

    interface Saver {
        void save(long value);

        Long getSavedValue();
    }
}
