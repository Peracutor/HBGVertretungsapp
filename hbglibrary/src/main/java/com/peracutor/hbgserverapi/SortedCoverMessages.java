package com.peracutor.hbgserverapi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 02.06.2016
 */
public class SortedCoverMessages extends ArrayList<CoverMessage> {
    private final ArrayList<String> mDays;
    private final Comparator<? super String> mDayComparator = new Comparator<String>() {
        private final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);

        @Override
        public int compare(String day1, String day2) {
            try {
                Date d1 = SHORT_SDF.parse(day1);
                Date d2 = SHORT_SDF.parse(day2);
                return d1.compareTo(d2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    };

    /*For Objectify*/
    public SortedCoverMessages() {
        super();
        mDays = new ArrayList<>();
    }

    public SortedCoverMessages(int initialCapacity) {
        super(initialCapacity);
        mDays = new ArrayList<>();
    }

    public void insert(CoverMessage message) {

        if (!mDays.contains(message.get(CoverMessage.DATE))) {
            mDays.add(message.get(CoverMessage.DATE));
            Collections.sort(mDays,  mDayComparator);
        }

        boolean added = false;
        for (int i = 0; i < size(); i++) {
            added = get(i).tryMerge(message);
            if (added) {
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

    public ArrayList<ReplacedCoverMessage> getMessagesForDay(String day, Filter filter, Replacer replacer) {
        ArrayList<ReplacedCoverMessage> dayMessages = new ArrayList<>();

        if (filter == null) {
            filter = coverMessage -> true;
        }
        if (replacer == null) {
            replacer = messageToReplace -> {
                if (messageToReplace instanceof ReplacedCoverMessage) {
                    return (ReplacedCoverMessage) messageToReplace;
                } else {
                    return new ReplacedCoverMessage(messageToReplace);
                }
            };
        }

        for (CoverMessage coverMsg : this) {
            if (day.equals(coverMsg.get(CoverMessage.DATE))) {
                if (!filter.shouldShowMessage(coverMsg)) {
                    continue;
                }
                ReplacedCoverMessage replacedCoverMessage = replacer.replace(coverMsg);
                dayMessages.add(replacedCoverMessage);
            } else if (dayMessages.size() != 0) {
                //as list is sorted, further looping would be redundant
                break;
            }
        }

        return dayMessages;
    }

    public ArrayList<String> getDays() {
        return mDays;
    }

    @Override
    public void clear() {
        mDays.clear();
        super.clear();
    }

    public ArrayList<HBGMessage> getListItems(Filter filter, Replacer replacer) {

        ArrayList<HBGMessage> listItems = new ArrayList<>(size());

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 8);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        final SimpleDateFormat dayNameSdf = new SimpleDateFormat("EE", Locale.GERMANY);
        for (String day : getDays()) {
            ArrayList<ReplacedCoverMessage> dayMessages = getMessagesForDay(day, filter, replacer);
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
            for (ReplacedCoverMessage coverMessage : dayMessages) {
                if (new Date().after(coverMessage.getConcernedDate().getTime())) {
                    continue;
                }

                added = true;
                listItems.add(coverMessage);
            }

            if (!added) {
                listItems.remove(listItems.size() - 1);
            }
        }
        return listItems;
    }

}
