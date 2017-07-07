package com.eissler.micha.hbgvertretungsapp.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.eissler.micha.hbgvertretungsapp.R;

/**
 * Created by Micha.
 * 06.01.2017
 */

public class NotificationSettingsActivity extends SettingsActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new NotificationSettingsFragment()).commit();
    }

    public static class NotificationSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_screen_notifications);
        }
    }
}
