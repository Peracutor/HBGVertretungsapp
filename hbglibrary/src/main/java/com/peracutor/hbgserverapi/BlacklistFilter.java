package com.peracutor.hbgserverapi;

import java.util.List;

/**
 * Created by Micha.
 * 11.12.2016
 */

public class BlacklistFilter implements Filter {

    private List<String> blackList;

    public BlacklistFilter(List<String> blackList) {
        this.blackList = blackList;
    }

    @Override
    public boolean shouldShowMessage(CoverMessage coverMessage) {
        if (blackList.size() == 0) return true;
        
        String subject = coverMessage.get(CoverMessage.SUBJECT);
        String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);
        boolean shouldNotShowMessage = blackList.contains(subject) || subject.equals("") && blackList.contains(newSubject);
        return !shouldNotShowMessage;
    }
}
