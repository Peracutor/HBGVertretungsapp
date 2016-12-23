package com.peracutor.hbgbackend;

import com.google.android.gcm.server.Message;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HbgAsOfDateDownload;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.HtmlDownloadHandler;
import com.peracutor.hbgserverapi.ResultCallback;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
            weekChanged = savedAsOfDate == null || savedAsOfDate.getWeekNumber() != weekNumber;
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
                sortedCoverMessages = new HbgDataDownload(classNumber, weekNumber, new DownloadHandler()).executeSync(); // TODO: 03.12.2016 maybe load each class-data async
            } catch (Exception e) {
                log.warning("Error occurred: " + e.getMessage());
                if (e.getCause() != null) e.getCause().printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                continue;
            }

            if (sortedCoverMessages == null) {
                log.info("No data for classnumber " + classNumber);
                continue;
            }

            if (weekChanged) {
                SavedMessages.save(sortedCoverMessages, classNumber); //override old week data
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
                Map<String, String> data = message.toMap();
                data.put(MessageConstants.MESSAGE_ACTION, MessageConstants.ACTION_PUSH);
                Message.Builder builder = new Message.Builder()
                        .timeToLive(timeToLive)
                        .setData(data);

                try {
                    String condition = String.format(Locale.GERMANY, "'%s' in topics || '%s' in topics", message.getTopic(classNumber), classNumber + "-no_whitelist");
                    log.info("condition = " + condition);
                    MessagingEndpoint.sendToMultipleTopics(condition, builder.build());
                } catch (IOException e1) {
                    log.warning("e1.getMessage() = " + e1.getMessage());
                    e1.printStackTrace();
                    unsentMessages.add(coverMessage);

                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

            if (unsentMessages.size() > 0) {
                SortedCoverMessages savedCoverMessages = SavedMessages.load(classNumber);
                unsentMessages.forEach(savedCoverMessages::remove);
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

    private static class DownloadHandler implements HtmlDownloadHandler {

        static final char[] UMLAUTS = "äöüÄÖÜß".toCharArray();

        @Override
        public void asyncDownload(String urlString, Charset charset, ResultCallback<String> callback) {
        }

        @Override
        public String syncDownload(String urlString, Charset charset) throws Exception {
            java.net.URL url = new URL(urlString);
            URLConnection con = url.openConnection();
            Reader r = new InputStreamReader(con.getInputStream(), charset);
            StringBuilder buf = new StringBuilder();
            while (true) {
                int ch = r.read();
                if (ch < 0) {
                    break;
                } else if (isUmlaut(ch)) {
                    buf.append("%").append(ch).append("%");
                } else {
                    buf.append((char) ch);
                }
            }
            return buf.toString();
        }

        private boolean isUmlaut(int ch) {
            boolean isUmlaut = false;
            for (char umlaut : UMLAUTS) {
                if ((int) umlaut == ch) {
                    isUmlaut = true;
                    break;
                }
            }
            return isUmlaut;
        }
    }
}
