package com.eissler.micha.cloudmessaginglibrary;

import java.util.Map;

/**
 * Created by Micha.
 * 04.07.2017
 */

public class InfoNotification extends AbstractPushNotification<InfoNotification> {

    public static final String ACTION = "InfoNotification";
    public static final String PARAM_TITLE = "title";
    public static final String PARAM_CONTENT = "content";
    public static final String PARAM_IMAGE_URL = "imageUrl";

    public InfoNotification(Map<String, String> data) {
        super(data);
    }

    @Override
    public String getAction() {
        return ACTION;
    }

    public String getTitle() {
        return  data.get(PARAM_TITLE);
    }

    public String getContent() {
        return data.get(PARAM_CONTENT);
    }

    public String getImageUrl() {
        return data.get(PARAM_IMAGE_URL);
    }

    public static class Builder extends AbstractPushNotification.Builder<InfoNotification> {

        @Override
        public InfoNotification build() {
            return new InfoNotification(data);
        }

        public Builder setTitle(String title) {
            data.put(PARAM_TITLE, title);
            return this;
        }

        public Builder setContent(String content) {
            data.put(PARAM_CONTENT, content);
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            data.put(PARAM_IMAGE_URL, imageUrl);
            return this;
        }
    }

}
