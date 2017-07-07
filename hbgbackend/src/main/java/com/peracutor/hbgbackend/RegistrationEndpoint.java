/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Backend with Google Cloud Messaging" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/GcmEndpoints
*/

package com.peracutor.hbgbackend;

import com.eissler.micha.cloudmessaginglibrary.Recipients;
import com.eissler.micha.cloudmessaginglibrary.RegConfirmNotification;
import com.eissler.micha.cloudmessaginglibrary.VersionInfo;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.googlecode.objectify.cmd.QueryExecute;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.peracutor.hbgbackend.OfyService.ofy;

/**
 * A registration endpoint class we are exposing for a device's GCM registration id on the backend
 * <p>
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 * <p>
 * NOTE: This endpoint does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Api(
        name = "registration",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "hbgbackend.peracutor.com",
                ownerName = "hbgbackend.peracutor.com",
                packagePath = ""
        )
)
public class RegistrationEndpoint {

    private static final Logger log = Logger.getLogger(RegistrationEndpoint.class.getName());

    /**
     * Register a device to the backend
     *
     * @param regId The Google Cloud Messaging registration Id to add
     */
    @ApiMethod(name = "registerDevice")
    public void registerDevice(@Named("registrationId") String regId, @Named("acraId") String acraId) throws IOException {
        log.info("Register request came in");
        if (findRecord(regId) != null) {
            log.info("Device " + regId + " already registered, skipping register");
            sendConfirmation(regId);
            return;
        } else {
            RegistrationRecord record = findRecordByAcraId(acraId);
            if (record != null) {
                ofy().delete().entity(record).now();
            }
        }
        RegistrationRecord record = new RegistrationRecord();
        record.setRegId(regId);
        record.setAcraId(acraId);
        ofy().save().entity(record).now();

        sendConfirmation(regId);

        log.info("Registriert: " + regId);
    }

    private void sendConfirmation(String regId) throws IOException {
        log.info("RegistrationEndpoint.sendConfirmation");
        new RegConfirmNotification().send(new Recipients().id(regId), MessagingEndpoint.API_KEY);
    }

    /**
     * Unregister a device from the backend
     *
     * @param regId The Google Cloud Messaging registration Id to remove
     */
    @ApiMethod(name = "unregister")
    public void unregisterDevice(@Named("regId") String regId) {
        RegistrationRecord record = findRecord(regId);
        if (record == null) {
            log.info("Device " + regId + " not registered, skipping unregister");
            return;
        }
        ofy().delete().entity(record).now();
    }

    public RegistrationRecord findRecordByAcraId(@Named("acraId") String acraId) {
        return ofy().load().type(RegistrationRecord.class).filter("acraId", acraId).first().now();
    }

    /**
     * Return a collection of registered devices
     *
     * @param count The number of devices to list
     * @return a list of Google Cloud Messaging registration Ids
     */
    @ApiMethod(name = "listDevices")
    public CollectionResponse<RegistrationRecord> listDevices(@Nullable @Named("count") Integer count) {
        QueryExecute<RegistrationRecord> loadType;
        if (count != null && count > 0) {
            loadType = ofy().load().type(RegistrationRecord.class).limit(count);
        } else {
            loadType = ofy().load().type(RegistrationRecord.class);
        }
        List<RegistrationRecord> records = loadType.list();
        CollectionResponse<RegistrationRecord> collectionResponse = CollectionResponse.<RegistrationRecord>builder().setItems(records).build();
        log.info("RegIds:");
        for (RegistrationRecord r :
                collectionResponse.getItems()) {
            log.info("    " + r.getRegId());
        }
        return collectionResponse;
    }

    @ApiMethod(name = "showSavedSubjects")
    public CollectionResponse<String> showSavedSubjects(@Named("classNumber") int classNumber) {
        SortedCoverMessages sortedCoverMessages = SavedMessages.load(classNumber);
        ArrayList<String> items = new ArrayList<>(sortedCoverMessages.size());
        for (CoverMessage coverMessage : sortedCoverMessages) {
            items.add(coverMessage.toString());
        }
        return new CollectionResponse.Builder<String>().setItems(items).build();
    }

    private RegistrationRecord findRecord(String regId) {
        return ofy().load().type(RegistrationRecord.class).filter("regId", regId).first().now();
    }


    @ApiMethod(name = "getVersionInfo")
    public VersionInfo getVersionInfo() {
        return new VersionInfo() {
            @Override
            public Integer getVersionNumber() {
                return 8;
            }

            @Override
            public String getVersionName() {
                return "2.1.1";
            }

            @Override
            public String getVersionDescription() { //http://unicode-table.com
                return "Was neu ist in v2.1.1:\n" +
                        "- Bugfix\n" +
                        "seit v2.1.0:" +
                        "- neues Design\n" +
                        "- Fachk\u00fcrzel automatisch ausschreiben lassen\n" +
                        "- Echtzeitbenachrichtigungen\n" +
                        "- Teilenfunktion verbessert\n" +
                        "- Wochennachricht\n" +
                        "- anpassbare Benachrichtigungen\n" +
                        "- mehr Einstellungsoptionen\n" +
                        "- Bugfixes";


//                        "- Design-Anpassung an das der Schule (rot)\n" +
//                        "- Drawer-Menu f\u00fcr die Klassenauswahl (wird Dropdown-Auswahl im n\u00e4chsten Update ersetzen)\n" +
//                        "- Bugfixes (App st\u00fcrzt seltener ab)";
            }

            @Override
            public String getApkUrl() {
                return "https://www.dropbox.com/s/d86yiqhqr75az6j/hbg-vertretungsapp-2_1_1.apk?dl=1";
            }

        };
    }

}
