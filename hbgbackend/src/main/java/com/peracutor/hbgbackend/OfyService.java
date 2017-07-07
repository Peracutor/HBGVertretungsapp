package com.peracutor.hbgbackend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

/**
 * Objectify service wrapper so we can statically register our persistence classes
 * More on Objectify here : https://code.google.com/p/objectify-appengine/
 *
 */
public class OfyService {

    static {
        ObjectifyService.register(RegistrationRecord.class);
        ObjectifyService.register(SavedMessages.class);
        ObjectifyService.register(SavedAsOfDate.class);
        ObjectifyService.register(SavedSubmittedNotification.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

//    public static ObjectifyFactory factory() {
//        return ObjectifyService.factory();
//    }
}
