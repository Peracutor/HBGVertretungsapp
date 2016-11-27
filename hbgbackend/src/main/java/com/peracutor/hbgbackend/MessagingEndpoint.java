/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Backend with Google Cloud Messaging" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/GcmEndpoints
*/

package com.peracutor.hbgbackend;

import com.google.android.gcm.server.Message;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

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
    private static final String API_KEY = System.getProperty("gcm.api.key");

    /**
     * Send to the first 10 devices (You can modify this to send to any number of devices or a specific device)
     *
     * @param message The message to send
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

    public void sendMessageTo(@Named("to") String to, Message message) throws IOException {
        boolean matches = to.matches("/topics/[a-zA-Z0-9-_.~%]+");
        log.info("sending to topic? " + matches);

        com.peracutor.hbgbackend.Result result = new Sender(API_KEY).sendMessage(message, to, 3);
        log.info("result = " + result.toString());
        log.info("result.getErrorCodeName() = " + result.getErrorCodeName());

//        com.peracutor.hbgbackend.Result send = new Sender(API_KEY).sendMessage(message, "eRhKUU_jm5o:APA91bHjNYuFl8ZmLm8EGTEFblRG4Mm64GukxA-q2OMJ83NForOZ7oomNBRZnDdFcyw7MzgUG_19fUaj-vTt6nYcGBIIvAiZ-GrZ8ukpaLewTBGLsjwZOJrHzgwvMKFHCR-yDNYAg6vi", 1);
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
    }


    public void sendNotification(@Named("to") String to, @Named("titleNoUmlauts") String title, @Named("bodyNoUmlauts") String body, @Nullable @Named("imageUrl") String imageUrl, @Nullable @Named("endDate") String endDate/*, @Nullable @Named("collapseKey") String collapseKey*/) throws IOException, ParseException {
        Map<String, String> notificationData = new HashMap<>(4);
        System.out.println("Charset.defaultCharset() = " + Charset.defaultCharset());
//        title = replaceUmlauts(title);
//        body = replaceUmlauts(body);

        notificationData.put("title", title);
        notificationData.put("body", body);
        System.out.println("title = " + title);
        System.out.println("body = " + body);
        if (imageUrl != null) notificationData.put("imageUrl", imageUrl);

        notificationData.put(MessageConstants.MESSAGE_ACTION, MessageConstants.ACTION_INFO_NOTIFICATION);

        Message.Builder message = new Message.Builder()
                .setData(notificationData);

        if (endDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
            Date date = dateFormat.parse(endDate);

            int timeToLive = (int) ((date.getTime() - Calendar.getInstance().getTimeInMillis()) / 60);
            if (timeToLive < 0) {
                log.warning("endDate is in the past, not sending message");
                return;
            }
            message.timeToLive(timeToLive);
        }

        sendMessageTo(to, message.build());

    }

//    private String convertToUTF8(String s) {
//        return new String(s.getBytes(), Charset.forName("US-ASCII"));
//    }
//
//    private static String replaceUmlauts(String s) {
//        return s.replace("ae", "ä").replace("ue", "ü").replace("oe", "ö")
//                .replace("Ae", "Ä").replace("Ue", "Ü").replace("Oe", "Ö")
//                .replace("s_z", "ß");
//    }

    public void sendTestPush(@Named("token") String token, @Named("acraID") String acraID) {
        log.info("MessagingEndpoint.sendTestPush");

        new RegistrationEndpoint().registerDevice(token, acraID);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 30);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);

        try {
            sendNotification(token, "Testbenachrichtigung", "Das ist eine Testnachricht", null, dateFormat.format(new Date(calendar.getTimeInMillis())));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    @ApiMethod
    public void sendUpdateAvailablePush() {
        System.out.println("MessagingEndpoint.sendUpdateAvailablePush");
        RegistrationEndpoint.VersionInfo versionInfo = new RegistrationEndpoint().getVersionInfo();
        Message.Builder builder = new Message.Builder()
                .addData(MessageConstants.MESSAGE_ACTION, MessageConstants.ACTION_UPDATE_AVAILABLE)
                .addData("versionName", versionInfo.getVersionName())
                .addData("versionNumber", String.valueOf(versionInfo.getVersionNumber()))
                .addData("apkUrl", versionInfo.getApkUrl());
        try {
            sendMessageTo("/topics/global", builder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    public void sendMessageToAcraId(@Named("acraId") String acraId, @Named("message") String message) {
//        RegistrationRecord record = findRecordByAcraId(acraId);
//        if (record == null) {
//            log.warning("Not sending message - no record for acraId: " + acraId);
//            return;
//        }
//
//        sendMessageTo(new Message.Builder().addData(MessageConstants.MESSAGE_ACTION, MessageConstants.ACTION_INFO_NOTIFICATION)record.getRegId());
//
//    }
}
