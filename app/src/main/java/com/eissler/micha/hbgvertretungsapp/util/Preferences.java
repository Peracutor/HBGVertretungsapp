package com.eissler.micha.hbgvertretungsapp.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Created by Micha.
 * 01.10.2016
 */
public class Preferences implements SharedPreferences {


    public enum Preference {
        MAIN_PREFERENCE("MainPreference");

        private String preferenceName;

        Preference(String preferenceName) {
            this.preferenceName = preferenceName;
        }


        public String getPreferenceName() {
            return preferenceName;
        }
    }

    public enum Key {
        LAST_UPDATE_CHECK("LastUpdateCheck"),
        FIRST_START("FirstStart"),
        SELECTED_CLASS("classSelection"),
        VERSION("version"),
        SUBSCRIBED_TOPICS("subscribed_topics"),
        NOTIFICATION_SWITCH("notification_switch"),
        APK("APK"),
        TOKEN_SENT("token_sent"),
        NOTIFICATION_SERVICE_BACKOFF("notification_service_backoff"),
        REGISTER_CHECK_BACKOFF("register_check_backoff"),
        PUSH_NOTIFICATION_SWITCH("push_notifications"),
        ALARM_TIME_1("time_for_alarm_1"),
        ALARM_TIME_2("time_for_alarm_2"),
        WHITELIST_SWITCH("whitelist_switch"),
        ALARM_WAKEUP("alarm_wake_up"),
        TEST_PUSH_PROMPTED("test_push_prompted"),
        AUTO_NAME("auto_name_subjects"),
        AUTO_NAME_PATTERN("auto_name_pattern"),

        NOTIFICATION_VIBRATION("notification_vibration"),
        NOTIFICATION_LIGHT("notification_light"),
        NOTIFICATION_SOUND("notification_sound"),
        NOTIFICATION_LIGHT_COLOR("notification_light_color"),
        NOTIFICATION_SOUND_URI("notification_sound_uri"),
        LAST_PUSH_ID("last_push_id"),
        WEEK_CHANGE_NOTIFICATION("week_change_notification"),
        PUSH_INFO_SHOWN("push_info_shown"),
        WHITELIST_DONT_PROMPT("whitelist_dont_prompt"),
        DONT_PROMPT_FILTER_MODE("dont_prompt_filter_mode"),
        SHOW_WHITELIST_CONFIRMATION_PROMPT("show_whitelist_confirmation_prompt"),
        LAST_NOTIFICATION_ID("last_notification_id");

        private String key;

        Key(String key) {
            this.key = key;
        }


        public String getKey() {
            return key;
        }
    }

    private SharedPreferences mPrefs;

    private Preferences(SharedPreferences sharedPreferences) {
        this.mPrefs = sharedPreferences;
    }

    public static Preferences getDefaultPreferences(Context context) {
        return new Preferences(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static Preferences getPreference(Preference preference, Context context) {
        return new Preferences(context.getSharedPreferences(preference.getPreferenceName(), Context.MODE_PRIVATE));
    }

    public String getString(Key key, String defaultValue) {
        return mPrefs.getString(key.getKey(), defaultValue);
    }

    public int getInt(Key key, int defaultValue) {
        return mPrefs.getInt(key.getKey(), defaultValue);
    }

    public long getLong(Key key, long defaultValue) {
        return mPrefs.getLong(key.getKey(), defaultValue);
    }

    public boolean getBoolean(Key key, boolean defaultValue) {
        return mPrefs.getBoolean(key.getKey(), defaultValue);
    }

    public Set<String> getStringSet(Key key, Set<String> defaultSet) {
        return mPrefs.getStringSet(key.getKey(), defaultSet);
    }

    @Override
    public Map<String, ?> getAll() {
        return mPrefs.getAll();
    }

    @Nullable
    @Override
    public String getString(String s, String s1) {
        return mPrefs.getString(s, s1);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String s, Set<String> set) {
        return mPrefs.getStringSet(s, set);
    }

    @Override
    public int getInt(String s, int i) {
        return mPrefs.getInt(s, i);
    }

    @Override
    public long getLong(String s, long l) {
        return mPrefs.getLong(s, l);
    }

    @Override
    public float getFloat(String s, float v) {
        return mPrefs.getFloat(s, v);
    }

    @Override
    public boolean getBoolean(String s, boolean b) {
        return mPrefs.getBoolean(s, b);
    }

    @Override
    public boolean contains(String s) {
        return mPrefs.contains(s);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public Editor edit() {
        return new Editor(mPrefs.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        mPrefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        mPrefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public class Editor implements SharedPreferences.Editor {
        private SharedPreferences.Editor e;

        public Editor(SharedPreferences.Editor editor) {
            e = editor;
        }

        public SharedPreferences.Editor putString(Key key, String value) {
            return e.putString(key.getKey(), value);
        }

        public SharedPreferences.Editor putInt(Key key, int value) {
            return e.putInt(key.getKey(), value);
        }

        public SharedPreferences.Editor putLong(Key key, long value) {
            return e.putLong(key.getKey(), value);
        }

        public SharedPreferences.Editor putBoolean(Key key, boolean value) {
            return e.putBoolean(key.getKey(), value);
        }

        public SharedPreferences.Editor putStringSet(Key key, Set<String> defaultSet) {
            return e.putStringSet(key.getKey(), defaultSet);
        }

        @Override
        public SharedPreferences.Editor putString(String s, String s1) {
            return e.putString(s, s1);
        }

        @Override
        public SharedPreferences.Editor putStringSet(String s, Set<String> set) {
            return e.putStringSet(s, set);
        }

        @Override
        public SharedPreferences.Editor putInt(String s, int i) {
            return e.putInt(s, i);
        }

        @Override
        public SharedPreferences.Editor putLong(String s, long l) {
            return e.putLong(s, l);
        }

        @Override
        public SharedPreferences.Editor putFloat(String s, float v) {
            return e.putFloat(s, v);
        }

        @Override
        public SharedPreferences.Editor putBoolean(String s, boolean b) {
            return e.putBoolean(s, b);
        }

        @Override
        public SharedPreferences.Editor remove(String s) {
            return e.remove(s);
        }

        @Override
        public SharedPreferences.Editor clear() {
            return e.clear();
        }

        @Override
        public boolean commit() {
            return e.commit();
        }

        @Override
        public void apply() {
            e.apply();
        }
    }
}
