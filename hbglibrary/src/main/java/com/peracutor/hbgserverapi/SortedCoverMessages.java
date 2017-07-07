package com.peracutor.hbgserverapi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by Micha.
 * 02.06.2016
 */
public class SortedCoverMessages extends ArrayList<CoverMessage> {

    public SortedCoverMessages() {
        super();
    }

    public SortedCoverMessages(int initialCapacity) {
        super(initialCapacity);
    }

    public SortedCoverMessages(SortedCoverMessages messagesToClone) {
        super(messagesToClone.size());
        addAll(messagesToClone);
    }

    public SortedCoverMessages(SortedCoverMessages messagesToClone, Filter filter, Replacer replacer) {
        this(messagesToClone.size());
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
        for (CoverMessage coverMessage : messagesToClone) {
            if (!filter.shouldShowMessage(coverMessage)) {
                continue;
            }
            add(replacer.replace(coverMessage));
        }
    }

    public void insert(CoverMessage message) {

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

    public SortedCoverMessages getMessagesForWeekDay(int weekDay) {
        SortedCoverMessages dayMessages = new SortedCoverMessages();

        for (CoverMessage coverMsg : this) {
            if (weekDay == coverMsg.getConcernedDay()) {
                dayMessages.add(coverMsg);
            } else if (dayMessages.size() != 0) {
                //as list is sorted, further looping would be redundant
                break;
            }
        }

        return dayMessages;
    }

    public ArrayList<HBGMessage> getListItems() {

        ArrayList<HBGMessage> listItems = new ArrayList<>(size());

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 8);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        final SimpleDateFormat dayNameSdf = new SimpleDateFormat("EE", Locale.GERMANY);
        for (int day = Calendar.MONDAY; day <= Calendar.SATURDAY; day++) {
            SortedCoverMessages dayMessages = getMessagesForWeekDay(day);
            if (dayMessages.size() == 0) continue;

            Calendar concernedDate = Calendar.getInstance();
            concernedDate.setTime(dayMessages.get(0).getConcernedDate());

            if (today.after(concernedDate)) {
                continue;
            }

            String dateHeaderText;
            String prefix = "";

            if (concernedDate.get(Calendar.DAY_OF_WEEK) == today.get(Calendar.DAY_OF_WEEK) && concernedDate.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)) {
                prefix = "Heute, ";
            } else if (concernedDate.get(Calendar.DAY_OF_WEEK) == tomorrow.get(Calendar.DAY_OF_WEEK) && concernedDate.get(Calendar.WEEK_OF_YEAR) == tomorrow.get(Calendar.WEEK_OF_YEAR)) {
                prefix = "Morgen, ";
            }

            dateHeaderText = prefix + dayNameSdf.format(concernedDate.getTime()) + " " + dayMessages.get(0).get(CoverMessage.DATE);
            listItems.add(new HeaderMessage(dateHeaderText));

            boolean added = false;
            for (CoverMessage coverMessage : dayMessages) {
                if (new Date().after(coverMessage.getConcernedDate())) {
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

    public void filter(Filter filter) {
        for (Iterator<CoverMessage> iterator = this.iterator(); iterator.hasNext(); ) {
            CoverMessage coverMessage = iterator.next();

            if (!filter.shouldShowMessage(coverMessage)) {
                iterator.remove();
            }
        }
    }

    public void replace(Replacer replacer) {
        for (int i = 0; i < this.size(); i++) {
            CoverMessage coverMessage = this.get(i);
            set(i, replacer.replace(coverMessage));
        }
    }

    //    public String toJsonString() throws IOException {
//        StringWriter writer = new StringWriter();
//        JsonGenerator generator = new JsonFactory().createGenerator(writer);
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.writeTree(generator, asJsonNode());
//        return writer.toString();
//    }
//
//    public static SortedCoverMessages fromJsonString(String jsonString) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode messages = mapper.readTree(jsonString);
//        SortedCoverMessages sortedCoverMessages = new SortedCoverMessages(messages.size());
//        for (JsonNode node : messages) {
//            sortedCoverMessages.insert(CoverMessage.deserializer().fromJson(node));
//        }
//        return sortedCoverMessages;
//    }

//    public JsonNode asJsonNode() {
//        JsonNodeFactory factory = new JsonNodeFactory(false);
//        ArrayNode messages = factory.arrayNode();
//        for (CoverMessage coverMessage : this) {
//            messages.add(coverMessage.serializer().toJson());
//        }
//        return messages;
//    }
}
