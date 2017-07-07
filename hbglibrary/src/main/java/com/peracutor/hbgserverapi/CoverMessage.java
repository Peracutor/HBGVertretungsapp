package com.peracutor.hbgserverapi;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 30.04.2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverMessage implements HBGMessage {

    private static final int SIZE = 8;

    public final static int DATE = 0;
    public final static int LESSON = 1;
    public final static int SUBJECT = 2;
    public final static int NEW_SUBJECT = 3;
    public final static int ROOM = 4;
    public final static int KIND = 5;
    public final static int COVER_TEXT = 6;
    public final static int CLASS = 7;

    private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);

    private String[] fields;
    protected int year;
    private Date concernedDate;

    @SuppressWarnings("unused")
    private CoverMessage() {
        fields = new String[SIZE];/*For Objectify*/
    }

    public CoverMessage(CoverMessage coverMessage) {
        fields = coverMessage.fields;
        concernedDate = coverMessage.concernedDate;
        year = coverMessage.year;
    }

    public CoverMessage(Builder coverMessageBuilder) {
        year = coverMessageBuilder.year;
        fields = coverMessageBuilder.msgFields;
        init();
    }

    private void init() {
        Calendar concernedCal = Calendar.getInstance();
        concernedCal.set(Calendar.SECOND, 0);
        concernedCal.set(Calendar.MILLISECOND, 0);
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

        if (get(LESSON).equals("?")) {
            concernedCal.set(Calendar.HOUR_OF_DAY, 23);
            concernedCal.set(Calendar.MINUTE, 59);
            concernedDate = concernedCal.getTime();
            return;
        }
        Calendar endOfClassCal = Calendar.getInstance();
        endOfClassCal.setTime(EndOfClass.get(getBeginningHour()));

        concernedCal.set(Calendar.HOUR_OF_DAY, endOfClassCal.get(Calendar.HOUR_OF_DAY));
        concernedCal.set(Calendar.MINUTE, endOfClassCal.get(Calendar.MINUTE));
        concernedDate = concernedCal.getTime();
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
        return fields[index];
    }

    public void set(int index, String field) {
        fields[index] = field;
    }

    public Date getConcernedDate() {
        return concernedDate;
    }

    @JsonIgnore
    public int getConcernedDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getConcernedDate());
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    @JsonIgnore
    public int getBeginningHour() {
        String lesson = get(LESSON).replace(".", "");

        int index = lesson.indexOf("-");
        if (index == -1) {
            return Integer.parseInt(lesson.trim());
        }
        return Integer.parseInt(lesson.substring(0, index).trim());
    }

    @JsonIgnore
    public int getEndingHour() {
        String lesson = get(LESSON).replace(".", "");

        int index = lesson.indexOf("-");
        if (index == -1) {
            return Integer.parseInt(lesson.trim());
        }
        return Integer.parseInt(lesson.substring(index + 1).trim());
    }

    public boolean tryMerge(CoverMessage message) {
        boolean mergeAllowed = true;
        for (int i = 0; i < SIZE; i++) {
            if (i == LESSON || i == COVER_TEXT) {
                continue;
            }

            if (!get(i).equals(message.get(i))) {
                mergeAllowed = false;
                break;
            }
        }
        if (!mergeAllowed) return false;

        boolean equalCoverTexts = get(COVER_TEXT).equals(message.get(COVER_TEXT));
        boolean noCoverText = get(COVER_TEXT).equals("");
        boolean noCompareCoverText = message.get(COVER_TEXT).equals("");
        if (!equalCoverTexts && !noCoverText && !noCompareCoverText) {
            return false;
        }

        if (get(LESSON).equals("?") || message.get(LESSON).equals("?")) {
            return false;
        }

        //begin of merging:

        if (message.get(LESSON).equals(get(LESSON))) return true; //messages identical

        int compareBeginningHour = message.getBeginningHour();
        int compareEndingHour = message.getEndingHour();
        int beginningHour = getBeginningHour();
        int endingHour = getEndingHour();

        String hourField = get(LESSON);
        if (endingHour == compareBeginningHour - 1) {
            set(LESSON, String.format("%s - %s", beginningHour, compareEndingHour));
        } else if (beginningHour == compareEndingHour + 1) {
            set(LESSON, String.format("%s - %s", compareBeginningHour, endingHour));
        } else if (!(beginningHour <= compareBeginningHour && compareEndingHour <= endingHour)) { //return false only if compareMessage hour-range is not included in this message
            return false;
        }

        if (!equalCoverTexts) {
            String coverTextFormat = "%1$s (Vertretungstext für Std. %2$s)";
            if (noCoverText) {
                set(COVER_TEXT, String.format(Locale.GERMANY, coverTextFormat, message.get(COVER_TEXT), message.get(LESSON)));
            } else {
                set(COVER_TEXT, String.format(Locale.GERMANY, coverTextFormat, get(COVER_TEXT), hourField));
            }
        }

        return true;
    }

    public Serializer<CoverMessage> serializer() {
        return new Serializer<CoverMessage>(this) {
            @Override
            protected void initMapper(ObjectMapper mapper) {
                mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            }
        };
    }

    public static Deserializer<CoverMessage> deserializer() {
        return new Deserializer<CoverMessage>(CoverMessage.class) {
            @Override
            protected void initMapper(ObjectMapper mapper) {
                mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            }
        };
    }

    public static abstract class Serializer<T> {

        private final ObjectMapper mapper;
        private T object;

        public Serializer(T objectClass) {
            this.object = objectClass;
            mapper = new ObjectMapper();
            initMapper(mapper);
        }

        protected abstract void initMapper(ObjectMapper mapper);

        public JsonNode toJson() {
            return mapper.valueToTree(object);
        }

        public String toJsonString() throws IOException {
            return mapper.writeValueAsString(object);
        }
    }

    public static abstract class Deserializer<T> {
        private final ObjectMapper mapper;
        private final Class<T> objectClass;

        public Deserializer(Class<T> objectClass) {
            this.objectClass = objectClass;
            mapper = new ObjectMapper();
            initMapper(mapper);
        }

        protected abstract void initMapper(ObjectMapper mapper);

        public T fromJson(JsonNode node) throws JsonProcessingException {
            return mapper.treeToValue(node, objectClass);
        }

        public T fromJsonString(String jsonString) throws IOException {
            return mapper.readValue(jsonString, objectClass);
        }
    }



    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public CoverMessage clone() {
        Builder copyBuilder = new Builder(year);
        System.arraycopy(fields, 0, copyBuilder.msgFields, 0, SIZE);
        return new CoverMessage(copyBuilder);
    }

    @Override
    public final String toString() {
        String room = get(ROOM).equals("") ?
                "" : " in Raum " +
                (get(ROOM).substring(0, 1).equals("R") ?
                        get(ROOM).substring(1) : get(ROOM));

        String kind = get(KIND).equalsIgnoreCase("anderer Raum") ?
                "" : get(KIND);

        String subject = get(SUBJECT);

        String newSubject = (get(NEW_SUBJECT).equals("") || get(NEW_SUBJECT).equals(subject)) ?
                "" : ": " + get(NEW_SUBJECT);

        String lesson = get(LESSON).contains("-") ?
                get(LESSON) : get(LESSON) + ".";

//        String date = SHORT_SDF.format(getConcernedDate().getTime());

        String space = subject.equals("") ? "" : " ";
        return /*date + ": " + */lesson + " Std. " + subject + space + kind + newSubject + room;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CoverMessage))
            return false;
        if (obj == this)
            return true;

        CoverMessage anotherMessage = (CoverMessage) obj;
        try {
            return serializer().toJsonString().equals(anotherMessage.serializer().toJsonString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
