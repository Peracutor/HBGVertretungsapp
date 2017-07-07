package com.peracutor.hbgbackend;

import com.eissler.micha.cloudmessaginglibrary.SubmittedNotification;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Random;

/**
 * Created by Micha.
 * 06.07.2017
 */

@Entity
public class SavedSubmittedNotification {

    @Id
    Long id;

    private SubmittedNotification notification;

    private SavedSubmittedNotification() {}

    public SavedSubmittedNotification(SubmittedNotification notification) {
        this.notification = notification;
        id = new Random().nextLong();
    }

    public static Long save(SubmittedNotification notification) {
        SavedSubmittedNotification savedNotification = new SavedSubmittedNotification(notification);
        OfyService.ofy().save().entity(savedNotification).now();
        return savedNotification.getId();
    }

    public static SubmittedNotification load(Long key) {
        SavedSubmittedNotification savedSubmittedNotification = OfyService.ofy().load().type(SavedSubmittedNotification.class).id(key).now();
        return savedSubmittedNotification == null ? null : savedSubmittedNotification.getNotification();
    }

    public static void delete(Long id) {
        OfyService.ofy().delete().type(SavedSubmittedNotification.class).id(id).now();
    }

    public SubmittedNotification getNotification() {
        return notification;
    }

    public Long getId() {
        return id;
    }
}
