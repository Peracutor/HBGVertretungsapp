package com.eissler.micha.hbgvertretungsapp.evaluation;

import android.content.Context;
import android.content.SharedPreferences;

import com.eissler.micha.hbgvertretungsapp.App;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import hugo.weaving.DebugLog;

public class DataEvaluation {

    private final static String PREFERENCE = "MainPreference";
    private final static String SELECTED_CLASS = "classSelection";

//    private static final Runnable START_PROGRESS = new Runnable() {
//        @Override
//        public void run() {
//            MainActivity.progressBar.setProgress(0);
//            MainActivity.progressBar.setVisibility(View.VISIBLE);
//            System.out.println("setVisibility VISIBLE");
//        }
//    };

//    private static final Runnable END_PROGRESS = new Runnable() {
//        @Override
//        public void run() {
//            MainActivity.progressBar.setVisibility(View.INVISIBLE);
//            System.out.println("setVisibility INVISIBLE");
//        }
//    };

    private Calendar calendar;


    private final Context context;
    private final SharedPreferences prefs;

    private String URL;
    private int weekNumber;

    private DataEvaluationInterface dataEvaluationInterface;

    public DataEvaluation(Context context, DataEvaluationInterface dataEvaluationInterface) {
        this(context, 0, dataEvaluationInterface);
    }


    public DataEvaluation(Context context, int weeksToAdd, DataEvaluationInterface dataEvaluationInterface) {
        this(context, -1, weeksToAdd, dataEvaluationInterface);
    }


    public DataEvaluation(final Context context, int classNum, int weeksToAdd, DataEvaluationInterface dataEvaluationInterface) {
        this.context = context;
        this.dataEvaluationInterface = dataEvaluationInterface;

        prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);

        if (classNum == -1) {
            classNum = prefs.getInt(SELECTED_CLASS, 0);
        }

        calendar = getCalendar(weeksToAdd);
        weekNumber = getWeek(weeksToAdd, calendar);
        URL = makeURL(weekNumber, classNum);

        downloadAndEvaluate();
    }

    @DebugLog
    public static int getWeek(int weeksToAdd, Calendar calendar) {
        System.out.println("weeksToAdd = " + weeksToAdd);

        int weekNumber = calendar.get(Calendar.WEEK_OF_YEAR);

        System.out.println("weekNumber = " + weekNumber);

        return weekNumber;
    }

    public static Calendar getCalendar(int weeksToAdd) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, weeksToAdd);

        int dayInWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        boolean isWeekEnd = dayInWeek == Calendar.SATURDAY || dayInWeek == Calendar.SUNDAY;


        if (isWeekEnd) {
            System.out.println("Adding a week");
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return calendar;
    }

    @DebugLog
    public static String makeURL(int weekNumber, int classNum) {
        return "http://vp.hbgym.de/w/" + (weekNumber < 10 ? "0" + weekNumber : weekNumber) + ((classNum < 10) ? "/w0000" : "/w000") + classNum + ".htm";
    }

    private void downloadAndEvaluate() {

//        try {
//            whySortedDataMapIsNull.put(weekNumber, 0);
//            //noData.put(weekNumber, false);
//
//            file = downloadUrl(URL, "downloading website");
//        } catch (FileNotFoundException fnfe) {
//            System.out.println(fnfe.getMessage());
//            //noData.put(weekNumber, true);
//            whySortedDataMapIsNull.put(weekNumber, NO_DATA);
//            return;
//        } catch (SocketException | UnknownHostException | SocketTimeoutException e) {
//            e.printStackTrace();
//            //createMessageAdapter("Es konnte keine Verbindung zum Server hergestellt werden. Besteht eine Internetverbindung?");
//            whySortedDataMapIsNull.put(weekNumber, BAD_CONNECTION);
//            return;
//        } catch (Download.NoInternetConnectionException e) {
//            e.printStackTrace();
//            whySortedDataMapIsNull.put(weekNumber, NO_CONNECTION);
//            return;
//        } catch (Exception e) {
//            App.reportUnexpectedException(e);
//            e.printStackTrace();
//
//            whySortedDataMapIsNull.put(weekNumber, ERROR);
//            return;
//        }
//        catch (Exception e) {
//            System.out.println("Error at download: " + e.getMessage());
//            e.printStackTrace();
//            createMessageAdapter("Beim Herunterladen ist ein Fehler aufgetreten.");
//            return;
//        }

//        MainActivity.progressBar.setVisibility(View.VISIBLE);

        if (!App.isConnected(context)) {
            dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("Es besteht keine Internetverbindung", DataEvaluationException.NO_CONNECTION), null);
            return;
        }

        Ion.with(context)
                .load(URL)
