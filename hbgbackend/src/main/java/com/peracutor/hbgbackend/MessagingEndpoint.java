/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Backend with Google Cloud Messaging" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/GcmEndpoints
*/

package com.peracutor.hbgbackend;

import com.eissler.micha.cloudmessaginglibrary.InfoNotification;
import com.eissler.micha.cloudmessaginglibrary.Recipients;
import com.eissler.micha.cloudmessaginglibrary.SubmittedNotification;
import com.eissler.micha.cloudmessaginglibrary.TimeToLive;
import com.eissler.micha.cloudmessaginglibrary.UpdateAvailableNotification;
import com.eissler.micha.cloudmessaginglibrary.VersionInfo;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.apphosting.api.ApiProxy;
import com.google.common.io.CharStreams;
import com.googlecode.objectify.cmd.QueryExecute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static com.peracutor.hbgbackend.OfyService.ofy;

/**
 * An endpoint to send messages to devices registered with the backend
 *
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 *
 * NOTE: This endpoint does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Api(
  name = "messaging",
  version = "v1",
  namespace = @ApiNamespace(
    ownerDomain = "hbgbackend.peracutor.com",
    ownerName = "hbgbackend.peracutor.com"
  )
)
public class MessagingEndpoint {
    private static final Logger log = Logger.getLogger(MessagingEndpoint.class.getName());

    /** Api Keys can be obtained from the google cloud console */
    static final String API_KEY = System.getProperty("gcm.api.key");

    /**
     * Send to the first 10 devices (You can modify this to send to any number of devices or a specific device)
     *
     *  message The message to send
     */
//    public void sendMessage(@Named("message") String message) throws IOException {
//        if(message == null || message.trim().length() == 0) {
//            log.warning("Not sending message because it is empty");
//            return;
//        }
//        // crop longer messages
//        if (message.length() > 1000) {
//            message = message.substring(0, 1000) + "[...]";
//        }
//        com.google.android.gcm.server.Sender sender = new com.google.android.gcm.server.Sender(API_KEY);
//        Message msg = new Message.Builder().addData("message", message).build();
//        List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).limit(10).list();
//        log.info("records = " + records);
//        for(RegistrationRecord record : records) {
//            Result result = sender.send(msg, record.getRegId(), 5);
//            if (result.getMessageId() != null) {
//                log.info("Message sent to " + record.getRegId());
//                String canonicalRegId = result.getCanonicalRegistrationId();
//                if (canonicalRegId != null) {
//                    // if the regId changed, we have to update the datastore
//                    log.info("Registration Id changed for " + record.getRegId() + " updating to " + canonicalRegId);
//                    record.setRegId(canonicalRegId);
//                    ofy().save().entity(record).now();
//                }
//            } else {
//                String error = result.getErrorCodeName();
//                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
//                    log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
//                    // if the device is no longer registered with Gcm, remove it from the datastore
//                    ofy().delete().entity(record).now();
//                }
//                else {
//                    log.warning("Error when sending message : " + error);
//                }
//            }
//        }
//    }

//    public CollectionResponse<Byte> test(@Named("string") String string) throws IOException {
//    }

//    public static void sendMessageTo(String to, Message message) throws IOException {
//        boolean matches = to.matches("/topics/[a-zA-Z0-9-_.~%]+");
//        log.info("sending to topic? " + matches);
//
//        Result result = new Sender(API_KEY).sendMessage(message, to, 3);
//        log.info("result = " + result.toString());
//        log.info("result.getErrorCodeName() = " + result.getErrorCodeName());

//        com.eissler.micha.backend_library.Result send = new Sender(API_KEY).sendMessage(message, "eRhKUU_jm5o:APA91bHjNYuFl8ZmLm8EGTEFblRG4Mm64GukxA-q2OMJ83NForOZ7oomNBRZnDdFcyw7MzgUG_19fUaj-vTt6nYcGBIIvAiZ-GrZ8ukpaLewTBGLsjwZOJrHzgwvMKFHCR-yDNYAg6vi", 1);
//        log.info("send = " + send);


