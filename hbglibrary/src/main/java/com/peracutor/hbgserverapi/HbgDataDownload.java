package com.peracutor.hbgserverapi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Calendar;
import java.util.Locale;

public class HbgDataDownload extends HbgDownload<SortedCoverMessages> {
    private static final String HBG_URL_FORMAT = "http://vp.hbgym.de/w/%1$02d/w000%2$02d.htm";

    private final int year;

    public HbgDataDownload(int classNum, int weekNumber) {
        super(makeURL(weekNumber, classNum));
        year = getYear(weekNumber);
    }

    public HbgDataDownload(int classNum, int weekNumber, HtmlDownloadHandler downloadHandler) {
        super(makeURL(weekNumber, classNum), downloadHandler);
        year = getYear(weekNumber);
    }

    public static int getYear(int weekNumber) {
        Calendar calendar = Calendar.getInstance(Locale.GERMANY);
        return weekNumber < calendar.get(Calendar.WEEK_OF_YEAR) ? calendar.get(Calendar.YEAR) + 1 : calendar.get(Calendar.YEAR);
    }

    public static String makeURL(int weekNumber, int classNum) {
        return String.format(Locale.GERMANY, HBG_URL_FORMAT, weekNumber, classNum);
    }

    @Override
    protected SortedCoverMessages evaluate(String htmlText) throws Exception {
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
                } else if (j == CoverMessage.KIND && nextField.matches("\\d+ [a-zA-ZÄÖÜäöüß.]+")) { //bug occurring on the hbg website from time to time
                    messageBuilder.setField(j, nextField.split(" ")[1]);
                } else if (j == CoverMessage.ROOM) {
                    messageBuilder.setField(j, nextField.replace("???", "R?"));
                } else if (j == CoverMessage.LESSON && nextField.equals("")) { // TODO: 07.07.2017 spawn spaces if they are not on the web-page
                    messageBuilder.setField(j, "?");
                } else {
                    messageBuilder.setField(j, nextField);
                }
            }

            messages.insert(new CoverMessage(messageBuilder));
        }
        return messages;
    }
}
