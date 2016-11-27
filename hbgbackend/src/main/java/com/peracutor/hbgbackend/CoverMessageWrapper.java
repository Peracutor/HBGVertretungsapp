package com.peracutor.hbgbackend;

import com.peracutor.hbgserverapi.CoverMessage;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Micha.
 * 05.06.2016
 */
public class CoverMessageWrapper extends CoverMessage {

    public CoverMessageWrapper(CoverMessage coverMessage) {
        super(coverMessage);
    }

//    public String getCollapseKey() {
//        String date = get(DATE);
//        String subject = get(SUBJECT).equals("") ? "n-" + get(NEW_SUBJECT) : get(SUBJECT);
//        String hour = get(HOUR);
//        return !hour.equals("") && !subject.equals("") ?
//                String.format("%s/%s/%s", date.substring(0, date.length()-1).replace('.', '-'), hour, subject)
//                : null;
//    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>(9);
        map.put("Jahr", String.valueOf(year));

        for (int i = 0; i < 8; i++) {
            map.put(String.valueOf(i), get(i));
        }
        return map;
    }

    public String getTopic(int classNum) {
        String topic;
        if (get(SUBJECT).equals("") && get(NEW_SUBJECT).equals("")) {
            topic = String.format("/topics/%s", classNum);
        } else {
            topic = String.format("/topics/%s-%s",
                    classNum,
                    !get(SUBJECT).equals("") ? get(SUBJECT) : get(NEW_SUBJECT));
        }
        return topic;
    }

    public int getTimeToLive() {
        return (int) (((getConcernedDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 1000) - (5 * 60));
    }
}
