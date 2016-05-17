package com.eissler.micha.hbgvertretungsapp.evaluation;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.CustomNames;
import com.eissler.micha.hbgvertretungsapp.HbgListAdapter;
import com.eissler.micha.hbgvertretungsapp.settings.WhitelistSubjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import hugo.weaving.DebugLog;

/**
 * Created by Micha Eißler.
 * 30.04.2016
 */
public class SortedCoverMessages {

    private final ArrayList<CoverMessage> mMessages;
    private final Context context;
    private ArrayList<String> mDays;

    @DebugLog
    public SortedCoverMessages(int initialCapacity, Context context) {
        this.context = context;
        mMessages = new ArrayList<>(initialCapacity);
        mDays = new ArrayList<>();
    }

    public void insert(CoverMessage message) {

        if (!mDays.contains(message.getField(CoverMessage.DATE))) {
            mDays.add(message.getField(CoverMessage.DATE));
        }

        boolean added = false;
        for (int i = 0; i < mMessages.size(); i++) {
            if (message.getConcernedDate().before(mMessages.get(i).getConcernedDate())) {
                mMessages.add(i, message);
                added = true;
                break;
            }
        }

        if (!added) {
            mMessages.add(message);
        }
    }

    public HbgListAdapter getFormattedListAdapter() {

        System.out.println("FORMAT LIST ADAPTER");

        // TODO: 30.04.2016 if there is regularly no data do not throw exception
        ArrayList<HBGMessage> listItems = new ArrayList<>(mMessages.size());

        Replacer replacer = new Replacer();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 8);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        for (String day : mDays) {
            ArrayList<CoverMessage> dayMessages = getMessagesForDay(day);
            Calendar concernedDate = dayMessages.get(0).getConcernedDate();

            if (today.after(concernedDate)) {
                System.out.println("today = " + App.PRECISE_SDF.format(today.getTime()));
                System.out.println("concernedDate = " + App.PRECISE_SDF.format(concernedDate.getTime()));
                continue;
            }


            String dateHeaderText;
            String prefix = "";

            if (concernedDate.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)) {
                if (concernedDate.get(Calendar.DAY_OF_WEEK) == today.get(Calendar.DAY_OF_WEEK)) {
                    prefix = "Heute, ";
                } else {
                    if (concernedDate.get(Calendar.DAY_OF_WEEK) == tomorrow.get(Calendar.DAY_OF_WEEK)) {
                        prefix = "Morgen, ";
                    }
                }
            }

            dateHeaderText = prefix + App.DAY_NAME_SDF.format(concernedDate.getTime()) + " " + day;
            listItems.add(new HeaderMessage(dateHeaderText));


            boolean added = false;
            for (CoverMessage coverMessage : dayMessages) {
                CoverMessage coverMessageCopy = coverMessage.copy();


                if (new Date().after(coverMessageCopy.getConcernedDate().getTime())) {
                    continue;
                }

                coverMessageCopy = replacer.replace(coverMessageCopy);

                if (coverMessageCopy == null) {
                    continue;
                }

                added = true;
                listItems.add(coverMessageCopy);
            }

            if (!added) {
                listItems.remove(listItems.size() - 1);
            }
        }

