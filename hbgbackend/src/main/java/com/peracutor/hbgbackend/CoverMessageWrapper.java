package com.peracutor.hbgbackend;

import com.eissler.micha.cloudmessaginglibrary.AsciiEncoder;
import com.eissler.micha.cloudmessaginglibrary.Recipients;
import com.peracutor.hbgserverapi.CoverMessage;

import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 05.06.2016
 */
public class CoverMessageWrapper extends CoverMessage {

    public CoverMessageWrapper(CoverMessage coverMessage) {
        super(coverMessage);
    }

    public String getTopic(int classNumber) {
        String topic;
        if (get(SUBJECT).equals("") && get(NEW_SUBJECT).equals("")) {
            topic = String.format(Locale.GERMANY, "%s-whitelist", classNumber);
        } else {
            topic = String.format("%s-%s",
                    classNumber,
                    !get(SUBJECT).equals("") ? get(SUBJECT) : get(NEW_SUBJECT));
        }

        return AsciiEncoder.encode(topic);
    }

    public Recipients getCondition(int classNumber) {
        return new Recipients().condition(new Recipients.Condition()
                .topic(getTopic(classNumber))
                .or().topic(classNumber + "-no_whitelist")
                .or().topic(getTopic(classNumber).toLowerCase()));
    }

    public int getTimeToLive() {
        return (int) (((getConcernedDate().getTime() - new Date().getTime()) / 1000) - (5 * 60));
    }
}