//                .progressBar(MainActivity.progressBar)
                .noCache()
                .asString(Charset.forName("ISO-8859-15"))
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String htmlText) {
                        System.out.println("onCompleted");
                        if (htmlText == null || e != null) {
                            if (e instanceof FileNotFoundException) {

                                dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("Es liegen keine Vertretungsdaten vor", e, DataEvaluationException.NO_DATA), null);
                            } else if (e instanceof SocketException || e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof TimeoutException) {
                                dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("Es konnte keine Internetverbingung hergestellt werden", e, DataEvaluationException.BAD_CONNECTION), null);
                            } else {
                                dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("Ein unbekannter Fehler ist aufgetreten", e, DataEvaluationException.ERROR), null); // TODO: 02.05.2016 EOF Exception
                            }
                            return;
                        }

                        if (htmlText.contains("keine Vertretungsdaten")) {
//                            whySortedDataMapIsNull.put(weekNumber, NO_DATA);
                            dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("No Data2", DataEvaluationException.NO_DATA), null);
                            return;
                        }

                        Document document = Jsoup.parse(htmlText);

                        Elements tables = document.select("table"); //select the first table.
                        if (tables.size() == 0) {
//                            whySortedDataMapIsNull.put(weekNumber, NO_DATA);
                            dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("No Data3", DataEvaluationException.NO_DATA), null);
                            return;
                        }

                        Elements rows = tables.get(0).select("tr");
                        if (rows.isEmpty()) {
                            dataEvaluationInterface.onDataEvaluationComplete(new DataEvaluationException("No rows in the table", DataEvaluationException.ERROR), null);
                            return;
                        }

                        SortedCoverMessages messages = new SortedCoverMessages(rows.size(), context);

                        for (int i = 1; i < rows.size(); i++) {
                            Element row = rows.get(i);
                            Elements cols = row.select("td");
                            CoverMessage message = new CoverMessage(calendar.get(Calendar.YEAR));

                            for (int j = 0; j < cols.size(); j++) {
                                String nextField = cols.get(j).text().replace('\u00A0', ' ').trim();

                                if (j == CoverMessage.DATE) {
                                    String[] splitDate = nextField.split("\\.", 3);

                                    String day;
                                    if (Integer.parseInt(splitDate[0]) < 10) {
                                        day = "0" + splitDate[0];
                                    } else {
                                        day = splitDate[0];
                                    }

                                    String month;
                                    if (Integer.parseInt(splitDate[1]) < 10) {
                                        month = "0" + splitDate[1];
                                    } else {
                                        month = splitDate[1];
                                    }

                                    String datum = day + "." + month + ".";

                                    message.setField(j, datum);

                                } else if (j == CoverMessage.KIND && nextField.contains(":")) {
                                    message.setField(j, nextField.substring(0, nextField.lastIndexOf(":")));
                                } else {
                                    message.setField(j, nextField.equals("---") ? "" : nextField);
                                }
                            }

                            messages.insert(message);
                        }

                        dataEvaluationInterface.onDataEvaluationComplete(null, messages);
                    }
                });
    }

//    private ArrayList<String[]> sortList(ArrayList<String[]> unsortedList) {
//        ArrayList<String[]> sortedList = new ArrayList<>(unsortedList.size());
//        for (String[] dataSet : unsortedList) {
//            boolean added = false;
//            int concernedHour;
//            if (!dataSet[HOUR].equals("")) {
//                concernedHour = getBeginningHour(dataSet[HOUR]);
//            } else {
//                dataSet[HOUR] = "?";
//                sortedList.add(dataSet);
//                continue;
//            }
//            int size = sortedList.size();
//
//            for (int i = 0; i < size; i++) {
//                int compareHour;
//                if (!sortedList.get(i)[HOUR].equals("?")) {
//                    compareHour = getBeginningHour(sortedList.get(i)[HOUR]);
//                } else {
//                    compareHour = 99;
//                }
//                if (compareHour > concernedHour) {
//                    sortedList.add(i, dataSet);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                sortedList.add(dataSet);
//            }
//        }
//        return sortedList;
//    }

//    private String retrieveStand() {
//        String file;
//        try {
//            file = downloadUrl("http://vp.hbgym.de/frames/title.htm", "retrieving stand");
//        } catch (Exception e) {
//            e.printStackTrace();
//            //returning null causes newDataAvailable()-function to return true
//            return null;
//        }
//
//
//        int start = file.indexOf("Stand: ") + 7;
//        int end = start + 16;
//
//        String dateString = file.substring(start, end);
//        System.out.println("Stand der HBG-Website: " + dateString);
//
//
//        prefs.edit().putString(LAST_TIME_STAND_RETRIEVED, PRECISE_SDF.format(new Date())).apply();
//
//        return dateString;
//    }

//    boolean minutesLater(int minutes) {
//
//        Calendar twoMinLater = Calendar.getInstance();
//        twoMinLater.setTime(lastTimeStandRetrieved);
//        twoMinLater.add(Calendar.MINUTE, minutes);
//        Date twoMinutesLater = twoMinLater.getTime();
//
//        System.out.println("Two minutes later?: " + new Date().after(twoMinutesLater));
//
//        return new Date().after(twoMinutesLater);
//    }
//
//    boolean newDataAvailable() {
//        String dateString = retrieveStand();
//        if (dateString == null) {
//            return true;
//        }
//
//        Date stand;
//        try {
//            stand = PRECISE_SDF.parse(dateString);
//        } catch (ParseException e) {
//            System.err.println("Error parsing stand");
//            e.printStackTrace();
//            return true;
//        }
//
//        boolean newDataAvailable = stand.after(lastTimeStandRetrieved);
//        System.out.println("New data available?: " + newDataAvailable);
//        return newDataAvailable;
//    }

    public static Calendar getDayToNotify() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);

        if (new Date().after(calendar.getTime())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar;
    }

    public interface DataEvaluationInterface {
        void onDataEvaluationComplete(DataEvaluationException e, SortedCoverMessages sortedCoverMessages);
    }


    public static class DataEvaluationException extends Exception {
        public static final Integer NO_DATA = 1;
        public static final Integer NO_CONNECTION = 2;
        public static final Integer BAD_CONNECTION = 3;
        public static final Integer ERROR = 4;

        private int errorCode;

        public DataEvaluationException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public DataEvaluationException(String message, Exception cause, int errorCode) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }
}
