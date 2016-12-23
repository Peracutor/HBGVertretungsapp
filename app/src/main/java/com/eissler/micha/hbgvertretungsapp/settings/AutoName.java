package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.util.Preferences;

/**
 * Created by Micha.
 * 20.09.2016
 */
public class AutoName extends com.peracutor.hbgserverapi.AutoName {

    public AutoName(Context context) {
        super(Preferences.getDefaultPreferences(context).getString(Preferences.Key.AUTO_NAME_PATTERN, ""));
    }

    public static boolean isAutoNamingEnabled(Context context) {
        return Preferences.getDefaultPreferences(context).getBoolean(Preferences.Key.AUTO_NAME, true);
    }

    //    public static int autoNameWhitelist(Activity activity) {
//        return new AutoName(activity).execute();
//    }
//
//    private int execute() {
//        int renamings = 0;
//        for (String subject : whitelist) {
//            if (customNames.get(subject) == null) {
//                String newName = getAutoName(subject);
//                if (newName != null) {
//                    customNames.put(subject, newName);
//                    renamings++;
//                }
//            }
//        }
//
//        customNames.save();
//
//        return renamings;
//    }
}
