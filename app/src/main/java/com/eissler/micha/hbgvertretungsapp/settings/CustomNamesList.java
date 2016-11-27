package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.FilterDialog;
import com.eissler.micha.hbgvertretungsapp.InputValidator;
import com.eissler.micha.hbgvertretungsapp.R;

import java.util.ArrayList;
import java.util.Map;


public class CustomNamesList extends SubjectListActivity {

    private CustomNames customNames;
    private boolean validOriginalName;
    private boolean validNewName;

    @Override
    protected void initialize() {
        customNames = CustomNames.get(getApplicationContext());
    }

    @Override
    protected SubjectListAdapter getSubjectListAdapter() {
        return new CustomNamesAdapter();
    }

    @Override
    protected ArrayList<String> getData() {
        ArrayList<String> data = new ArrayList<>(12);

        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            if (!entry.getValue().equals("Nicht anzeigen")) {
                data.add(entry.getKey() + ": " + entry.getValue());
            }
        }

        return data;
    }

    @Override
    protected void addToData(String subject) {

    }

    @Override
    protected void removeFromData(ArrayList<Integer> indices) {
        ArrayList<String> data = getData();
        for (Integer index : indices) {
            String subject = data.get(index).split(":")[0];
            customNames.remove(subject);
        }
    }

    @Override
    protected void saveData() {
        customNames.save();
    }

    @Override
    protected int getLabelResource() {
        return R.string.label_custom_names_list;
    }


    @Override
    protected void actionAdd() {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_name, null);
        final EditText originalDisplayname = (EditText) dialogView.findViewById(R.id.kursName);
        final EditText newDisplayName = (EditText) dialogView.findViewById(R.id.new_subject_name);

        final AlertDialog dialog = new AlertDialog.Builder(CustomNamesList.this)
                .setView(dialogView)
                .setTitle("Fach hinzufügen")
                .setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String originalName = originalDisplayname.getText().toString();
                        String newName = newDisplayName.getText().toString();

                        customNames.put(originalName, newName);
                        updateList();
                        customNames.save();
                    }
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

    class CustomNamesAdapter extends SubjectListAdapter {
        public CustomNamesAdapter() {
            super(getData(), CustomNamesList.this);
        }

        @Override
        protected String getNoItemsString() {
            return "Keine Anzeigenamen gespeichert";
        }

        @Override
        protected View.OnClickListener getNonSelectiveListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String[] split = ((TextView) view.findViewById(R.id.textView)).getText().toString().split(":");
                    new FilterDialog(split[1].trim(), split[0].trim(), customNames,
                            new FilterDialog.PostExecuteInterface() {
                                @Override
                                public void onPostExecute() {
                                    updateList();
                                }
                            },
                            CustomNamesList.this).show();
                }
            };
        }
    }
}
