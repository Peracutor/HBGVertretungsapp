package com.eissler.micha.hbgvertretungsapp.evaluation;

/**
 * Created by Micha.
 * 02.05.2016
 */
public interface HBGMessage {

    enum Type {
        COVER_MESSAGE,
        HEADER_MESSAGE
    }

    Type getMessageType();
}
