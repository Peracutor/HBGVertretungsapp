package com.peracutor.hbgbackend;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;

/**
 * Created by Micha.
 * 14.11.2016
 */
@Entity
public class SavedAsOfDate {

    @Id
    String id;
    private long millis;
    private int weekNumber;

    @SuppressWarnings("unused")
    private SavedAsOfDate() {/*For Objectify*/}

    public SavedAsOfDate(Date date, int weekNumber, String id) {
        millis = date.getTime();
        this.weekNumber = weekNumber;
        this.id = id;
    }

    public Date getAsOfDate() {
        return new Date(millis);
    }

    public int getWeekNumber() {
        return weekNumber;
    }
}
