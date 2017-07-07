package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 07.12.2016
 */

public class ReplacedCoverMessage extends CoverMessage {

    private String[] replacedFields = new String[8];

//    public ReplacedCoverMessage(Builder coverMessageBuilder) {
//        super(coverMessageBuilder);
//    }

    public ReplacedCoverMessage(CoverMessage coverMessage) {
        super(coverMessage);
    }

    @Override
    public String get(int field) {
        if (replacedFields[field] != null) {
            return replacedFields[field];
        }
        return super.get(field);
    }

    public String getOriginal(int index) {
        return super.get(index);
    }

    public void setReplaced(int field, String value) {
        replacedFields[field] = value;
    }

//    protected String[] getReplacedFields() {
//        return replacedFields;
//    }
}
