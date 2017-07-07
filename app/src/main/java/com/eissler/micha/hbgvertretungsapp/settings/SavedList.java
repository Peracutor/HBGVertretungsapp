package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;

import com.eissler.micha.hbgvertretungsapp.App;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Micha.
 * 21.12.2016
 */
public abstract class SavedList extends ArrayList<String> {
    private final String fileName;
    protected final Context context;

    public SavedList(String fileName, Context context) {
        this(fileName, 10, context);
    }

    public SavedList(String fileName, int initialCapacity, Context context) {
        super(initialCapacity);
        this.fileName = fileName;
        this.context = context;

        ArrayList<String> savedList = null;
        try {
            savedList = App.retrieveObject(fileName, context);
        } catch (FileNotFoundException e) {
            System.out.println("No file saved: " + e.getMessage());
        } catch (ClassNotFoundException | IOException e) {
            App.exitWithError(e);
            return;
        }

        if (savedList != null) {
            this.addAll(savedList);
        }
    }

    public boolean save() {
        try {
            App.writeObject(new ArrayList<>(this), fileName, context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            App.report(e);
            return false;
        }
    }

    @Override
    public boolean add(String s) {
        return !contains(s) && super.add(s);
    }

    @Override
    public void add(int index, String element) {
        if (contains(element)) {
            return;
        }
        super.add(index, element);
    }
}
