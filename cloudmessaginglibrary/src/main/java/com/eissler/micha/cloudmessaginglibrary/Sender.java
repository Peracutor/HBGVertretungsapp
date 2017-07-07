package com.eissler.micha.cloudmessaginglibrary;

import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.logging.Level;

import static com.google.android.gcm.server.Constants.PARAM_COLLAPSE_KEY;
import static com.google.android.gcm.server.Constants.PARAM_DELAY_WHILE_IDLE;
import static com.google.android.gcm.server.Constants.PARAM_TIME_TO_LIVE;

public class Sender extends com.google.android.gcm.server.Sender {
    private static final String FCM_SEND_ENDPOINT = "https://fcm.googleapis.com/fcm/send";
    private static final String TOKEN_MESSAGE_ID = "message_id";
    private static final String PARAM_TO = "to";
    private static final String PARAM_DATA = "data";
    private static final String PARAM_CONDITION = "condition";
    //    private static final String PARAM_NOTIFICATION = "notification";
    private String key;

    /**
     * Default constructor.
     *
     * @param key API key obtained through the Google API Console.
     */
    public Sender(String key) {
        super(key);
        this.key = key;
    }

    public Result sendMessage(Message message, String to, int retries) throws IOException {
        return sendMessage(message, to, to.contains("in topics") ? PARAM_CONDITION : PARAM_TO, retries);
    }

