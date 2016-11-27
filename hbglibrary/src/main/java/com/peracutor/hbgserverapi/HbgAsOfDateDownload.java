package com.peracutor.hbgserverapi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Micha.
 * 14.11.2016
 */

public class HbgAsOfDateDownload extends HbgDownload<Date> {

    public static final String HBG_HEADER_URL = "http://vp.hbgym.de/frames/title.htm";
    public static final String AS_OF_STRING = "Stand: ";

    public HbgAsOfDateDownload() {
        super(HBG_HEADER_URL);
    }


    @Override
    protected Date evaluate(String htmlText) {
        Document document = Jsoup.parse(htmlText);
        String infoText = document.select("table").select("tr").select("td").get(1).select("table").select("tr").select("td").select("span").get(2).text();
        String asOfDateString = infoText.substring(infoText.indexOf(AS_OF_STRING) + AS_OF_STRING.length());
        try {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).parse(asOfDateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
