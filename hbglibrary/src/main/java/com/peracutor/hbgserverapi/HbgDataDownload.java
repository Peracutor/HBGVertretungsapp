package com.peracutor.hbgserverapi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HbgDataDownload extends HbgDownload<SortedCoverMessages> {
    public static final String HBG_URL_FORMAT = "http://vp.hbgym.de/w/%s/w000%s%d.htm";

    private final int year;

    public HbgDataDownload(int classNum, int weekNumber) {
        super(makeURL(weekNumber, classNum));
        Calendar calendar = Calendar.getInstance(Locale.GERMANY);
        this.year = weekNumber < calendar.get(Calendar.WEEK_OF_YEAR) ? calendar.get(Calendar.YEAR) + 1 : calendar.get(Calendar.YEAR);
        System.out.println("year = " + year);
    }

    public static String makeURL(int weekNumber, int classNum) {
        return String.format(Locale.GERMANY, HBG_URL_FORMAT, weekNumber < 10 ? "0" + weekNumber : weekNumber, (classNum < 10) ? "0" : "", classNum);
    }

    @Override
    protected SortedCoverMessages evaluate(String htmlText) {
        if (htmlText.contains("keine Vertretungsdaten")) {
            return null;
        }

        Document document = Jsoup.parse(htmlText);

        Elements tables = document.select("table"); //select the first table.
        if (tables.size() == 0) {
            return null;
        }

        Elements rows = tables.get(0).select("tr");
        if (rows.isEmpty()) {
            return null;
        }

        SortedCoverMessages messages = new SortedCoverMessages(rows.size());

        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            CoverMessage.Builder messageBuilder = new CoverMessage.Builder(year);

            for (int j = 0; j < cols.size(); j++) {
                String nextField = cols.get(j).text().replace('\u00A0', ' ').replace("---", "").trim();


                if (j == CoverMessage.DATE) {
                    String[] splitDate = nextField.split("\\.", 3);

                    String day;
                    if (Integer.parseInt(splitDate[0]) < 10) {
                        day = "0" + splitDate[0];
                    } else {
                        day = splitDate[0];
                    }

                    String month;
                    if (Integer.parseInt(splitDate[1]) < 10) {
                        month = "0" + splitDate[1];
                    } else {
                        month = splitDate[1];
                    }

                    String datum = day + "." + month + ".";

                    messageBuilder.setField(j, datum);

                } else if (j == CoverMessage.KIND && nextField.contains(":")) {
                    messageBuilder.setField(j, nextField.substring(0, nextField.lastIndexOf(":")));
                } else if (j == CoverMessage.ROOM) {
                    messageBuilder.setField(j, nextField.replace("???", "R?"));
                } else if (j == CoverMessage.HOUR && nextField.equals("")) {
                    messageBuilder.setField(j, "?");
                } else {
                    messageBuilder.setField(j, nextField);
                }
            }

            messages.insert(new CoverMessage(messageBuilder));
        }
        return messages;
    }

    public static Calendar getDayToNotify() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);

        if (new Date().after(calendar.getTime())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        System.out.println("dayToNotify= " + calendar.get(Calendar.DAY_OF_WEEK));
        return calendar;
    }

    //    private ArrayList<String[]> sortList(ArrayList<String[]> unsortedList) {
//        ArrayList<String[]> sortedList = new ArrayList<>(unsortedList.size());
//        for (String[] dataSet : unsortedList) {
//            boolean added = false;
//            int concernedHour;
//            if (!dataSet[HOUR].equals("")) {
//                concernedHour = getBeginningHour(dataSet[HOUR]);
//            } else {
//                dataSet[HOUR] = "?";
//                sortedList.add(dataSet);
//                continue;
//            }
//            int size = sortedList.size();
//
//            for (int i = 0; i < size; i++) {
//                int compareHour;
//                if (!sortedList.get(i)[HOUR].equals("?")) {
//                    compareHour = getBeginningHour(sortedList.get(i)[HOUR]);
//                } else {
//                    compareHour = 99;
//                }
//                if (compareHour > concernedHour) {
//                    sortedList.add(i, dataSet);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                sortedList.add(dataSet);
//            }
//        }
//        return sortedList;
//    }

//    private String retrieveStand() {
//        String file;
//        try {
//            file = downloadUrl("http://vp.hbgym.de/frames/title.htm", "retrieving stand");
//        } catch (Exception e) {
//            e.printStackTrace();
//            //returning null causes newDataAvailable()-function to return true
//            return null;
//        }
//
//
//        int start = file.indexOf("Stand: ") + 7;
//        int end = start + 16;
//
//        String dateString = file.substring(start, end);
//        System.out.println("Stand der HBG-Website: " + dateString);
//
//
//        prefs.edit().putString(LAST_TIME_STAND_RETRIEVED, PRECISE_SDF.format(new Date())).apply();
//
//        return dateString;
//    }

//    boolean minutesLater(int minutes) {
//
//        Calendar twoMinLater = Calendar.getInstance();
//        twoMinLater.setTime(lastTimeStandRetrieved);
//        twoMinLater.add(Calendar.MINUTE, minutes);
//        Date twoMinutesLater = twoMinLater.getTime();
//
//        System.out.println("Two minutes later?: " + new Date().after(twoMinutesLater));
//
//        return new Date().after(twoMinutesLater);
//    }
//
//    boolean newDataAvailable() {
//        String dateString = retrieveStand();
//        if (dateString == null) {
//            return true;
//        }
//
//        Date stand;
//        try {
//            stand = PRECISE_SDF.parse(dateString);
//        } catch (ParseException e) {
//            System.err.println("Error parsing stand");
//            e.printStackTrace();
//            return true;
//        }
//
//        boolean newDataAvailable = stand.after(lastTimeStandRetrieved);
//        System.out.println("New data available?: " + newDataAvailable);
//        return newDataAvailable;
//    }

}
