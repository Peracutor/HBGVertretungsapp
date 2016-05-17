package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class CustomNames extends HashMap<String, String> {

    private Context context;


    /**
     * Get saved CustomNames or if no CustomNames are saved, create new CustomNames.
     * @param context The Context.
     * @throws IOException Thrown when IO-Error occurs while trying to retrieve the CustomNames.
     */
    public CustomNames(Context context) throws IOException {
        this(context, 0);
    }

    public CustomNames(Context context, int capacity) throws IOException {
        super(capacity);
        this.context = context;

        HashMap<String, String> customNames;
        try {
            customNames = App.retrieveObject(App.CUSTOM_NAMES, context);
        } catch (FileNotFoundException e) {
            App.logError("No CustomNames-file found.");
            e.printStackTrace();
            return;
        } catch (ClassNotFoundException e) {
            App.logError("ClassNotFound, should never happen.");
            App.reportUnexpectedException(e);
            e.printStackTrace();
            return;
        }

        if (customNames != null) {
            this.putAll(customNames);
        }
    }


    /**
     * Saves this CustomNames-Object to the internal storage.
     *
     * @return true if saved successfully, false if not.
     */
    public boolean save() {
        try {
            App.writeObject(new HashMap<>(this), App.CUSTOM_NAMES, context);
        } catch (Exception e) {
            App.logError("Error writing object customNames");
            App.reportUnexpectedException(e);
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
