package com.peracutor.hbgbackend;

import com.eissler.micha.cloudmessaginglibrary.InfoNotification;
import com.eissler.micha.cloudmessaginglibrary.Recipients;
import com.eissler.micha.cloudmessaginglibrary.SubmittedNotification;

import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.peracutor.hbgbackend.MessagingEndpoint.API_KEY;

/**
 * Created by Micha.
 * 29.05.2017
 */

public class ConfirmNotificationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyString = req.getParameter("key");
        if (keyString == null) {
            resp.getOutputStream().println("Fehlerhafter Aufruf: Kein Benachrichtigungsschlüssel wurde übertragen");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Long key;
        try {
            key = Long.valueOf(keyString);

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean delete = false;
        if (req.getParameter("delete") != null) {
            delete = Boolean.parseBoolean(req.getParameter("delete"));
        }

        SubmittedNotification notification = SavedSubmittedNotification.load(key);
        if (notification == null) {
            resp.getOutputStream().println("Der Link, der die Benachrichtigung bestätigen soll, ist nicht mehr aktuell.<p>Die Benachrichtigung wurde entweder gelöscht, oder schon bestätigt und gesendet.");
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        SavedSubmittedNotification.delete(key);

        if (delete) {
//            Map<String, String> data = notification.getData();

//            if (data.get("sender") != null) {
//                new MessagingEndpoint().sendNotification(data.get("sender"), "Nicht genehmigt", "Die Nachricht \"" + notification.getData().get("title") + "\" wurde nicht genehmigt.", null, null);
//            }

            InfoNotification infoNotification = new InfoNotification.Builder()
                    .setTitle("Nicht genehmigt")
                    .setContent("Die Nachricht \"" + notification.getTitle() + "\" wurde nicht genehmigt.")
                    .build();
            infoNotification.send(new Recipients().to(notification.getSenderToken()), API_KEY);

            resp.getOutputStream().println("Benachrichtigung erfolgreich gelöscht.");
            resp.setStatus(HttpServletResponse.SC_OK);
            return;

        }

        try {

//            if (data.get("sender") != null) {
//                new MessagingEndpoint().sendNotification(data.get("sender"), "Nachricht genehmigt", "Die Nachricht \"" + data.get("title") + "\" wurde genehmigt.", null, null);
//                data.remove("sender");
//            }

//            notification.send(notification.getRecipients(), API_KEY);
            notification.send(new Recipients().id("criXnLMVous:APA91bEn7lrSEZbHLeU7Q5otvPZf8Xa7tjPRKnvss4GGJA9QJ6S0N_-cSBgsdrvwWmsLdXq67mTQro23RJxVQDz7JkgytaVyrURsAokOT79Lih8lfUJmIk6HZaJ3zc2BefFvr8-eeCw1"),
                    notification.getTimeToLive(), API_KEY); // TODO: 01.06.2017 remove this line and uncomment above line

            InfoNotification infoNotification = new InfoNotification.Builder()
                    .setTitle("Nachricht genehmigt")
                    .setContent("Die Nachricht \"" + notification.getTitle() + "\" wurde genehmigt.")
                    .build();
            infoNotification.send(new Recipients().to(notification.getSenderToken()), API_KEY);
//                timeToLive = getTimeToLive(data.get("endDate"));

            resp.getOutputStream().println("Benachrichtigung erfolgreich bestätigt und gesendet.");
            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (IllegalArgumentException ignored) {
//                new MessagingEndpoint().sendNotification(data.get("sender"), "Nachricht abgelaufen", "Die Nachricht \"" + data.get("title") + "\" wurde zu spät genehmigt.", null, null);
            InfoNotification infoNotification = new InfoNotification.Builder()
                    .setTitle("Nachricht abgelaufen")
                    .setContent("Die Nachricht \"" + notification.getTitle() + "\" wurde zu spät genehmigt.")
                    .build();
            infoNotification.send(new Recipients().to(notification.getSenderToken()), API_KEY);
            resp.getOutputStream().println("Die Nachricht ist nicht mehr gültig. Das angegebene Gültigkeitsdatum war: " + SimpleDateFormat.getInstance().format(notification.getTimeToLive().getEndDate()));
            resp.setStatus(HttpServletResponse.SC_OK);
        }

//            data.put("to", "criXnLMVous:APA91bEn7lrSEZbHLeU7Q5otvPZf8Xa7tjPRKnvss4GGJA9QJ6S0N_-cSBgsdrvwWmsLdXq67mTQro23RJxVQDz7JkgytaVyrURsAokOT79Lih8lfUJmIk6HZaJ3zc2BefFvr8-eeCw1");
//            MessagingEndpoint.sendNotification(data);

    }
}
