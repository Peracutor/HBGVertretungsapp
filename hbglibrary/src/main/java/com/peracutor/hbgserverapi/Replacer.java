package com.peracutor.hbgserverapi;

/**
 * Created by Micha.
 * 21.06.2016
 */
public interface Replacer {
    /**
     * Replaces the CoverMessages of a SortedCoverMessages-List (i.e. with custom subject-names).
     * @param messageToReplace The message that should be replaced
     * @return Should return null if message should not be shown or should return the CoverMessage with the replaced field
     */
    CoverMessage replace(CoverMessage messageToReplace);
}
