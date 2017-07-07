package com.eissler.micha.hbgvertretungsapp.settings;

import com.eissler.micha.hbgvertretungsapp.FilterDialog;
import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.fastadapter.FastAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class HiddenSubjects extends SubjectListActivity<SimpleTextItem> {

    private Blacklist blacklist;

    @Override
    protected void initialize() {
        blacklist = Blacklist.get(getApplicationContext());
    }

    @Override
    protected void addToData(String subject) {
        blacklist.add(subject);
        blacklist.save();
    }

    @Override
    protected void removeFromData(Set<SimpleTextItem> selectedItems) {
        for (SimpleTextItem item : selectedItems) {
            for (Iterator<String> iterator = blacklist.iterator(); iterator.hasNext(); ) {
                String subject = iterator.next();
                if (item.getText().equals(subject)) {
                    iterator.remove();
                }
            }
        }
        blacklist.save();
    }

    @Override
    protected List<SimpleTextItem> getItems() {
        blacklist = Blacklist.get(this);

        ArrayList<SimpleTextItem> items = new ArrayList<>(blacklist.size());
        for (String subject : blacklist) {
            items.add(new SimpleTextItem(subject, fastAdapter));
        }
        Collections.sort(items, (item1, item2) -> item1.getText().compareTo(item2.getText()));
        return items;
    }

    @Override
    protected int getLabelResource() {
        return R.string.label_hidden_subjects;
    }

    @Override
    protected SimpleTextItem getNoItemsItem() {
        return new SimpleTextItem("Es werden alle Fächer angezeigt", fastAdapter);
    }

    @Override
    protected FastAdapter.OnClickListener<SimpleTextItem> getOnClickListener() {
        return (v, adapter, item, position) -> {
            new FilterDialog(item.getText(), null, HiddenSubjects.this, HiddenSubjects.this::updateList).show(false);
            return true;
        };
    }
}