//        if (result.getMessageId() != null) {
//            log.info("Message sent to "  + record.getRegId());
//            String canonicalRegId = result.getCanonicalRegistrationId();
//            if (canonicalRegId != null) {
//                // if the regId changed, we have to update the datastore
//                log.info("Registration Id changed for " + record.getRegId() + " updating to " + canonicalRegId);
//                record.setRegId(canonicalRegId);
//                ofy().save().entity(record).now();
//            }
//        } else {
//            String error = result.getErrorCodeName();
//            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
//                log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
//                // if the device is no longer registered with Gcm, remove it from the datastore
//                ofy().delete().entity(record).now();
//            }
//            else {
//                log.warning("Error when sending message : " + error);
//            }
//        }
//    }


    public CollectionResponse<SubmittedNotification> deleteSubmittedMessages(@com.google.api.server.spi.config.Nullable @Named("count") Integer count) {
        QueryExecute<SavedSubmittedNotification> loadType;
        if (count != null && count > 0) {
            loadType = ofy().load().type(SavedSubmittedNotification.class).limit(count);
        } else {
            loadType = ofy().load().type(SavedSubmittedNotification.class);
        }
        List<SavedSubmittedNotification> savedSubmittedNotifications = loadType.list();
        ArrayList<SubmittedNotification> notifications = new ArrayList<>(savedSubmittedNotifications.size());
        log.info("Following submitted messages are currently pending:");

        for (SavedSubmittedNotification savedNotification : savedSubmittedNotifications) {
            SubmittedNotification notification = savedNotification.getNotification();
            notifications.add(notification);
            log.info("    " + notification.getTitle());
        }
        return CollectionResponse.<SubmittedNotification>builder().setItems(notifications).build();
    }

    @ApiMethod
    public void submitNotification(SubmittedNotification notification) throws MessagingException, IOException {
        Long key = SavedSubmittedNotification.save(notification);

        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage adminMessage = new MimeMessage(session);
        adminMessage.setFrom(new InternetAddress("arkanseidos@gmail.com", "HBG-Vertretungsapp"));
        adminMessage.setSubject("HBG-Vertretungsapp: Neue Nachricht eingereicht");
        adminMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress("admins"));

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("arkanseidos@gmail.com", "HBG-Vertretungsapp"));
        message.setSubject("HBG-App: Neue Nachricht eingereicht");
        message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress("m.eissler@hotmail.de"));
        String filepath = notification.getImageUrl() != null ? "email_with_image.html" : "email_no_image.html";
        String emailHtml = String.format(getFromFile(filepath),
                notification.getTitle(),
                notification.getContent(),
                key,
                notification.getImageUrl());

        message.setContent(emailHtml, "text/html; charset=utf-8");

        String adminMessageText = String.format(getFromFile("email_admin_message.txt"),
                notification.getTitle(),
                notification.getContent(),
                key,
                notification.getImageUrl() != null ? "Bild-URL: " +  notification.getImageUrl() : "");

        try {
//            Transport.send(message);// TODO: 29.06.2017 change recipient mail-adress and remove the line below
            adminMessageText += "\n\n\nMail wurde nur an Admin geschickt.";
        } catch (ApiProxy.OverQuotaException e) {
            e.printStackTrace();
            adminMessageText += "\n\n\nACHTUNG: Quota-Limit erreicht, Mail wurde nur an Admin geschickt!";
        }

        adminMessage.setText(adminMessageText, "utf-8");
        Transport.send(adminMessage);
    }

    public static String getFromFile(String pathname) throws IOException {
        return CharStreams.toString(new InputStreamReader(new FileInputStream(new File("WEB-INF/" + pathname)), "utf-8"));
    }

    public void sendNotification(@Nullable @Named("to") String to, @Named("title") String title, @Named("body") String body, @Nullable @Named("imageUrl") String imageUrl, @Nullable @Named("endDate") String endDate) throws IOException, ParseException {
        InfoNotification notification = new InfoNotification.Builder()
                .setTitle(title)
                .setContent(body)
                .setImageUrl(imageUrl)
                .build();

        Recipients recipients = new Recipients().to(to);
        TimeToLive timeToLive;
        if (endDate != null) timeToLive = new TimeToLive(endDate, "dd.MM.yy HH:mm");
        else timeToLive = null;

        notification.send(recipients, timeToLive, API_KEY);
//        Map<String, String> notification = bundleNotification(to, condition, title, body, imageUrl, endDate);
//        sendNotification(notification);
    }

//    private String convertToUTF8(String s) {
//        return new String(s.getBytes(), Charset.forName("US-ASCII"));
//    }
//


    public void sendTestPush(@Named("token") String token) throws IOException {
        log.info("MessagingEndpoint.sendTestPush");

        Calendar calendar = Calendar.getInstance(Locale.GERMANY);
        calendar.add(Calendar.SECOND, 30);

        InfoNotification notification = new InfoNotification.Builder()
                .setTitle("Testbenachrichtigung")
                .setContent("Das ist eine Testnachricht")
                .build();

        notification.send(new Recipients().to(token), new TimeToLive(calendar.getTime()), API_KEY);
    }

    @ApiMethod
    public void sendUpdateAvailablePush() throws IOException {
        VersionInfo versionInfo = new RegistrationEndpoint().getVersionInfo();
        new UpdateAvailableNotification(versionInfo).send(API_KEY);
    }
}
