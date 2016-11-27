package com.peracutor.hbgserverapi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 30.04.2016
 */
public class CoverMessage implements HBGMessage {

    private static final int SIZE = 8;

    public final static int DATE = 0;
    public final static int HOUR = 1;
    public final static int SUBJECT = 2;
    public final static int NEW_SUBJECT = 3;
    public final static int ROOM = 4;
    public final static int KIND = 5;
    public final static int COVER_TEXT = 6;
//    public final static int CLASS = 7;

    private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);

    private String[] msgFields;
    private Date concernedDate;
    protected int year;

    @SuppressWarnings("unused")
    private CoverMessage() {
        msgFields = new String[SIZE];/*For Objectify*/
    }

    public CoverMessage(CoverMessage coverMessage) {
        msgFields = coverMessage.msgFields;
        concernedDate = coverMessage.concernedDate;
        year = coverMessage.year;
    }

    public CoverMessage(Builder coverMessageBuilder) {
        year = coverMessageBuilder.year;
        msgFields = coverMessageBuilder.msgFields;
    }


    public static class Builder {
        private final int year;
        private String[] msgFields;

        public Builder(int year) {
            this.year = year;
            msgFields = new String[SIZE];
            for (int i = 0; i < msgFields.length; i++) {
                msgFields[i] = "";
            }
        }

        public Builder setField(int field, String value) {
            msgFields[field] = value;
            return this;
        }
    }

    public String get(int index) {
        return msgFields[index];
    }

    public void set(int index, String field) {
        msgFields[index] = field;
    }

    public boolean tryMerge(CoverMessage message) {
        boolean mergeSuccessful = true;
        for (int i = 0; i < SIZE; i++) {
            if (i == HOUR/* || i == COVER_TEXT*/) {
                continue;
            }

            if (!get(i).equals(message.get(i))) {
                mergeSuccessful = false;
                break;
            }
        }

        if (get(HOUR).equals("?") || message.get(HOUR).equals("?")) {
            mergeSuccessful = false;
        }

        if (!mergeSuccessful) return false;

//        String coverText = get(COVER_TEXT);
//        String compareCoverText = message.get(COVER_TEXT);
//        if (!compareCoverText.equals(coverText) && !coverText.equals("") && !compareCoverText.equals("")) {
//            return false;
//        }
//
//        if (coverText.equals("")) {
//            set(COVER_TEXT, compareCoverText);
//        }
        String hourField = get(HOUR);
        String compareHourField = message.get(HOUR);

        if (compareHourField.equals(hourField)) return true; //messages identical

        int compareBeginningHour = getBeginningHour(compareHourField);
        int compareEndingHour = getEndingHour(compareHourField);
        int beginningHour = getBeginningHour(hourField);
        int endingHour = getEndingHour(hourField);

        if (beginningHour <= compareBeginningHour && compareEndingHour <= endingHour) return true; //compareMessage info is included in this message

        if (hourField.length() > 1) {
            if (endingHour == compareBeginningHour - 1) {
                set(CoverMessage.HOUR, hourField.replace(hourField.split("-")[1].trim(), String.valueOf(compareEndingHour)));
            } else if (beginningHour == compareEndingHour + 1) {
                set(CoverMessage.HOUR, hourField.replace(hourField.split("-")[0].trim(), String.valueOf(compareBeginningHour)));
            } else {
                return false;
            }
        } else {
            if (endingHour == compareBeginningHour - 1) {
                set(CoverMessage.HOUR, String.format("%s - %s", hourField, compareEndingHour));
            } else if (beginningHour == compareEndingHour + 1) {
                set(CoverMessage.HOUR, String.format("%s - %s", compareBeginningHour, hourField));
            } else {
                return false;
            }
        }

        return true;
    }

    public Calendar getConcernedDate() {
        Calendar concernedCal = Calendar.getInstance();
        if (concernedDate == null) {
            concernedCal.set(Calendar.SECOND, 0);
            concernedCal.set(Calendar.YEAR, year);

            Calendar concernedDay = Calendar.getInstance();
            try {
                concernedDay.setTime(SHORT_SDF.parse(get(DATE)));
            } catch (ParseException e) {
                e.printStackTrace();
                System.exit(1);
            }

            concernedCal.set(Calendar.MONTH, concernedDay.get(Calendar.MONTH));
            concernedCal.set(Calendar.DAY_OF_MONTH, concernedDay.get(Calendar.DAY_OF_MONTH));

            String hour = get(HOUR);
            if (hour.equals("?")) {
                concernedCal.set(Calendar.HOUR_OF_DAY, 23);
                concernedCal.set(Calendar.MINUTE, 59);
                return concernedCal;
            }
            int classHour = getBeginningHour(hour);
            Calendar endOfClassCal = Calendar.getInstance();
            endOfClassCal.setTime(EndOfClass.get(classHour));

            concernedCal.set(Calendar.HOUR_OF_DAY, endOfClassCal.get(Calendar.HOUR_OF_DAY));
            concernedCal.set(Calendar.MINUTE, endOfClassCal.get(Calendar.MINUTE));
            concernedDate = concernedCal.getTime();
        } else {
            concernedCal.setTime(concernedDate);
        }

        return concernedCal;
    }

    private static int getBeginningHour(String hour) {
        hour = hour.replace(".", "");

        int index = hour.indexOf("-");
        if (index == -1) {
            return Integer.parseInt(hour.trim());
        }
        return Integer.parseInt(hour.substring(0, index).trim());
    }

    private static int getEndingHour(String hour) {
        hour = hour.replace(".", "");

        int index = hour.indexOf("-");
        if (index == -1) {
            return Integer.parseInt(hour.trim());
        }
        return Integer.parseInt(hour.substring(index + 1).trim());
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public CoverMessage clone() {
        Builder copyBuilder = new Builder(year);
        for (int i = 0; i < SIZE; i++) {
            copyBuilder.setField(i, get(i));
        }
        return new CoverMessage(copyBuilder);
    }

    @Override
    public String toString() {
//        StringBuilder stringBuilder = new StringBuilder(SIZE);
//        stringBuilder.append("Message(").append(year).append(", ").append(SHORT_SDF.format(concernedDate)).append("): ");
//        for (int i = 0; i < SIZE; i++) {
//            stringBuilder.append(get(i)).append(", ");
//        }
//        return stringBuilder.toString();
        String room = get(ROOM).equals("") ?
                "" : " in Raum " +
                (get(ROOM).substring(0, 1).equals("R") ?
                        get(ROOM).substring(1) : get(ROOM));

        String kind = get(KIND).equalsIgnoreCase("anderer Raum") ?
                "" : get(KIND);

        String subject = get(SUBJECT);

        String newSubject = (get(NEW_SUBJECT).equals("") || get(NEW_SUBJECT).equals(subject)) ?
                "" : ": " + get(NEW_SUBJECT);

        String hour = get(HOUR).length() > 1 ?
                get(HOUR) : get(HOUR) + ".";

//        String date = SHORT_SDF.format(getConcernedDate().getTime());

        String space = subject.equals("") ? "" : " ";
        return /*date + ": " + */hour + " Std. " + subject + space + kind + newSubject + room;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CoverMessage))
            return false;
        if (obj == this)
            return true;

        CoverMessage anotherMessage = (CoverMessage) obj;
        return toString().equals(anotherMessage.toString());
    }
}
