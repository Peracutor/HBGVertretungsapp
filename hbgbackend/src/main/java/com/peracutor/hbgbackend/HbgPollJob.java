package com.peracutor.hbgbackend;

import com.google.android.gcm.server.Message;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HbgAsOfDateDownload;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.peracutor.hbgbackend.OfyService.ofy;


/**
 * Created by Micha.
 * 02.06.2016
 */
public class HbgPollJob extends HttpServlet {


    public static final String AS_OF_DATE = "asOfDate";
    private Logger log = Logger.getLogger(HbgPollJob.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        final SavedAsOfDate savedAsOfDate = ofy().load().type(SavedAsOfDate.class).id(AS_OF_DATE).now();
        if (savedAsOfDate != null) {
            System.out.println("savedAsOfDate.getAsOfDate() = " + savedAsOfDate.getAsOfDate());
        }

        try {
            Date asOfDate = new HbgAsOfDateDownload().executeSync();

            System.out.println("as of = " + asOfDate);
            ofy().save().entity(new SavedAsOfDate(asOfDate, AS_OF_DATE)).now();
            boolean newDataAvailable = savedAsOfDate == null || savedAsOfDate.getAsOfDate().before(asOfDate);
            System.out.println("newDataAvailable = " + newDataAvailable);
            if (!newDataAvailable) {
                resp.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        resp.setContentType("text/html;charset=UTF-8");
        resp.getOutputStream().println("Starting<br>");



        int weekNumber = Calendar.getInstance(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);
        if (changeToNextWeek()) weekNumber++;
        log.info("weekNumber = " + weekNumber);

        final MessagingEndpoint messagingEndpoint = new MessagingEndpoint();
        for (int classNumber = 22; classNumber < 23; classNumber++) {// TODO: 15.11.2016 1
            System.out.println("classNumber = " + classNumber);
            SortedCoverMessages sortedCoverMessages;
            try {
                sortedCoverMessages = new HbgDataDownload(classNumber, weekNumber).executeSync();
            } catch (Exception e) {
                log.warning("Error occurred: " + e.getMessage());
                if (e.getCause() != null) e.getCause().printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            if (sortedCoverMessages == null) {
                log.info("No data for classnumber " + classNumber);
                return;
            }
            sortedCoverMessages = sortOutNewMessages(sortedCoverMessages, classNumber);

            for (CoverMessage coverMessage : sortedCoverMessages) {
                CoverMessageWrapper message = new CoverMessageWrapper(coverMessage);

                int timeToLive = message.getTimeToLive();
                if (timeToLive < 0) {
                    continue;
                }
                Map<String, String> data = message.toMap();
                data.put(MessageConstants.MESSAGE_ACTION, MessageConstants.ACTION_PUSH);
                Message.Builder builder = new Message.Builder()
//                                .collapseKey(message.getCollapseKey())
                        .timeToLive(timeToLive)
                        .setData(data);

                try {
                    log.info("SENDING to " + message.getTopic(classNumber));
                    messagingEndpoint.sendMessageTo(message.getTopic(classNumber), builder.build());
                } catch (IOException e1) {
                    log.info("e1.getMessage() = " + e1.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);

        resp.getOutputStream().println("Finished");

    }

    private boolean changeToNextWeek() {
        int day = Calendar.getInstance(Locale.GERMANY).get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY ||
               day == Calendar.FRIDAY && Calendar.getInstance(Locale.GERMANY).get(Calendar.HOUR_OF_DAY) >= 19;
    }

    private SortedCoverMessages sortOutNewMessages(SortedCoverMessages sortedCoverMessages, int classNumber) {
        SortedCoverMessages savedMessages = SavedMessages.getForClass(classNumber);

        if (!savedMessages.equals(sortedCoverMessages)) {
            SavedMessages.save(sortedCoverMessages, classNumber);
        }

        for (Iterator i = sortedCoverMessages.iterator(); i.hasNext(); ) {
            CoverMessage coverMessage = (CoverMessage) i.next();
            if (savedMessages.contains(coverMessage)) {
                System.out.println("Not showing: \"" + coverMessage.toString() + "\"");
                i.remove();
            }
        }

        return sortedCoverMessages;
    }

}
