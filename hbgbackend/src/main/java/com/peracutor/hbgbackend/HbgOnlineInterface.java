package com.peracutor.hbgbackend;

/**
 * Created by Micha.
 * 08.04.2017
 */

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.HeaderMessage;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import java.util.ArrayList;

@Api(
        name = "hbgOnlineInterface",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "hbgbackend.peracutor.com",
                ownerName = "hbgbackend.peracutor.com"
        )
)
public class HbgOnlineInterface {

    @ApiMethod(name = "loadData")
    public ArrayList<HBGMessage> loadData(@Named("classNumber") int classNumber, @Named("weekNumber") int weekNumber) {
        try {
            SortedCoverMessages listItems = new HbgDataDownload(classNumber, weekNumber).executeSync();
            ArrayList<HBGMessage> jsonMessages = new ArrayList<>(listItems.size());
            for (HBGMessage message :
                    listItems) {
                if (message instanceof HeaderMessage) {
                    jsonMessages.add(message);
                } else {
                    jsonMessages.add(new HeaderMessage(((CoverMessage) message).serializer().toJsonString()));
                }
                System.out.println(message.toString());
            }
            return jsonMessages;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
