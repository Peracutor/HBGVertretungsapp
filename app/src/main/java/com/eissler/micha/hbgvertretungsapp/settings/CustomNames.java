package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.App;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.TreeMap;

public class CustomNames extends TreeMap<String, String> {

    public final static String CUSTOM_NAMES = "CustomNames";
    private Context context;


    public static CustomNames get(Context context) {
        TreeMap<String, String> customNames;
        try {
            customNames = App.retrieveObject(CUSTOM_NAMES, context);
        } catch (FileNotFoundException e) {
            App.logError("No CustomNames-file found.");
            return new CustomNames(context, new TreeMap<>());
        } catch (ClassCastException e) {
            try {
                Map<String, String> map = App.retrieveObject(CustomNames.CUSTOM_NAMES, context);
                customNames = new TreeMap<>(map);
            } catch (Exception e1) {
                App.exitWithError(e1);
                return new CustomNames(context, null);//just so AndroidStudio does not show "may cause NullPointerException"
            }
        } catch (Exception e) {
            App.exitWithError(e);
            return new CustomNames(context, null); //just so AndroidStudio does not show "may cause NullPointerException"
        }
        return new CustomNames(context, customNames);
    }

    private CustomNames(Context context, TreeMap<String, String> customNames) {
        super(customNames);
        this.context = context;
    }


    /**
     * Saves this CustomNames-Object to the internal storage.
     *
     * @return true if saved successfully
     */
    public boolean save() {
        try {
            App.writeObject(new TreeMap<>(this), CUSTOM_NAMES, context);
        } catch (Exception e) {
            App.logError("Error writing object customNames");
            App.report(e);
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
