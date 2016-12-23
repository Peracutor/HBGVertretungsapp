package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 07.12.2016
 */

public class ReplacedCoverMessage extends CoverMessage {
    private String replacedSubject;
    private String replacedNewSubject;

//    public ReplacedCoverMessage(Builder coverMessageBuilder) {
//        super(coverMessageBuilder);
//    }

    public ReplacedCoverMessage(CoverMessage coverMessage) {
        super(coverMessage);
    }

    @Override
    public String get(int index) {
        if (index == SUBJECT && replacedSubject != null) {
            return replacedSubject;
        } else if (index == NEW_SUBJECT && replacedNewSubject != null) {
            return replacedNewSubject;
        }
        return super.get(index);
    }

    public String getOriginal(int index) {
        return super.get(index);
    }

    public void setReplacedSubject(String replacedSubject) {
        this.replacedSubject = replacedSubject;
    }

    public void setReplacedNewSubject(String replacedNewSubject) {
        this.replacedNewSubject = replacedNewSubject;
    }
}
