package com.peracutor.hbgserverapi;

import java.util.List;

/**
 * Created by Micha.
 * 10.12.2016
 */

public class WhitelistFilter implements Filter {

    private final List<String> whiteList;

    public WhitelistFilter(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public boolean shouldShowMessage(CoverMessage coverMessage) {
        String subject = coverMessage.get(CoverMessage.SUBJECT);
        String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);
        return subject.equals("") && newSubject.equals("")
                || containsIgnoreCase(whiteList, subject)
                || containsIgnoreCase(whiteList, newSubject)
                || isSubjectInCoverText(coverMessage.get(CoverMessage.COVER_TEXT));
    }

    public boolean containsIgnoreCase(List<String> list, String contains) {
        for (String s : list) {
            if (contains.equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    private boolean isSubjectInCoverText(String coverText) {
        for (String listedSubject : whiteList) {
            if (coverText.contains(listedSubject)) {
                return true;
            }
        }
        return false;
    }


}
