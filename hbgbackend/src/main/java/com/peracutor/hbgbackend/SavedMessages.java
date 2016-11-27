package com.peracutor.hbgbackend;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import static com.peracutor.hbgbackend.OfyService.ofy;

/**
 * Created by Micha.
 * 05.06.2016
 */

@Entity
public class SavedMessages {
    @Id String id;

    SortedCoverMessages sortedCoverMessages;


    @SuppressWarnings("unused")
    private SavedMessages() {/*For Objectify*/}

    public SavedMessages(SortedCoverMessages sortedCoverMessages, int classNumber) {
        this.sortedCoverMessages = sortedCoverMessages;
        this.id = "Klasse_" + classNumber;
    }

    public SortedCoverMessages getSortedCoverMessages() {
        return sortedCoverMessages;
    }

    public static SortedCoverMessages getForClass(int classNumber) {
        SavedMessages saved = ofy().load().type(SavedMessages.class).id("Klasse_" + classNumber).now();
        SortedCoverMessages savedMessages;
        if (saved == null) {
            savedMessages = new SortedCoverMessages(0);
        } else {
            savedMessages = saved.getSortedCoverMessages();
        }
        return savedMessages;
    }

    public static void save(SortedCoverMessages sortedCoverMessages, int classNumber) {

        try {
            ofy().save().entity(new SavedMessages(sortedCoverMessages, classNumber)).now();
        } catch (Exception e) {
            System.err.println("Error saving sortedCoverMessages");
            e.printStackTrace();
        }
    }
}
