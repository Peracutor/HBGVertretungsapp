package com.eissler.micha.hbgvertretungsapp.fcm;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;

/**
 * Created by Micha.
 * 10.09.2016
 */
public class AppEngine {

    public static <T extends AbstractGoogleJsonClient> T getApiInstance(AbstractGoogleJsonClient.Builder builder) {
        System.out.println("AppEngine.getApiInstance");
        // options for running against local devappserver
        // - 10.0.2.2 is localhost's IP address in Android emulator
        // - turn off compression when running against local devappserver
        builder.setRootUrl("https://hbg-vertretungsapp.appspot.com/_ah/api/")
//        builder.setRootUrl("http://10.0.2.2:8080/_ah/api/") // TOxDO: 08.10.2016 change root-url
//                .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
//                    @Override
//                    public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
//                        abstractGoogleClientRequest.setDisableGZipContent(true);
//                    }
//                })
        ;
        //noinspection unchecked
        return (T) builder.build();
    }
}
