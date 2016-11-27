package com.eissler.micha.hbgvertretungsapp.settings;

import android.app.Activity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Micha.
 * 20.09.2016
 */
public class AutoName {

    private static final Map<String, String> subjectDictionary = Collections.unmodifiableMap(new HashMap<String, String>(){
        {
            put("en","Englisch");
            put("de","Deutsch");
            put("ku", "Kunst");
            put("ek", "Erdkunde");
            put("ma", "Mathe");
            put("mu", "Musik");
            put("ge", "Geschichte");
            put("pw", "PW");
            put("pl", "Philosophie");
            put("ph", "Physik");
            put("la", "Latein");
            put("fr", "Französisch");
            put("gr", "Griechisch");
            put("in", "Informatik");
            put("bi", "Biologie");
            put("ch", "Chemie");
            put("ds", "DS");
            put("me", "Ensemblekurs"); // TODO: 20.09.2016 für Chor, Orchester und BigBand gleich?

            put("gym", "Gymnastik-Tanz");
            put("hck", "Hockey");
            put("ttn", "Tischtennis");
            put("bdm", "Badminton");
            put("fub", "Fußball");
            put("swi", "Schwimmen");
            put("lat", "Leichtathletik");
            put("tur", "Turnen");
            put("vob", "Volleyball");
            put("fit", "Fitness");

            put("d", "Deutsch");
            put("m", "Mathe");
            put("e", "Engisch");
            put("l", "Latein");
            put("g", "Geschichte");
            put("f", "Französisch");
            put("eth", "Ethik");
            put("rel", "Religion");
            put("sp", "Sport");
            put("mchor", "Mädchenchor");
            put("sinf", "Sinfonietta");
        }
    });


    private final Whitelist whitelist;
    private final CustomNames customNames;

    public AutoName(Activity activity) {
        whitelist = Whitelist.get(activity);
        customNames = CustomNames.get(activity);
    }

    public static int autoNameWhitelist(Activity activity) {
        return new AutoName(activity).execute();
    }

    private int execute() {
        int renamings = 0;
        for (String subject : whitelist) {
            if (customNames.get(subject) == null) {
                String target;
                if (subject.matches("[GL][A-Za-z]{2}[NAÜ]\\d?")) { //Oberstufen-Kürzel
                    target = subject.substring(1, 3);
                } else if (subject.matches("S[A-Z]{3}\\d?")) { //Oberstufen-Sportkürzel
                    target = subject.substring(1, 4);
                } else {
                    target = subject;
                }

                target = target.toLowerCase();

                String newName = subjectDictionary.get(target);
                if (newName != null) {
                    customNames.put(subject, newName);
                    renamings++;
                }
            }
        }

        customNames.save();

        return renamings;
//            com.google.code.gson : gson : 2.7
    }

    public static Map<String, String> getSubjectDictionary() {
        return subjectDictionary;
    }
}
