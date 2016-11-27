package com.eissler.micha.hbgvertretungsapp.settings;

import com.eissler.micha.hbgvertretungsapp.R;

import java.util.ArrayList;

public class WhitelistSubjects extends SubjectListActivity {

    private Whitelist whitelist;


    @Override
    protected void initialize() {
        whitelist = Whitelist.get(this);
    }

    @Override
    protected SubjectListAdapter getSubjectListAdapter() {
        return new WhiteListAdapter();
    }

    @Override
    protected ArrayList<String> getData() {
        return whitelist;
    }

    @Override
    protected void addToData(String subject) {
        whitelist.add(subject);
    }

    @Override
    protected void removeFromData(ArrayList<Integer> indices) {
        for (int index : indices) {
            whitelist.set(index, null); //null elements are removed when saving
        }
    }

    @Override
    protected void saveData() {
        whitelist.save();
    }

    @Override
    protected boolean showCheckBoxesConstantly() {
        return true;
    }

    @Override
    protected int getLabelResource() {
        return R.string.whitelist_label;
    }

    class WhiteListAdapter extends SubjectListAdapter {
        public WhiteListAdapter() {
            super(whitelist, WhitelistSubjects.this);
        }

        @Override
        protected String getNoItemsString() {
            return "Keine Fächer gespeichert - Es wird dir keine einzige Meldung angezeigt werden!";
        }

        @Override
        public boolean isSelectionMode() {
            return true;
        }
    }
}
