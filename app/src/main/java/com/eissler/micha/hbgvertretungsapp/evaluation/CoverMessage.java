package com.eissler.micha.hbgvertretungsapp.evaluation;

import com.eissler.micha.hbgvertretungsapp.App;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Micha.
 * 30.04.2016
 */
public class CoverMessage implements HBGMessage {

    public final static int DATE = 0;
    public final static int HOUR = 1;
    public final static int SUBJECT = 2;
    public final static int NEW_SUBJECT = 3;
    public final static int ROOM = 4;
    public final static int KIND = 5;
    public final static int COVER_TEXT = 6;
    public final static int CLASS = 7;

    private final String[] msgFields;
    private Calendar concernedDate;
    private int year;

    public CoverMessage(int year) {
        this.year = year;
        msgFields = new String[8];
    }

    public void setField(int field, String value) {
        msgFields[field] = value;
    }

    public String getField(int field) {
        return msgFields[field];
    }

    public Calendar getConcernedDate() {
        if (concernedDate == null) {
            concernedDate = Calendar.getInstance();
            concernedDate.set(Calendar.SECOND, 0);
            concernedDate.set(Calendar.YEAR, year);

            Calendar concernedDay = Calendar.getInstance();
            try {
                concernedDay.setTime(App.SHORT_SDF.parse(getField(DATE)));
            } catch (ParseException e) {
                e.printStackTrace();
                App.reportUnexpectedException(e);
                System.exit(1);
            }

            concernedDate.set(Calendar.MONTH, concernedDay.get(Calendar.MONTH));
            concernedDate.set(Calendar.DAY_OF_MONTH, concernedDay.get(Calendar.DAY_OF_MONTH));

            int classHour = getBeginningHour(getField(HOUR));
            Date endOfClass = EndOfClass.get(classHour);
            Calendar endOfClassCal = Calendar.getInstance();
            endOfClassCal.setTime(endOfClass);

            concernedDate.set(Calendar.HOUR_OF_DAY, endOfClassCal.get(Calendar.HOUR_OF_DAY));
            concernedDate.set(Calendar.MINUTE, endOfClassCal.get(Calendar.MINUTE));
        }

        return concernedDate;
    }

    private static int getBeginningHour(String hour) {
        hour = hour.replace(".", "");

        int index = hour.indexOf("-");
        if (index == -1) {
            return Integer.parseInt(hour.trim());
        }
        return Integer.parseInt(hour.substring(0, index).trim());
    }

    @Override
    public Type getMessageType() {
        return Type.COVER_MESSAGE;
    }

    public CoverMessage copy() {
        CoverMessage copy = new CoverMessage(year);
        for (int i = 0; i < 8; i++) {
            copy.setField(i, getField(i));
        }

        return copy;
    }
}
