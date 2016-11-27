package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.App;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class CustomNames extends HashMap<String, String> {

    private Context context;


    public static CustomNames get(Context context) {
        return new CustomNames(context);
    }

    public static CustomNames get(Context context, int capacity) {
        return new CustomNames(context, capacity);
    }

    private CustomNames(Context context) {
        this(context, 0);
    }

    private CustomNames(Context context, int capacity) {
        super(capacity);
        this.context = context;

        HashMap<String, String> customNames;
        try {
            customNames = App.retrieveObject(App.CUSTOM_NAMES, context);
        } catch (FileNotFoundException e) {
            App.logError("No CustomNames-file found.");
            return;
        } catch (ClassNotFoundException | IOException e) {
            App.exitWithError(e);
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
