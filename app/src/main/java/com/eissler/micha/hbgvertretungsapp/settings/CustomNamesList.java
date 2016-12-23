package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.eissler.micha.hbgvertretungsapp.FilterDialog;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;
import com.mikepenz.fastadapter.FastAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CustomNamesList extends SubjectListActivity<SimpleTextItem> {

    private CustomNames customNames;
    private boolean validOriginalName;
    private boolean validNewName;

    @Override
    protected void initialize() {
        customNames = CustomNames.get(getApplicationContext());
    }

    @Override
    protected List<SimpleTextItem> getItems() {
        System.out.println("CustomNamesList.getItems");
        ArrayList<SimpleTextItem> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            String text = entry.getKey() + ": " + entry.getValue();
            System.out.println("text = " + text);
            items.add(new SimpleTextItem(text, fastAdapter));
        }
        return items;
    }

    @Override
    protected void addToData(String subject) {/*never gets called, as actionAdd is overridden*/}

    @Override
    protected void removeFromData(Set<SimpleTextItem> selectedItems) {
        for (SimpleTextItem item : selectedItems) {
            String subject = item.getText().split(":")[0];
            customNames.remove(subject);
        }
        customNames.save();
    }

    @Override
    protected SimpleTextItem getNoItemsItem() {
        return new SimpleTextItem("Keine Anzeigenamen gespeichert", fastAdapter).withSelectable(false);
    }

    @Override
    protected int getLabelResource() {
        return R.string.label_custom_names_list;
    }

    @Override
    protected FastAdapter.OnClickListener<SimpleTextItem> getOnClickListener() {
        return (v, adapter, item, position) -> {
            String[] split = item.getText().split(":");
            new FilterDialog(split[0].trim(), split[1].trim(), customNames, CustomNamesList.this, CustomNamesList.this::updateList).show();
            return true;
        };
    }

    @Override
    protected void actionAdd() {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_name, null);
        final EditText originalDisplayname = (EditText) dialogView.findViewById(R.id.kursName);
        final EditText newDisplayName = (EditText) dialogView.findViewById(R.id.new_subject_name);

        final AlertDialog dialog = new AlertDialog.Builder(CustomNamesList.this)
                .setView(dialogView)
                .setTitle("Fach hinzufügen")
                .setPositiveButton("Hinzufügen", (dialog1, which) -> {

                    String originalName = originalDisplayname.getText().toString();
                    String newName = newDisplayName.getText().toString();

                    customNames.put(originalName, newName);
                    updateList();
                    customNames.save();
                })
                .setNegativeButton("Abbrechen", null)
                .show();

        originalDisplayname.addTextChangedListener(new InputValidator() {
            @Override
            protected boolean validate(String text) {
                if (text.equals("")) {
                    originalDisplayname.setError("Feld darf nicht leer sein");
                    return false;
                } else {
                    originalDisplayname.setError(null);
                    return true;
                }
            }

            @Override
            protected void onValidated(boolean valid) {
                validOriginalName = valid;
                if (validNewName) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(valid);
                }
            }
        });

        newDisplayName.addTextChangedListener(new InputValidator() {
            @Override
            protected boolean validate(String text) {
                if (text.equals("")) {
                    newDisplayName.setError("Feld darf nicht leer sein");
                    return false;
                } else {
                    newDisplayName.setError(null);
                    return true;
                }
            }

            @Override
            protected void onValidated(boolean valid) {
                validNewName = valid;
                if (validOriginalName) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(valid);
                }
            }
        });
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }
}
