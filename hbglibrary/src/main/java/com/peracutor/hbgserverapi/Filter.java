package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 11.12.2016
 */

public interface Filter {
    boolean shouldShowMessage(CoverMessage coverMessage);
}
