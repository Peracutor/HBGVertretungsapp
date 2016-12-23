package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 21.06.2016
 */
public interface Replacer {
    ReplacedCoverMessage replace(CoverMessage messageToReplace);
}
