package com.eissler.micha.cloudmessaginglibrary;

import java.io.IOException;
import java.util.List;
import java.util.Map;



/**
 * Created by Micha.
 * 07.07.2017
 */

public class UpdateAvailableNotification extends AbstractPushNotification<UpdateAvailableNotification> {
    public static final String ACTION = "UpdateAvailableNotification";

    private static final String PARAM_VERSION_NAME = "versionName";
    private static final String PARAM_VERSION_NUMBER = "versionNumber";
    private static final String PARAM_APK_URL = "apkUrl";
    private static final String PARAM_VERSION_DESCRIPTION = "versionDescription";

    public UpdateAvailableNotification(Map<String, String> data) {
        super(data);
    }

    public UpdateAvailableNotification(VersionInfo versionInfo) {
        super();
        data.put(PARAM_VERSION_NAME, versionInfo.getVersionName());
        data.put(PARAM_VERSION_NUMBER, String.valueOf(versionInfo.getVersionNumber()));
//        data.put(PARAM_VERSION_DESCRIPTION, versionInfo.getVersionDescription());
//        data.put(PARAM_APK_URL, versionInfo.getApkUrl());
    }

    public VersionInfo getVersionInfo() {
        return new VersionInfo() {
            @Override
            public Integer getVersionNumber() {
                return Integer.valueOf(data.get(PARAM_VERSION_NUMBER));
            }

            @Override
            public String getVersionName() {
                return data.get(PARAM_VERSION_NAME);
            }

            @Override
            public String getVersionDescription() {
                return data.get(PARAM_VERSION_DESCRIPTION);
            }

            @Override
            public String getApkUrl() {
                return data.get(PARAM_APK_URL);
            }
        };
    }
    @Override
    public String getAction() {
        return ACTION;
    }

    public List<Result> send(String apiKey) throws IOException {
        return send(new Recipients().topic("global"), apiKey);
    }
}
