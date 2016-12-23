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
        boolean showSubject = whiteList.contains(coverMessage.get(CoverMessage.SUBJECT));
        boolean showNewSubject = whiteList.contains(coverMessage.get(CoverMessage.NEW_SUBJECT));

        return showSubject || showNewSubject || isSubjectInCoverText(coverMessage.get(CoverMessage.COVER_TEXT));
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
