package com.eissler.micha.hbgvertretungsapp.settings;

import com.eissler.micha.hbgvertretungsapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WhitelistSubjects extends SubjectListActivity<SimpleTextItem> {

    private Whitelist whitelist;

    @Override
    protected void initialize() {
        whitelist = Whitelist.get(this);
    }

    @Override
    protected List<SimpleTextItem> getItems() {
        ArrayList<SimpleTextItem> items = new ArrayList<>(whitelist.size());
        for (String subject : whitelist) {
            items.add(new SimpleTextItem(subject, fastAdapter));
        }
        return items;
    }

    @Override
    protected void addToData(String subject) {
        whitelist.add(subject);
    }

    @Override
    protected void removeFromData(Set<SimpleTextItem> selectedItems) {
        for (SimpleTextItem item : selectedItems) {
            whitelist.set(whitelist.indexOf(item.getText()), null); //null elements are removed when saving
        }
        whitelist.save();
    }

    @Override
    protected SimpleTextItem getNoItemsItem() {
        return new SimpleTextItem("Keine Fächer gespeichert - Es wird dir keine einzige Meldung angezeigt werden!", fastAdapter);
    }

    @Override
    protected int getLabelResource() {
        return R.string.whitelist_label;
    }
}
