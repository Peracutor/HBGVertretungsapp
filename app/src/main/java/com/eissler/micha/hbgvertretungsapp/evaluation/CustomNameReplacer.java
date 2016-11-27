package com.eissler.micha.hbgvertretungsapp.evaluation;


import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.Replacer;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomNameReplacer implements Replacer {
    HashMap<String, String> customNames = null;
    ArrayList<String> whiteList = null;
    CoverMessage coverMsg;
    boolean whiteListMode = false;


    public CustomNameReplacer(Context context) {

        whiteListMode = Whitelist.isWhitelistModeActive(context);
        System.out.println("whiteListMode = " + whiteListMode);

        if (whiteListMode) {
            whiteList = Whitelist.get(context);
        }

        customNames = CustomNames.get(context);
    }


    /**
     * Replaces the subject and newSubject field of the coverMessage with their custom names.
     * If the coverMessage should not be shown returns null
     * @param coverMessage the given CoverMessage
     * @return <li>null if in whitelistmode and neither of the subjects should be shown</li>
     * <li>null if not in whitelistmode and one of both subjects should not be shown</li>
     *  <li>otherwise returns the coverMessage with the replaced fields</li>
     */
    @Override
    public CoverMessage replace(CoverMessage coverMessage) {
        if (!whiteListMode && customNames.size() == 0) {
            return coverMessage;
        }
        coverMsg = coverMessage;

        String subject = coverMessage.get(CoverMessage.SUBJECT);
        String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);

        if (subject.equals("") && newSubject.equals("")) {
            return coverMessage;
        }

        boolean subjectShouldBeShown = replaceField(CoverMessage.SUBJECT);
        boolean newSubjectShouldBeShown = replaceField(CoverMessage.NEW_SUBJECT);


        if (whiteListMode && !subjectShouldBeShown && !newSubjectShouldBeShown && !isSubjectInCoverText(coverMessage.get(CoverMessage.COVER_TEXT))) {
            return null;
        } else if (!whiteListMode && !subjectShouldBeShown) {
            return null;
        } else if (!whiteListMode && (subject.equals("")) && !newSubjectShouldBeShown) {
            return null;
        } else {
            return coverMsg;
        }
    }

    /**
     * Replaces the field with the saved custom name
     *
     * @param field the field to replace
     * @return true if subject should be shown (regarding if in whitelistMode or not)
     */
    private boolean replaceField(int field) {
        if (coverMsg == null) {
            return false;
        }

        String subject = coverMsg.get(field);

        if (whiteListMode && !whiteList.contains(subject)) {
            return false;
        }

        String customName = customNames.get(subject);

        if (customName != null) {
            if (customName.equals("Nicht anzeigen")) {
                return whiteListMode;
            }

            coverMsg.set(field, customName);
        }

        return true;
    }

    private boolean isSubjectInCoverText(String coverText) {
        for (String listedSubject : whiteList) {
            if (coverText.contains(listedSubject)) {
                System.out.println("subjectInCoverText = true");
                return true;
            }
        }
        return false;
    }
}
