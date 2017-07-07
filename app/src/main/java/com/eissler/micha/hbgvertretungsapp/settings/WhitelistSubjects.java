package com.eissler.micha.hbgvertretungsapp.settings;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.fcm.Subscriptions;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WhitelistSubjects extends SubjectListActivity<SimpleTextItem> {

    private Whitelist whitelist;
    private boolean changed;
    private MenuItem confirmItem;

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
        Collections.sort(items, (item1, item2) -> item1.getText().compareTo(item2.getText()));
        return items;
    }

    @Override
    protected void addToData(String subject) {
        whitelist.add(subject);
        whitelist.save();
        changed = true;
        if (whitelist.size() > 4) {
            confirmItem.setEnabled(true);
            confirmItem.getIcon().setAlpha(255);
        }
    }

    @Override
    protected void removeFromData(Set<SimpleTextItem> selectedItems) {
        boolean pushEnabled = Subscriptions.isEnabled(this);
        for (SimpleTextItem item : selectedItems) {
            for (Iterator<String> iterator = whitelist.iterator(); iterator.hasNext(); ) {
                String subject = iterator.next();
                if (item.getText().equals(subject)) {
                    iterator.remove();
                    if (pushEnabled) {
                        whitelist.getSubscriptions().remove(subject);
                    }
                }
            }
        }
        whitelist.save();
        changed = true;
        if (whitelist.size() <= 4) {
            confirmItem.setEnabled(false);
            confirmItem.getIcon().setAlpha(130);
        }
    }

    @Override
    protected SimpleTextItem getNoItemsItem() {
        return new SimpleTextItem("Keine Fächer gespeichert - Es wird dir keine einzige Meldung angezeigt werden!", fastAdapter);
    }

    @Override
    protected int getLabelResource() {
        return R.string.whitelist_label;
    }

    @Override
    protected int getMenuRes() {
        return R.menu.menu_whitelist_subjects;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        confirmItem = menu.getItem(1);
        if (whitelist.size() <= 4) {
            confirmItem.setEnabled(false);
            confirmItem.getIcon().setAlpha(130);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_confirm:
                Preferences prefs = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, this);
                if (!changed || !prefs.getBoolean(Preferences.Key.SHOW_WHITELIST_CONFIRMATION_PROMPT, true)) {
                    super.onBackPressed();
                    return true;
                }
                getAskForConfirmationDialogBuilder(WhitelistSubjects.super::onBackPressed)
                        .checkBoxPrompt("Nicht mehr fragen", false, null)
                        .onPositive((dialog, which) -> {
                            if (dialog.isPromptCheckBoxChecked()) {
                                prefs.edit().putBoolean(Preferences.Key.SHOW_WHITELIST_CONFIRMATION_PROMPT, false).apply();
                            }
                            WhitelistSubjects.super.onBackPressed();
                        })
                        .show();

                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if (askForConfirmation(WhitelistSubjects.super::onBackPressed)) return;
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        //noinspection SimplifiableIfStatement
        if (askForConfirmation(WhitelistSubjects.super::onSupportNavigateUp)) return false;
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onRestart() {
        Whitelist.enableWhitelistMode(true, this);
        super.onRestart();
    }

    @Override
    protected void onStop() {
        System.out.println("WhitelistSubjects.onStop");
        if (whitelist.size() <= 4) {
            Whitelist.enableWhitelistMode(false, this);
        }
        super.onStop();
    }

    private boolean askForConfirmation(Runnable finish) {
        if (whitelist.size() <= 4) {
            new MaterialDialog.Builder(this)
                    .title("Verlassen")
                    .content("Es sind zu wenige Fächer eingespeichert, alsdass sie zum Filtern angewendet werden können. Trotzdem verlassen?")
                    .positiveText("Bearbeiten")
                    .negativeText("Verlassen")
                    .onNegative((dialog, which) -> {
                        Whitelist.enableWhitelistMode(false, WhitelistSubjects.this);
                        Toast.makeText(this, "Filter-Einstellung wurde zurückgesetzt", Toast.LENGTH_LONG).show();
                        finish.run();
                    })
                    .show();
            return true;
        }

        if (!changed) {
            return false;
        }

        getAskForConfirmationDialogBuilder(finish)
                .onPositive((dialog, which) -> finish.run())
                .show();

        return true;
    }

    private MaterialDialog.Builder getAskForConfirmationDialogBuilder(Runnable finish) {
        return new MaterialDialog.Builder(this)
                .title("Anwenden?")
                .content("Sollen diese Fächer zum Filtern angewendet werden?\n\nEs werden dir dann nur Meldungen zu den hier gespeicherten Fächern angezeigt!")
                .positiveText("Ja")
                .negativeText("Nein")
                .onNegative((dialog, which) -> {
                    Whitelist.enableWhitelistMode(false, WhitelistSubjects.this);
                    Toast.makeText(this, "Filter-Einstellung wurde zurückgesetzt", Toast.LENGTH_LONG).show();
                    finish.run();
                })
                .neutralText("Bearbeiten");
    }
}
