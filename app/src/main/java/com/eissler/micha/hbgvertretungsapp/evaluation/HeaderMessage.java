package com.eissler.micha.hbgvertretungsapp.evaluation;

/**
 * Created by Micha.
 * 02.05.2016
 */
public class HeaderMessage implements HBGMessage {

    private String mHeader;

    public HeaderMessage(String headerString) {
        mHeader = headerString;
    }

    public String getHeaderString() {
        return mHeader;
    }

    @Override
    public Type getMessageType() {
        return Type.HEADER_MESSAGE;
    }
}
