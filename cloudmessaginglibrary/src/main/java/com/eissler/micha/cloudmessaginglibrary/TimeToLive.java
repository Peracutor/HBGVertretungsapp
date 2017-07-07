package com.eissler.micha.cloudmessaginglibrary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Micha.
 * 06.07.2017
 */

public class TimeToLive {


    private Integer timeToLive;
    private Date endDate;

    private TimeToLive() {}

    public TimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    public TimeToLive(Date endDate) {
        this.endDate = endDate;
    }

    public TimeToLive(String endDate, String dateFormatPattern) throws ParseException {
        this.endDate = new SimpleDateFormat(dateFormatPattern).parse(endDate);
    }


    public Date getEndDate() {
        if (endDate != null) {
            return endDate;
        }

        return new Date(new Date().getTime() + timeToLive * 1000);
    }

    public Integer getTimeToLive() {
        if (timeToLive != null) {
            return timeToLive;
        }

        if (endDate == null) {
            return null;
        }

        int timeToLive = (int) ((endDate.getTime() - new Date().getTime()) / 1000);
        if (timeToLive >= 2419200) {
            return null;
        }
        return timeToLive;
    }
}