    private Result sendMessage(Message message, String target, String paramTarget, int retries) throws IOException {
        int attempt = 0;
        Result result;
        int backoff = BACKOFF_INITIAL_DELAY;
        boolean tryAgain;
        do {
            attempt++;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Attempt #" + attempt + " to send message " +
                        message + " to regIds " + target);
            }
            result = sendMessageNoRetry(message, target, paramTarget);
            tryAgain = result == null && attempt <= retries;
            if (tryAgain) {
                int sleepTime = backoff / 2 + random.nextInt(backoff);
                sleep(sleepTime);
                if (2 * backoff < MAX_BACKOFF_DELAY) {
                    backoff *= 2;
                }
            }
        } while (tryAgain);
        if (result == null) {
            throw new IOException("Could not send message after " + attempt +
                    " attempts");
        }
        return result;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public Result sendMessageNoRetry(Message message, String target, String paramTarget) throws IOException {
        StringBuilder body = newBody();
        addParameter(body, paramTarget, target, true);

        Map<String, String> data = message.getData();
        if (data != null) {
            addParameter(body, PARAM_DATA, JSONObject.toJSONString(data), false);
        }
        Boolean delayWhileIdle = message.isDelayWhileIdle();
        if (delayWhileIdle != null) {
            addParameter(body, PARAM_DELAY_WHILE_IDLE, delayWhileIdle ? "true" : "false", false);

        }
        String collapseKey = message.getCollapseKey();
        if (collapseKey != null) {
            addParameter(body, PARAM_COLLAPSE_KEY, collapseKey, true);
        }
        Integer timeToLive = message.getTimeToLive();
        if (timeToLive != null) {
            addParameter(body, PARAM_TIME_TO_LIVE, Integer.toString(timeToLive), false);
        }

        finishBody(body);
        String requestBody = body.toString();

        logger.info("Request body: " + requestBody); //originally finest() not info()

        HttpURLConnection conn = post(FCM_SEND_ENDPOINT, requestBody);
        System.out.println("posted");
        int status = conn.getResponseCode();
        if (status == 503) {
            logger.fine("GCM service is unavailable");
            return null;
        }
        if (status != 200) {
            throw new InvalidRequestException(status);
        }
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));
            try {
                String line = reader.readLine();
                System.out.println("response line = " + line);
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(line);
                Long messageID = (Long) json.get(TOKEN_MESSAGE_ID);
//                Long multicast_id = (Long) json.get("multicast_id");
//                System.out.println("multicast_id = " + multicast_id);
//                Boolean success = (Long) json.get("success") == 1;
//                System.out.println("success = " + success);
//                Boolean failure = (Long) json.get("failure") == 1;
//                System.out.println("failure = " + failure);
//                Long canonicalIds = (Long) json.get("canonical_ids");
//                System.out.println("canonicalIds = " + canonicalIds);
//                JSONArray results = (JSONArray) json.get("results");
//                System.out.println("results = " + results);
//                JSONObject result0 = (JSONObject) results.get(0);
//                System.out.println("result0 = " + result0); // TODO: 07.09.2016 process result: https://firebase.google.com/docs/cloud-messaging/http-server-ref#interpret-downstream


                return new Result.Builder().messageId(String.valueOf(messageID)).build();
//                if (line == null || line.equals("")) {
//                    throw new IOException("Received empty response from GCM service.");
//                }
//                String[] responseParts = split(line);
//                String token = responseParts[0];
//                String value = responseParts[1];
//                switch (token) {
//                    case TOKEN_MESSAGE_ID:
//                        Result.Builder builder = new Result.Builder().messageId(value);
//                        // check for canonical registration id
//                        line = reader.readLine();
//                        if (line != null) {
//                            responseParts = split(line);
//                            token = responseParts[0];
//                            value = responseParts[1];
//                            if (token.equals(TOKEN_CANONICAL_REG_ID)) {
//                                builder.canonicalRegistrationId(value);
//                            } else {
//                                logger.warning("Received invalid second line from GCM: " + line);
//                            }
//                        }
//
//                        Result result = builder.build();
//                        if (logger.isLoggable(Level.FINE)) {
//                            logger.fine("Message created succesfully (" + result + ")");
//                        }
//                        return result;
//                    case TOKEN_ERROR:
//                        return new Result.Builder().errorCode(value).build();
//                    default:
//                        throw new IOException("Received invalid response from GCM: " + line);
//                }
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            } finally {
                reader.close();
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void finishBody(StringBuilder body) {
        body.deleteCharAt(body.lastIndexOf(","));
        body.append('}');
    }

    protected static void addParameter(StringBuilder body, String name, String value, boolean wrap) {
        if (wrap) value = wrapValue(value);
        body.append('\"').append(nonNull(name)).append('\"')
                .append(':')
                .append(value)
                .append(",");
    }

    private static String wrapValue(String value) {
        return "\"" + value + "\"";
    }

    protected static StringBuilder newBody() {
        return new StringBuilder("{");
    }

    static <T> T nonNull(T argument) {
        if (argument == null) {
            throw new IllegalArgumentException("argument cannot be null");
        }
        return argument;
    }

//    private String[] split(String line) throws IOException {
//        String[] split = line.split("=", 2);
//        if (split.length != 2) {
//            throw new IOException("Received invalid response line from GCM: " + line);
//        }
//        return split;
//    }

    protected HttpURLConnection post(String url, String body)
            throws IOException {
        return post(url, "application/json; charset=UTF-8", body);
    }

    protected HttpURLConnection post(String url, String contentType, String body)
            throws IOException {
        if (url == null || body == null) {
            throw new IllegalArgumentException("arguments cannot be null");
        }
        if (!url.startsWith("https://")) {
            logger.warning("URL does not use https: " + url);
        }
        logger.fine("Sending POST to " + url);
        logger.finest("POST body: " + body);
        byte[] bytes = body.getBytes("UTF-8");
        HttpURLConnection conn = getConnection(url);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", "key=" + key);
        OutputStream out = conn.getOutputStream();
        out.write(bytes);
        out.close();
        return conn;
    }

//    public static void main(String[] args) {
//        try {
//            System.out.println("key = " + args[0]);
//            String body = "{\"condition\":\"'22-LMaA1' in topics || '22-no_whitelist' in topics\",\"data\":{\"action\":\"AbstractPushNotification\",\"message_json\":\"{\\\"fields\\\":[\\\"16.03.\\\",\\\"5 - 6\\\",\\\"LMaA1\\\",\\\"\\\",\\\"\\\",\\\"Entfall\\\",\\\"Aufgaben im Sekretariat abholen!\\\",\\\"Q-3\\/4\\\"],\\\"year\\\":2017,\\\"concernedDate\\\":"+ (new Date().getTime() + 3 * 60 * 60 * 24 * 1000) +",\\\"timeToLive\\\":20034}\"},\"time_to_live\":20034}";
//            System.out.println("body = " + body);
//            new Sender(MessagingEndpoint.API_KEY).post(FCM_SEND_ENDPOINT, body);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
