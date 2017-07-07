package com.eissler.micha.cloudmessaginglibrary;

/**
 * Created by Micha.
 * 04.06.2017
 */

public class AsciiEncoder {

    public static final char MARK = '%';

    public static String encode(String s) {
        StringBuilder result = new StringBuilder(s.length() + 8 /*buffer for 2 special characters*/);
        for (char c : s.toCharArray()) {
            if ((int) c > 127 || c == MARK) {
                result.append(MARK).append((int) c).append(MARK);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
