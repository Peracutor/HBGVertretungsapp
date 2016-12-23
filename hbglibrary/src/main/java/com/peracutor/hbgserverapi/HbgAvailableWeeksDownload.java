package com.peracutor.hbgserverapi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Micha.
 * 14.11.2016
 */

public class HbgAvailableWeeksDownload extends HbgDownload<ArrayList<Integer>> {

    public static final String HBG_NAVBAR_URL = "http://vp.hbgym.de/frames/navbar.htm";

    public HbgAvailableWeeksDownload() {
        super(HBG_NAVBAR_URL);
    }

    public HbgAvailableWeeksDownload(HtmlDownloadHandler downloadHandler) {
        super(HBG_NAVBAR_URL, downloadHandler);
    }

    @Override
    protected ArrayList<Integer> evaluate(String htmlText) {
        Elements options = Jsoup.parse(htmlText).select("select").get(0).select("option");


        if (options == null || options.size() == 0) {
            return new ArrayList<>();
        }

        ArrayList<Integer> availableWeeks;
        availableWeeks = new ArrayList<>(options.size());

        final Calendar calendar = Calendar.getInstance();
        final int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
        final int dayInWeek = calendar.get(Calendar.DAY_OF_WEEK);
        for (Element weekOption : options) {
            int weekNumber = Integer.parseInt(weekOption.attributes().get("value"));

            if (weekNumber < currentWeek) {
                continue;
            } else if (weekNumber == currentWeek) {
                boolean isWeekEnd = dayInWeek == Calendar.SATURDAY || dayInWeek == Calendar.SUNDAY;
                if (isWeekEnd) {
                    continue;
                }
            }
            availableWeeks.add(weekNumber);
        }
        return availableWeeks;
    }
}
