package com.eissler.micha.hbgvertretungsapp;

import android.text.Editable;
import android.text.Html;

import org.xml.sax.XMLReader;

public class HbgHandler implements Html.TagHandler {


    private int totalRows;

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {

        if (!opening) {
            if (tag.equals("td")) {
                output.append(";;");
            } else if (tag.equals("tr")) {
                totalRows++;
            }
        }
    }

    public int getTotalRows() {
        return totalRows;
    }
}
