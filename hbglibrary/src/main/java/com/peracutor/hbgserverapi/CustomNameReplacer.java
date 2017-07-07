package com.peracutor.hbgserverapi;

import java.util.Map;

/**
 * Created by Micha.
 * 10.12.2016
 */

public class CustomNameReplacer implements Replacer {
    private final Map<String, String> customNames;
    private final AutoName autoName;

    public CustomNameReplacer(Map<String, String> customNames) { // TODO: 19.12.2016 add constructor with only AutoName
        this.customNames = customNames;
        autoName = null;
    }

    public CustomNameReplacer(Map<String, String> customNames, String autoNamePattern) {
        this(customNames, new AutoName(autoNamePattern));
    }

    public CustomNameReplacer(Map<String, String> customNames, AutoName autoName) {
        this.customNames = customNames;
        this.autoName = autoName;
    }

    @Override
    public ReplacedCoverMessage replace(CoverMessage messageToReplace) {
        ReplacedCoverMessage replacedCoverMessage;
        String subject;
        String newSubject;
        if (messageToReplace instanceof ReplacedCoverMessage) {
            replacedCoverMessage = (ReplacedCoverMessage) messageToReplace;
            subject = replacedCoverMessage.getOriginal(CoverMessage.SUBJECT);
            newSubject = replacedCoverMessage.getOriginal(CoverMessage.NEW_SUBJECT);
        } else {
            replacedCoverMessage = new ReplacedCoverMessage(messageToReplace);
            subject = messageToReplace.get(CoverMessage.SUBJECT);
            newSubject = messageToReplace.get(CoverMessage.NEW_SUBJECT);
        }
        replacedCoverMessage.setReplaced(CoverMessage.SUBJECT, getCustomName(subject));
        replacedCoverMessage.setReplaced(CoverMessage.NEW_SUBJECT, getCustomName(newSubject));
        return replacedCoverMessage;
    }

    private String getCustomName(String subject) {
        if (subject.equals("")) {
            return subject;
        }
        String customSubjectName = customNames.get(subject);
        if (customSubjectName == null && autoName != null) {
            customSubjectName = autoName.getAutoName(subject);
        }

        if (customSubjectName != null) {
            customSubjectName = customSubjectName.trim();
        }
        return customSubjectName;
    }
}
