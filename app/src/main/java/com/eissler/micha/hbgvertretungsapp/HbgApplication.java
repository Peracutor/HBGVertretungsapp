package com.eissler.micha.hbgvertretungsapp;

import android.app.Application;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.TimeZone;

//import com.squareup.leakcanary.LeakCanary;

@ReportsCrashes(
        formUri = "https://hbg-vertretungsapp.cloudant.com/acra-hbg_app_crash_reports/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="dsheyourtinedshersubtaim",
        formUriBasicAuthPassword="1924d7856f1ac39e34ee53573f650c0fd227c5e9",
        logcatArguments = {"-t", "150", "-v", "time"},
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_text
)
public class HbgApplication extends Application {

    public static final String HBG_APP = "HbgApplication";

//    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
//        Logger.init().hideThreadInfo();
//        LeakCanary.install(this);
        TimeZone.setDefault(TimeZone.getTimeZone("CET"));
//        Ion.getDefault(this).configure().setLogging("Ion", Log.INFO);
        // The following line triggers the initialization of ACRA
//        ACRA.init(this); // TODO: 02.03.2016 uncomment!!!
    }

    //    /**
//     * Gets the default {@link Tracker} for this {@link Application}.
//     *
//     * @return tracker
//     */
//    synchronized public Tracker getDefaultTracker() {
//        if (mTracker == null) {
//            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//            analytics.setDryRun(true);
//            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
//            mTracker = analytics.newTracker("Test"); //there stood "R.xml.global_tracker"
//        }
//        return mTracker;
//
//    }
}
