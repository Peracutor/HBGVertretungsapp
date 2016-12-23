package com.peracutor.hbgserverapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Micha.
 * 11.12.2016
 */
public class AutoName {

    private static final Map<String, String> SUBJECT_DICTIONARY = Collections.unmodifiableMap(new HashMap<String, String>(){
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
            put("the", "Sport-Theorie");

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
            // TODO: 15.12.2016 put("sowi", "Sozial-Wissensch.") ?
        }
    });

    private final boolean k;
    private final boolean nk;
    private final boolean n;
    private String autoNamePattern;

    public AutoName(String autoNamePattern) {
        n = autoNamePattern.contains("*n") && !autoNamePattern.contains("*k");
        k = !autoNamePattern.contains("*n") && autoNamePattern.contains("*k");
        nk = autoNamePattern.contains("*n") && autoNamePattern.contains("*k");
        this.autoNamePattern = autoNamePattern
                .replace('%', ' ')
                .replace("*f", "%1$s")
                .replace("*n", "%2$s")
                .replace("*k", nk ? "%3$s" : "%2$s");
    }

    public String getAutoName(String subject) {
        String subjectAbbr;
        String classType;
        String classCount;
        if (subject.matches("[GL][A-Za-z]{2}[NAÜ]\\d?")) { //Oberstufen-Kürzel
            subjectAbbr = subject.substring(1, 3);
            classType = subject.substring(0, 1) + "K";
            classCount = subject.substring(4);

        } else if (subject.matches("S[A-Za-z]{3}\\d?")) { //Oberstufen-Sportkürzel
            subjectAbbr = subject.substring(1, 4);
            classType = ""; //S
            classCount = subject.substring(4);
        } else {
            subjectAbbr = subject;
            classCount = "";
            classType = "";
        }

        String subjectName = SUBJECT_DICTIONARY.get(subjectAbbr.toLowerCase());
        if (subjectName == null) {
            subjectName = subject;
        }
        if (nk)
            return String.format(autoNamePattern, subjectName, classCount, classType);
        if (n)
            return String.format(autoNamePattern, subjectName, classCount);
        if (k)
            return String.format(autoNamePattern, subjectName, classType);

        return String.format(autoNamePattern, subjectName);
    }
}
