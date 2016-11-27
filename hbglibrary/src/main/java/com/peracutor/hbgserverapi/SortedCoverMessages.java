package com.peracutor.hbgserverapi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 02.06.2016
 */
public class SortedCoverMessages extends ArrayList<CoverMessage> {
    private final ArrayList<String> mDays;

    /*For Objectify*/
    @SuppressWarnings("unused")
    private SortedCoverMessages() {
        this(0);
    }

    public SortedCoverMessages(int initialCapacity) {
        super(initialCapacity);
        mDays = new ArrayList<>();
    }

//    public SortedCoverMessages(SortedCoverMessages sortedCoverMessages) {
//        super(sortedCoverMessages);
//        mDays = sortedCoverMessages.getDays();
//    }

    public void insert(CoverMessage message) {

        if (!mDays.contains(message.get(CoverMessage.DATE))) {
            mDays.add(message.get(CoverMessage.DATE));
        }

        boolean added = false;
        for (int i = 0; i < size(); i++) {
            added = get(i).tryMerge(message);
            if (added) {
                System.out.println("Merge successful for " + message.get(CoverMessage.SUBJECT));
                break;
            } else if (message.getConcernedDate().before(get(i).getConcernedDate())) {
                add(i, message);
                added = true;
                break;
            }
        }

        if (!added) {
            add(message);
        }
    }

    public ArrayList<CoverMessage> getMessagesForDay(String day, Replacer replacer) {
        ArrayList<CoverMessage> dayMessages = new ArrayList<>();

        for (CoverMessage coverMsg :
                this) {
            if (day.equals(coverMsg.get(CoverMessage.DATE))) {
                CoverMessage coverMsgClone = replacer.replace(coverMsg.clone());
                if (coverMsgClone == null) continue;
                dayMessages.add(coverMsgClone);
            } else if (dayMessages.size() != 0) {
                break;
            }
        }

        return dayMessages;
    }

    public int getMessageCount() {
        return size();
    }

    public ArrayList<String> getDays() {
        return mDays;
    }

    @Override
    public void clear() {
        mDays.clear();
        super.clear();
    }

    public ArrayList<HBGMessage> getListItems(Replacer replacer) {
        ArrayList<HBGMessage> listItems = new ArrayList<>(getMessageCount());

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 8);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        final SimpleDateFormat dayNameSdf = new SimpleDateFormat("EE", Locale.GERMANY);
        for (String day : getDays()) {
            ArrayList<CoverMessage> dayMessages = getMessagesForDay(day, replacer);
            if (dayMessages.size() == 0) continue;

            Calendar concernedDate = dayMessages.get(0).getConcernedDate();

            if (today.after(concernedDate)) {
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

            dateHeaderText = prefix + dayNameSdf.format(concernedDate.getTime()) + " " + day;
            listItems.add(new HeaderMessage(dateHeaderText));

            boolean added = false;
            for (CoverMessage coverMessage : dayMessages) {
                CoverMessage coverMessageCopy = coverMessage.clone();


                if (new Date().after(coverMessageCopy.getConcernedDate().getTime())) {
                    continue;
                }

//                if (replacer != null) {
//                    coverMessageCopy = replacer.replace(coverMessageCopy);
//                    if (coverMessageCopy == null) {
//                        continue;
//                    }
//                }

                added = true;
                listItems.add(coverMessageCopy);
            }

            if (!added) {
                listItems.remove(listItems.size() - 1);
            }
        }

        if (listItems.size() <= 1) {
            listItems.clear();
            listItems.add(new HeaderMessage("Keine Vertretungsdaten"));
        }
        return listItems;
    }
}
