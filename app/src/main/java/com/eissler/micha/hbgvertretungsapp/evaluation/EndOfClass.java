package com.eissler.micha.hbgvertretungsapp.evaluation;

import com.eissler.micha.hbgvertretungsapp.App;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Micha.
 * 30.04.2016
 */
public class EndOfClass {

    private static Date[] endOfClass;

    public static Date get(int hour) {
        if (endOfClass == null) try {
            endOfClass = new Date[]{App.TIME_SDF.parse("08:45"), App.TIME_SDF.parse("09:35"), App.TIME_SDF.parse("10:40"),
                    App.TIME_SDF.parse("11:35"), App.TIME_SDF.parse("12:30"), App.TIME_SDF.parse("13:25"), App.TIME_SDF.parse("14:40"),
                    App.TIME_SDF.parse("15:30"), App.TIME_SDF.parse("16:25"), App.TIME_SDF.parse("17:20"), App.TIME_SDF.parse("18:15"),
                    App.TIME_SDF.parse("19:10")};

        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return endOfClass[hour - 1];
    }
}