//        if (listItems.size() < 2 && !nextWeek) {
//            setWeek(true);
//            initialised = new CountDownLatch(1);
//            downloadAndEvaluate();
//            return getFormattedListAdapter();
//        } else
        if (listItems.size() < 2) {
            listItems = new ArrayList<>(1);
            listItems.add(new HeaderMessage("Es liegen keine Vertretungsdaten vor."));
        }

        return new HbgListAdapter(context, listItems);
    }

    public NotificationCompat.Builder formatMessagesForNotification() {

//        if (whySortedDataMapIsNull.get(weekNumber) != null && whySortedDataMapIsNull.get(weekNumber) != 0) {
//            return null;
//        }



        String dateToNotify = App.SHORT_SDF.format(DataEvaluation.getDayToNotify().getTime());
        ArrayList<CoverMessage> dayMessages = getMessagesForDay(dateToNotify);
        if (dayMessages == null) {
            return null;
        }

        ArrayList<String> messages = new ArrayList<>(dayMessages.size());

        Replacer replacer = new Replacer();
        for (CoverMessage coverMessage : dayMessages) {
            coverMessage = replacer.replace(coverMessage);
            if (coverMessage == null) {
                continue;
            }

            String room = coverMessage.getField(CoverMessage.ROOM).equals("") ?
                    "" : " in Raum " +
                    (coverMessage.getField(CoverMessage.ROOM).substring(0, 1).equals("R") ?
                            coverMessage.getField(CoverMessage.ROOM).substring(1) : coverMessage.getField(CoverMessage.ROOM));

            String kind = coverMessage.getField(CoverMessage.KIND).equalsIgnoreCase("anderer Raum") ?
                    "" : coverMessage.getField(CoverMessage.KIND);

            String subject = coverMessage.getField(CoverMessage.SUBJECT);

            String newSubject = (coverMessage.getField(CoverMessage.NEW_SUBJECT).equals("") || coverMessage.getField(CoverMessage.NEW_SUBJECT).equals(subject)) ?
                    "" : ": " + coverMessage.getField(CoverMessage.NEW_SUBJECT);

            String hour = coverMessage.getField(CoverMessage.HOUR).length() > 1 ?
                    coverMessage.getField(CoverMessage.HOUR) : coverMessage.getField(CoverMessage.HOUR) + ".";

            String space = subject.equals("") ? "" : " ";
            messages.add(hour + " Std. " + subject + space + kind + newSubject + room);
        }

        StringBuilder ssb = new StringBuilder();
//        char arrow = (char) 0x25B6; //→
//        ssb.append(arrow);

        if (messages.size() == 0) {
            System.out.println("No data3");
            ssb.append("Keine Vertretungsdaten");
        } else if (messages.size() == 1) {
            ssb.append(messages.get(0));
        } else if (messages.size() > 1) {
            for (String message : messages) {
                if (message == null) {
                    continue;
                }
                ssb.append(message)
                        .append("\n");
//                        .append(arrow);
            }
            ssb = ssb.delete(ssb.length() - 1, ssb.length());
        }

        return new NotificationCompat.Builder(context)
                .setContentText(ssb)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(ssb));
    }

    private ArrayList<CoverMessage> getMessagesForDay(String day) {
        ArrayList<CoverMessage> dayMessages = new ArrayList<>();

        for (CoverMessage coverMsg :
                mMessages) {
            if (day.equals(coverMsg.getField(CoverMessage.DATE))) {
                dayMessages.add(coverMsg);
            } else if (dayMessages.size() != 0) {
                break;
            }
        }

        return dayMessages;
    }


    private class Replacer {
        HashMap<String, String> customNames = null;
        ArrayList<String> whiteList = null;
        CoverMessage coverMsg;
        boolean whiteListMode = false;


        public Replacer() {

            whiteListMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("whitelist_switch", false);

            if (whiteListMode) {
                whiteList = WhitelistSubjects.getWhiteListArray(context); // TODO: 26.03.2016 use exceptions...
            }

            try {
                customNames = new CustomNames(context);
            } catch (IOException e) {
                App.reportUnexpectedException(e);
                e.printStackTrace();
            }
        }


        /**
         * Replaces the subject and newSubject field of the coverMessage with their custom names.
         * If the coverMessage should not be shown returns null
         * @param coverMessage the given CoverMessage
         * @return <li>null if in whitelistmode and neither of the subjects should be shown</li>
         * <li>null if not in whitelistmode and one of both subjects should not be shown</li>
         *  <li>otherwise returns the coverMessage with the replaced fields</li>
         */
        public CoverMessage replace(CoverMessage coverMessage) {
            if (customNames == null || (!whiteListMode && customNames.size() == 0)) {
                return coverMessage;
            }
            coverMsg = coverMessage;

            boolean subjectShouldBeShown = replaceField(CoverMessage.SUBJECT);
            boolean newSubjectShouldBeShown = replaceField(CoverMessage.NEW_SUBJECT);



            if (whiteListMode && !subjectShouldBeShown && !newSubjectShouldBeShown || !whiteListMode && (!subjectShouldBeShown || !newSubjectShouldBeShown)) {
                return null;
            }

            return coverMsg;
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

            if (whiteListMode) {
                if (!whiteList.contains(coverMsg.getField(field))) {
                    return false;
                }
            }

            String customName = customNames.get(coverMsg.getField(field));

            if (customName != null) {
                if (customName.equals("Nicht anzeigen")) {
                    return whiteListMode;
                }

                coverMsg.setField(field, customName);
            }

            return true;
        }
    }
}
