package com.peracutor.hbgbackend;

import com.eissler.micha.cloudmessaginglibrary.PushNotification;
import com.eissler.micha.cloudmessaginglibrary.Recipients;
import com.eissler.micha.cloudmessaginglibrary.WeekChangeNotification;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HbgAsOfDateDownload;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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


    private static final String AS_OF_DATE = "asOfDate";
    private final Logger log = Logger.getLogger(HbgPollJob.class.getName());
    private boolean weekChanged;


    @Override
    protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        int weekNumber = getWeekNumber();
        log.info("weekNumber = " + weekNumber);

        try {
            final SavedAsOfDate savedAsOfDate = ofy().load().type(SavedAsOfDate.class).id(AS_OF_DATE).now();
            Date asOfDate = new HbgAsOfDateDownload().executeSync();

            System.out.println("as of = " + asOfDate);
            ofy().save().entity(new SavedAsOfDate(asOfDate, weekNumber, AS_OF_DATE)).now();
            weekChanged = savedAsOfDate != null && savedAsOfDate.getWeekNumber() != weekNumber;
            boolean newDataAvailable = savedAsOfDate == null || weekChanged || savedAsOfDate.getAsOfDate().before(asOfDate);
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

        for (int classNumber = 1; classNumber < 23; classNumber++) {
            System.out.println("classNumber = " + classNumber);
            SortedCoverMessages sortedCoverMessages;
            try {
                sortedCoverMessages = new HbgDataDownload(classNumber, weekNumber).executeSync();
            } catch (Exception e) {
                log.warning("Error occurred: " + e.getMessage());
                e.printStackTrace();
                if (weekChanged) {
                    SavedMessages.save(null, classNumber); //override old week data
                }
                continue;
            }

            if (weekChanged && sortedCoverMessages != null) {
                try {
                    new WeekChangeNotification().send(classNumber, MessagingEndpoint.API_KEY);
                } catch (IOException e) {
                    log.warning("e.getMessage() = " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (sortedCoverMessages == null) {
                log.info("No data for classnumber " + classNumber);
            }

            if (weekChanged || sortedCoverMessages == null) {
                SavedMessages.save(sortedCoverMessages, classNumber); //override old week data
                continue;
            } else {
                sortedCoverMessages = sortOutNewMessages(sortedCoverMessages, classNumber);
            }

            List<CoverMessage> unsentMessages = new ArrayList<>();
            for (CoverMessage coverMessage : sortedCoverMessages) {
                CoverMessageWrapper message = new CoverMessageWrapper(coverMessage);

                int timeToLive = message.getTimeToLive();
                if (timeToLive < 0) {
                    continue;
                }

                try {
                    Recipients recipients = message.getCondition(classNumber);
                    new PushNotification(message.serializer().toJsonString())
                            .send(recipients, timeToLive, MessagingEndpoint.API_KEY);
                } catch (IOException e) {
                    log.warning("e.getMessage() = " + e.getMessage());
                    e.printStackTrace();
                    unsentMessages.add(coverMessage);
                }
            }

            if (unsentMessages.size() > 0) {
                SortedCoverMessages savedCoverMessages = SavedMessages.load(classNumber);
                for (CoverMessage unsentMessage : unsentMessages) {
                    savedCoverMessages.remove(unsentMessage);
                }
                SavedMessages.save(savedCoverMessages, classNumber);
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getOutputStream().println("Finished");

    }

    private int getWeekNumber() {
        DateTime time = new DateTime(DateTimeZone.forID("Europe/Berlin"));
        int weekNumber = time.getWeekOfWeekyear();
        int dayOfWeek = time.getDayOfWeek();
        if (dayOfWeek == DateTimeConstants.SATURDAY || dayOfWeek == DateTimeConstants.SUNDAY ||
                dayOfWeek == DateTimeConstants.FRIDAY && time.getHourOfDay() >= 19) {
            weekNumber++;
        }
        return weekNumber;
    }

    private SortedCoverMessages sortOutNewMessages(SortedCoverMessages sortedCoverMessages, int classNumber) {
        SortedCoverMessages savedMessages = SavedMessages.load(classNumber);

        if (savedMessages == null) {
            System.out.println("No messages saved for " + classNumber);
            SavedMessages.save(sortedCoverMessages, classNumber);
            return sortedCoverMessages;
        } else if (savedMessages.equals(sortedCoverMessages)) {
            System.out.println("Not sending anything.");
            sortedCoverMessages.clear();
            return sortedCoverMessages;
        }

        SavedMessages.save(sortedCoverMessages, classNumber);

        for (Iterator i = sortedCoverMessages.iterator(); i.hasNext();) {
            CoverMessage coverMessage = (CoverMessage) i.next();
            if (savedMessages.contains(coverMessage)) {
                System.out.println("Not sending: \"" + coverMessage.toString() + "\"");
                i.remove();
            }
        }

        return sortedCoverMessages;
    }

//        public static void main(String[] args) throws Exception {
//
//    }
}
