package com.eissler.micha.hbgvertretungsapp.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    public static boolean settingsActivityPaused = true;
//    private Tracker mTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
//        mTracker = ((HbgApplication) getApplication()).getDefaultTracker();

    }

    @Override
    protected void onResume() {
        super.onResume();
//        mTracker.setScreenName("Settings");
//        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        settingsActivityPaused = false;


    }

    @Override
    protected void onPause() {
        super.onPause();
        settingsActivityPaused = true;
    }
}