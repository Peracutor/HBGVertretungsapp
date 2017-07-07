package com.eissler.micha.cloudmessaginglibrary;

import java.io.IOException;
import java.util.List;

/**
 * Created by Micha.
 * 07.07.2017
 */

public class WeekChangeNotification extends AbstractPushNotification<WeekChangeNotification> {
    public static final String ACTION = "ServerWeekChange";

    @Override
    public String getAction() {
        return ACTION;
    }

    public List<Result> send(int classNumber, String apiKey) throws IOException {
        return send(new Recipients().topic(classNumber + "-week_change"), 2 * 24 * 60 * 60, apiKey); //two days timeToLive
    }
}
