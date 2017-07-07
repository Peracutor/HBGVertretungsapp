package com.peracutor.hbgserverapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Micha.
 * 10.12.2016
 */

public class HbgClassListDownload extends HbgDownload<List<String>> {
    private static final String HBG_NAVBAR_URL = "http://vp.hbgym.de/frames/navbar.htm";

    public HbgClassListDownload() {
        super(HBG_NAVBAR_URL);
    }

    public HbgClassListDownload(HtmlDownloadHandler downloadHandler) {
        super(HBG_NAVBAR_URL, downloadHandler);
    }

    @Override
    protected List<String> evaluate(String htmlText) throws Exception {
        int occ = htmlText.indexOf("\"05a");
        String[] seq = htmlText.substring(occ, occ + 350).split(";", 2);
        String[] classes = seq[0].split(",");

        for (int i = 0; !classes[i].contains("]") && i < classes.length - 1; i++) {
            classes[i] = classes[i].substring(1, classes[i].length() - 1);
        }
        classes[classes.length - 1] = classes[classes.length - 1].substring(1, classes[classes.length - 1].length() - 2);

        ArrayList<String> classesList = new ArrayList<>(Arrays.asList(classes));
        classesList.add(0, null); //setting 0 index to null, so that indices can be used as class-numbers
        return Collections.unmodifiableList(classesList);
    }
}
