package com.peracutor.hbgserverapi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 30.04.2016
 */
public class EndOfClass {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm", Locale.GERMANY);

    private static Date[] endOfClass;

    public static Date get(int hour) {
        if (endOfClass == null) try {
            endOfClass = new Date[]{SDF.parse("08:45"), SDF.parse("09:35"), SDF.parse("10:40"),
                    SDF.parse("11:35"), SDF.parse("12:30"), SDF.parse("13:25"), SDF.parse("14:40"),
                    SDF.parse("15:30"), SDF.parse("16:25"), SDF.parse("17:20"), SDF.parse("18:15"),
                    SDF.parse("19:10")};

        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return endOfClass[hour - 1];
    }
}
